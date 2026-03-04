import requests
import json
from bs4 import BeautifulSoup
import time
import argparse
import string
from typing import List, Dict, Optional, Tuple

def get_product_data(isin: str) -> Optional[Dict]:
    """
    Fetches the detailed backend data for a single ISIN from Vontobel via redirect.
    This contains the flags we need for validation.
    """
    url = f"https://markets.vontobel.com/cms/de-de/productredirect/{isin}"
    try:
        r = requests.get(url, headers={'User-Agent': 'Mozilla/5.0'}, timeout=7)
        if "__NEXT_DATA__" not in r.text:
            return None
            
        soup = BeautifulSoup(r.text, 'html.parser')
        next_data_script = soup.find('script', id='__NEXT_DATA__')
        if not next_data_script:
            return None
            
        data = json.loads(next_data_script.string)
        page_props = data.get('props', {}).get('pageProps', {}).get('additionalData', {})
        product_data = page_props.get('data', {})
        
        # Extract leverage if not in data
        if 'leverage' not in product_data:
            # Try primaryFeatures
            features = page_props.get('primaryFeatures', [])
            for f in features:
                if f.get('type') == 27 or 'Leverage' in f.get('$type', ''):
                    product_data['leverage'] = f.get('leverage')
                    break
            
            # Try keyFigures if still missing
            if 'leverage' not in product_data:
                kfs = page_props.get('keyFigures', [])
                for kf in kfs:
                    if kf.get('name') == 7: # 7 is often leverage ratio
                        product_data['leverage'] = kf.get('value', {}).get('ratio')
                        break

        return product_data
    except (requests.exceptions.RequestException, json.JSONDecodeError, AttributeError):
        return None

def verify_and_get_data_with_all_rules(isin: str, expected_direction: int) -> Optional[Tuple]:
    """
    Performs the full 5-rule validation on a single ISIN.
    Rules: TR compatible, Type 5, Active, S&P 500, AND correct Direction.
    """
    prod_data = get_product_data(isin)
    if not prod_data:
        return None

    # Rule 1: Trade Republic Compatibility
    if not prod_data.get('isTradeRepublic'): 
        return None
    
    # Rule 2: Product Type (5 = Turbo-Optionsschein Open-End, 2 = Mini-Future)
    if prod_data.get('productType') not in [5, 2]: 
        return None
    
    # Rule 3: Active Status
    if prod_data.get('isActive') is not True: 
        return None

    # Rule 4: Underlying name check
    name = prod_data.get('name', '')
    if 'S&P 500' not in name:
        return None
            
    # Rule 5: Direction check (1: LONG, 2: SHORT)
    if prod_data.get('direction') != expected_direction:
        return None

    return (
        isin,
        prod_data.get('leverage'),
        prod_data.get('knockOut')
    )

def calculate_isin_checksum(isin_base: str) -> str:
    """Calculates ISIN checksum (Luhn-like for alphanumeric)."""
    s = ""
    for char in isin_base:
        if char.isdigit(): s += char
        else: s += str(ord(char) - ord('A') + 10)
    total = 0
    for i, digit in enumerate(reversed(s)):
        val = int(digit)
        if i % 2 == 0: val *= 2
        total += val // 10
        total += val % 10
    return str((10 - (total % 10)) % 10)

def generate_isin_series(prefix: str) -> List[str]:
    """Generates a series of ISINs for a given prefix."""
    charset = string.ascii_uppercase + string.digits
    return [prefix + c + calculate_isin_checksum(prefix + c) for c in charset]

def fetch_candidate_pool(direction: int, silent: bool = False) -> List[Dict]:
    """
    Fetches candidates from multiple sources:
    1. Vontobel's search page (dynamic candidates).
    2. Known TR-compatible ISIN series (fallback/proactive).
    """
    url = "https://markets.vontobel.com/de-de/produkte/hebel/turbo-optionsscheine-open-end"
    params = {
        'underlying': 70, 
        'direction': direction,
        'sort': 'leverage', 
        'order': 'desc'
    }
    
    all_isins = set()
    session = requests.Session()
    session.headers.update({'User-Agent': 'Mozilla/5.0'})

    dir_name = "LONG" if direction == 1 else "SHORT"
    if not silent:
        print(f"Gathering S&P 500 candidates for direction {dir_name}...")
    
    # Source 1: Search Page
    try:
        r = session.get(url, params=params, timeout=10)
        if r.status_code == 200:
            soup = BeautifulSoup(r.text, 'html.parser')
            next_data = soup.find('script', id='__NEXT_DATA__')
            if next_data:
                data = json.loads(next_data.string)
                items = data.get('props', {}).get('pageProps', {}).get('additionalData', {}).get('productSearchData', {}).get('items', [])
                for item in items:
                    if item.get('isin'): all_isins.add(item['isin'])
            if not silent:
                print(f"  [Source: Search] Found {len(all_isins)} candidates.")
    except Exception: pass

    # Source 2: Known Prefixes (Walking)
    # These are series known to contain S&P 500 products for Trade Republic
    prefixes = ["DE000VH9L7", "DE000VH2YM", "DE000VJ6EM", "DE000VJ6ZN", "DE000VH0WM", "DE000VH0Z8"]
    if direction == 2: # Add known short prefixes
        prefixes += ["DE000VJ6XS", "DE000VJ60V"]
    
    walk_count = 0
    for prefix in prefixes:
        series = generate_isin_series(prefix)
        for isin in series:
            if isin not in all_isins:
                all_isins.add(isin)
                walk_count += 1
                
    if not silent:
        print(f"  [Source: Walking] Added {walk_count} proactive candidates.")
    return [{"isin": isin} for isin in all_isins]
    
    try:
        r = session.get(url, params=params, timeout=10)
        if r.status_code == 200:
            soup = BeautifulSoup(r.text, 'html.parser')
            next_data = soup.find('script', id='__NEXT_DATA__')
            if not next_data:
                print("  [ERROR] No __NEXT_DATA__ found in search response.")
                return []
            data = json.loads(next_data.string)
            items = data.get('props', {}).get('pageProps', {}).get('additionalData', {}).get('productSearchData', {}).get('items', [])
            print(f"Found {len(items)} candidates from search page.")
            return items
        else:
            print(f"  [ERROR] Search page failed with status {r.status_code}")
            return []
    except Exception as e:
        print(f"  [ERROR] Candidate Pool Fetch Failed: {e}")
        return []

def run_scanner(direction: int, limit: int, output_format: str = 'table'):
    """
    Fetches, validates, and prints a list of TR-compatible ISINs.
    """
    silent = (output_format == 'json')
    dir_name = "LONG" if direction == 1 else "SHORT"
    candidates = fetch_candidate_pool(direction, silent=silent)
    
    if not silent:
        print(f"\n--- Verifying {dir_name} S&P 500 ISINs (Limit: {limit}) ---")
    
    found_count = 0
    verified_products = []
    
    for candidate in candidates:
        if found_count >= limit:
            break

        isin = candidate.get('isin')
        if not isin: continue

        if not silent:
            print(f"  Checking {isin}...", end='', flush=True)
        
        result = verify_and_get_data_with_all_rules(isin, direction)
        
        if result:
            verified_products.append({
                "isin": result[0], 
                "leverage": result[1], 
                "strike": result[2],
                "direction": dir_name,
                "underlying": "S&P 500"
            })
            if not silent:
                lev_val = result[1] if result[1] is not None else 0
                print(f" -> [OK] Found! (Lev: {lev_val:.2f})")
            found_count += 1
        else:
            if not silent:
                print(" -> [FAIL]")
            
        time.sleep(0.3) # Respectful delay

    if not silent:
        print("\n--- SCAN COMPLETE ---")
        print(f"\n✅ Found {len(verified_products)} verified {dir_name} products for S&P 500 on Trade Republic:")
    
    print_results(verified_products, output_format)

def print_results(products: List[Dict], output_format: str = 'table'):
    if not products:
        if output_format == 'json':
            print("[]")
        else:
            print("None found.")
        return
        
    products.sort(key=lambda p: p.get('leverage', 0) or 0, reverse=True)
    
    if output_format == 'json':
        print(json.dumps(products, indent=2))
    else:
        print_table(products)

def print_table(products: List[Dict]):
    print(f"{'ISIN':<15} | {'Leverage':<12} | {'Strike (Barrier)':<18}")
    print(f"{'-'*15} | {'-'*12} | {'-'*18}")
    for p in products:
        lev = f"{p.get('leverage', 'N/A'):.2f}x" if isinstance(p.get('leverage'), (int, float)) else 'N/A'
        strike = f"{p.get('strike', 'N/A'):.2f}" if isinstance(p.get('strike'), (int, float)) else 'N/A'
        print(f"{p['isin']:<15} | {lev:<12} | {strike:<18}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Find Vontobel Turbo Warrants for S&P 500 available on Trade Republic.")
    parser.add_argument('--type', type=str.upper, choices=['LONG', 'SHORT'], required=True, help="Scan for LONG or SHORT products.")
    parser.add_argument('--limit', type=int, default=10, help="Number of products to find.")
    parser.add_argument('--format', type=str.lower, choices=['table', 'json'], default='table', help="Output format: table (default) or json.")
    args = parser.parse_args()

    direction = 1 if args.type == 'LONG' else 2
    run_scanner(direction, args.limit, args.format)
