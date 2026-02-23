import requests
import json

def probe_socgen(isin):
    # Potential API endpoints to test
    endpoints = [
        f"https://www.sg-zertifikate.de/api/product/{isin}",
        f"https://www.sg-zertifikate.de/api/search?q={isin}",
        f"https://www.sg-zertifikate.de/product-detail/{isin}" 
    ]

    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36',
        'Accept': 'application/json, text/plain, */*'
    }

    requests.packages.urllib3.disable_warnings()
    
    for url in endpoints:
        print(f"Probing: {url}")
        try:
            resp = requests.get(url, headers=headers, timeout=10, verify=False)
            print(f"Status: {resp.status_code}")
            
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    print("SUCCESS! JSON Found.")
                    print(json.dumps(data, indent=2)[:500])
                    return
                except:
                    print("Response is not JSON. Previewing HTML content for clues:")
                    print(resp.text[:1000]) # Print start of HTML to see scripts
        except Exception as e:
            print(f"Error: {e}")

if __name__ == "__main__":
    probe_socgen("DE000FD28WH8")
