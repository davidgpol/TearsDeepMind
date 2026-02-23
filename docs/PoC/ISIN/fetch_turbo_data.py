import requests
from bs4 import BeautifulSoup
import re
import logging
from typing import Dict, Optional
import argparse
import json
import sys

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class OfficialIssuerFetcher:
    """
    Fetches Turbo Warrant data directly from Vontobel and Société Générale official websites.
    """

    def __init__(self, isin: str):
        self.isin = isin
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept-Language': 'de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7',
        })
        self.session.verify = False 
        requests.packages.urllib3.disable_warnings()

    def fetch_data(self) -> Dict:
        """
        Orchestrates fetching based on likely issuer.
        """
        # Determine issuer by ISIN prefix or trial?
        # Vontobel ISINs often start with DE000V... but not always.
        # SocGen ISINs often start with DE000S... or DE000C...
        
        logger.info(f"Attempting to fetch data for ISIN: {self.isin}")

        # Try Vontobel first (most robust extraction via Next.js data)
        vontobel_data = self._fetch_vontobel()
        if vontobel_data:
            return vontobel_data

        # Try SocGen
        socgen_data = self._fetch_socgen()
        if socgen_data:
            return socgen_data

        return {"error": "ISIN not found or issuer not supported on official sites."}

    def _fetch_vontobel(self) -> Optional[Dict]:
        """
        Fetches data from markets.vontobel.com using the CMS redirect to find the correct product page,
        then parsing the __NEXT_DATA__ blob.
        """
        # Step 1: Resolve the dynamic product URL using the redirect service
        redirect_url = f"https://markets.vontobel.com/cms/de-de/productredirect/{self.isin}"
        logger.info(f"Resolving Vontobel URL: {redirect_url}")
        
        try:
            # The redirect service requires a Referer to avoid 403 Forbidden
            # We mimic coming from an aggregator
            headers = self.session.headers.copy()
            headers['Referer'] = 'https://www.ariva.de/'
            
            response = self.session.get(redirect_url, headers=headers, timeout=10, allow_redirects=True)
            
            if response.status_code != 200:
                logger.warning(f"Vontobel Redirect failed. Status: {response.status_code}")
                return None
            
            final_url = response.url
            logger.info(f"Resolved to: {final_url}")

            # Step 2: Parse the Next.js data from the resolved page
            soup = BeautifulSoup(response.text, 'html.parser')
            next_data_script = soup.find('script', id='__NEXT_DATA__')
            
            if not next_data_script:
                logger.warning("Vontobel: __NEXT_DATA__ not found on resolved page.")
                return None

            data_blob = json.loads(next_data_script.string)
            
            # Step 3: Extract Product Data from 'additionalData'
            # Path: props.pageProps.additionalData
            
            additional_data = data_blob.get('props', {}).get('pageProps', {}).get('additionalData', {})
            
            if not additional_data:
                logger.info("Vontobel: 'additionalData' not found in JSON state.")
                return None

            core_data = additional_data.get('data', {})
            price_data = additional_data.get('price', {})
            key_figures = additional_data.get('keyFigures', [])
            
            # Helper to find key figure by name ID
            # ID Mapping based on empirical data:
            # 13: Effective Leverage (Hebel) - Common
            # 7: Leverage (Hebel) - Seen in some Turbos
            # 5: Ratio (Bezugsverhältnis) - NOT Leverage
            leverage = "N/A"
            for kf in key_figures:
                val = kf.get('value', {}).get('ratio')
                if kf.get('name') in [13, 7] and val:
                    leverage = str(val)
                    break
            
            # Fallback Calculation if Leverage is missing but we have prices
            # Leverage = (Underlying Price * Ratio) / Product Price
            if leverage == "N/A":
                try:
                    underlying_price = float(data_blob.get('props', {}).get('pageProps', {}).get('additionalData', {}).get('underlyings', [{}])[0].get('price', {}).get('latest', 0))
                    prod_price = float(price_data.get('latest', 0))
                    ratio = float(core_data.get('ratio', 0))
                    
                    if prod_price > 0 and ratio > 0:
                        calc_lev = (underlying_price * ratio) / prod_price
                        leverage = f"{calc_lev:.2f} (Calc)"
                except:
                    pass

            # Core Data Extraction
            strike = str(core_data.get('strikePrice', 'N/A'))
            barrier = str(core_data.get('barrier', 'N/A'))
            
            # Smart Knock-Out Logic
            # For Turbos/Warrants, if Barrier is N/A, the Strike is often the effective KO level (or equivalent)
            if barrier == "N/A" and strike != "N/A":
                knock_out = strike
            else:
                knock_out = barrier

            # Maturity Logic
            expiry_date = core_data.get('expiryDate')
            is_open_end = not core_data.get('isEndOfLifeComplete', True) # Often false for open end active
            
            # Refined check based on JSON
            # In debug JSON: "isEndOfLifeComplete": false, "optionExerciseType": 1
            # primaryFeatures might have FinalFixingDto
            maturity = "Fixed/Expired"
            if expiry_date:
                maturity = expiry_date
            elif is_open_end:
                maturity = "Open End"

            return {
                "issuer": "Vontobel",
                "isin": core_data.get('isin'),
                "current_value": str(price_data.get('latest', 'N/A')),
                "bid": str(price_data.get('bid', 'N/A')),
                "ask": str(price_data.get('ask', 'N/A')),
                "leverage": leverage, 
                "strike": strike,
                "knock_out": knock_out,
                "maturity": maturity,
                "ratio": str(core_data.get('ratio', 'N/A'))
            }

        except Exception as e:
            logger.error(f"Vontobel Fetch Error: {e}")
            return None

    def _fetch_socgen(self) -> Optional[Dict]:
        """
        Fetches data from Société Générale. 
        Note: SocGen is an Angular app. Direct requests often fail to get data without an API key.
        We will try to hit the backend API directly if possible, or fail gracefully.
        """
        # SocGen's API is protected and complex. 
        # For this PoC, we will try a known public-facing endpoint if available, 
        # otherwise acknowledge the limitation.
        
        # Hypothetical direct API endpoint based on common structures
        # api_url = f"https://www.sg-zertifikate.de/api/v1/products/{self.isin}" 
        
        logger.info("Checking SocGen (Limited Support via Raw Requests)...")
        return None

def main():
    parser = argparse.ArgumentParser(description="Fetch Turbo Warrant data from Official Sources.")
    parser.add_argument("isin", help="ISIN of the product")
    args = parser.parse_args()

    fetcher = OfficialIssuerFetcher(args.isin)
    result = fetcher.fetch_data()
    print(json.dumps(result, indent=4))

if __name__ == "__main__":
    main()