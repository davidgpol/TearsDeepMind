import requests
import json
import logging
import argparse
from bs4 import BeautifulSoup
from typing import List, Dict, Optional

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(message)s') # Cleaner output for CLI tool
logger = logging.getLogger(__name__)

class VontobelScanner:
    BASE_URL = "https://markets.vontobel.com"
    UNDERLYING_MAP = {
        "SPX": 70,
        "DAX": 1, 
        "NDX": 72 
    }

    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
            'Accept-Language': 'de-DE,de;q=0.9,en-US;q=0.8,en;q=0.7',
        })

    def scan(self, underlying_symbol: str = "SPX", direction: str = "LONG", limit: int = 20, tr_only: bool = False) -> List[Dict]:
        underlying_id = self.UNDERLYING_MAP.get(underlying_symbol)
        if not underlying_id:
            logger.error(f"Underlying {underlying_symbol} not mapped.")
            return []

        direction_val = 1 if direction.upper() == "LONG" else 2
        
        url = f"{self.BASE_URL}/de-de/produkte/hebel/turbo-optionsscheine"
        params = {
            'underlying': underlying_id,
            'direction': direction_val,
        }
        
        if tr_only:
            params['platforms'] = 1

        logger.info(f"Scanning Vontobel for {underlying_symbol} {direction} (TR Filter: {tr_only})...")
        
        try:
            response = self.session.get(url, params=params, timeout=10)
            if response.status_code != 200:
                logger.error(f"Failed to fetch data: {response.status_code}")
                return []

            soup = BeautifulSoup(response.text, 'html.parser')
            next_data = soup.find('script', id='__NEXT_DATA__')

            if not next_data:
                logger.error("Vontobel parsing failed: __NEXT_DATA__ missing.")
                return []

            data_blob = json.loads(next_data.string)
            items = data_blob.get('props', {}).get('pageProps', {}).get('additionalData', {}).get('productSearchData', {}).get('items', [])
            
            results = []
            for item in items:
                is_tr = item.get('isTradeRepublic', False)
                if tr_only and not is_tr and item.get('isTradeRepublic') is not None:
                    continue

                isin = item.get('isin')
                knock_out = item.get('knockOut')
                leverage = item.get('leverage')
                bid = item.get('price', {}).get('bid')
                ask = item.get('price', {}).get('ask')
                maturity = item.get('maturity')
                ratio = item.get('ratio')
                spot_price = item.get('spotPrice')
                
                dist_ko = item.get('barrierDistance') or item.get('riskBuffer')

                strike = item.get('strike')
                if not strike:
                    for feat in item.get('primaryFeatures', []):
                        if feat.get('type') == 14:
                             strike = feat.get('strike')
                             break
                
                if not strike and knock_out:
                    strike = knock_out

                if not leverage:
                    continue
                
                spread = (ask - bid) if (bid and ask) else 0.0

                results.append({
                    "isin": isin,
                    "type": direction,
                    "strike": strike,
                    "barrier": knock_out,
                    "leverage": leverage,
                    "bid": bid,
                    "ask": ask,
                    "maturity": maturity,
                    "ratio": ratio,
                    "dist_ko": dist_ko,
                    "spot": spot_price,
                    "spread": spread,
                    "bid_size": item.get('price', {}).get('bidSize'),
                    "time": item.get('price', {}).get('latestTimestamp'),
                    "is_tr": is_tr
                })

            results.sort(key=lambda x: x['leverage'] or 0, reverse=True)
            return results[:limit]

        except Exception as e:
            logger.error(f"Scan Error: {e}")
            return []

def output_results(data: List[Dict], output_format: str = "table"):
    if not data:
        if output_format == "json":
            print(json.dumps([], indent=2))
        elif output_format == "html":
            print("<p>No results found.</p>")
        else:
            print("No results found.")
        return

    if output_format == "json":
        print(json.dumps(data, indent=2, default=str))
        return

    if output_format == "html":
        print_html(data)
        return

    # Table format (default)
    header = f"{'ISIN':<18} {'Lev':<6} {'Strike':<8} {'Barrier':<8} {'Dist%':<6} {'Ratio':<5} {'Bid':<6} {'Ask':<6} {'Spread':<6} {'Liq':<6} {'Time':<8} {'Maturity':<10}"
    print(header)
    print("-" * len(header))

    for row in data:
        lev_str = f"{row['leverage']:.1f}"
        maturity = row['maturity'].split("T")[0] if row['maturity'] else "Open"
        strike = f"{row['strike']:.0f}" if row['strike'] else "-"
        barrier = f"{row['barrier']:.0f}" if row['barrier'] else "-"
        dist_str = f"{row['dist_ko']*100:.1f}%" if row['dist_ko'] else "-"
        ratio_str = f"{row['ratio']:.2f}"
        spread_str = f"{row['spread']:.2f}"
        liq_str = f"{row['bid_size']/1000:.0f}k" if row['bid_size'] else "-"
        
        isin_display = row['isin']
        if row['is_tr']:
            isin_display += " [TR]"

        time_str = "-"
        if row['time']:
            try:
                time_str = row['time'].split("T")[1].split(".")[0]
            except:
                pass

        print(f"{isin_display:<18} {lev_str:<6} {strike:<8} {barrier:<8} {dist_str:<6} {ratio_str:<5} {str(row['bid']):<6} {str(row['ask']):<6} {spread_str:<6} {liq_str:<6} {time_str:<8} {maturity:<10}")

def print_html(data: List[Dict]):
    style = """
    <style>
        body { font-family: sans-serif; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
        tr:nth-child(even) { background-color: #f9f9f9; }
        .long { color: green; }
        .short { color: red; }
        .risk { font-weight: bold; }
    </style>
    """
    
    html = f"""
    <!DOCTYPE html>
    <html>
    <head>{style}</head>
    <body>
    <h2>Turbo Warrants Scanner</h2>
    <table>
        <thead>
            <tr>
                <th>ISIN</th>
                <th>Type</th>
                <th>Leverage</th>
                <th>Strike</th>
                <th>Barrier</th>
                <th>Dist %</th>
                <th>Ratio</th>
                <th>Bid</th>
                <th>Ask</th>
                <th>Spread</th>
                <th>Liquidity</th>
                <th>Time</th>
                <th>Maturity</th>
            </tr>
        </thead>
        <tbody>
    """
    
    for row in data:
        dist_pct = row['dist_ko'] * 100 if row['dist_ko'] else 0
        risk_class = "risk" if dist_pct < 2.0 else ""
        type_class = "long" if row['type'] == "LONG" else "short"
        
        isin_display = row['isin']
        if row['is_tr']:
            isin_display += " <span style='font-size:0.8em; border:1px solid #ccc; padding:1px;'>TR</span>"

        time_str = "-"
        if row['time']:
            try:
                time_str = row['time'].split("T")[1].split(".")[0]
            except:
                pass

        html += f"""
            <tr>
                <td>{isin_display}</td>
                <td class="{type_class}">{row['type']}</td>
                <td class="{risk_class}">{row['leverage']:.2f}</td>
                <td>{row['strike']:.2f}</td>
                <td>{row['barrier']:.2f}</td>
                <td class="{risk_class}">{dist_pct:.2f}%</td>
                <td>{row['ratio']:.2f}</td>
                <td>{row['bid']}</td>
                <td>{row['ask']}</td>
                <td>{row['spread']:.2f}</td>
                <td>{row['bid_size']}</td>
                <td>{time_str}</td>
                <td>{row['maturity'] or 'Open End'}</td>
            </tr>
        """
    
    html += """
        </tbody>
    </table>
    </body>
    </html>
    """
    print(html)

def main():
    parser = argparse.ArgumentParser(description="Scanner for Turbo Warrants (SP500)")
    parser.add_argument("--type", choices=["LONG", "SHORT"], default="LONG", help="Direction (LONG/SHORT)")
    parser.add_argument("--limit", type=int, default=20, help="Number of results to show")
    parser.add_argument("-tr", "--trade-republic", action="store_true", help="Filter for Trade Republic compatible products")
    parser.add_argument("--format", choices=["table", "json", "html"], default="table", help="Output format")
    
    args = parser.parse_args()

    scanner = VontobelScanner()
    results = scanner.scan(underlying_symbol="SPX", direction=args.type, limit=args.limit, tr_only=args.trade_republic)
    output_results(results, args.format)

if __name__ == "__main__":
    main()