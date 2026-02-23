import requests
import re
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("SocGenReverser")

BASE_URL = "https://www.sg-zertifikate.de"

def reverse_engineer():
    session = requests.Session()
    session.headers.update({
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    })
    requests.packages.urllib3.disable_warnings()

    # Step 1: Get Index HTML
    logger.info("Fetching index page...")
    resp = session.get(f"{BASE_URL}/product-detail/DE000FD28WH8", verify=False)
    if resp.status_code != 200:
        logger.error("Failed to fetch index.")
        return

    html = resp.text
    
    # Step 2: Find main-*.js
    # Pattern: src="main-([A-Z0-9]+).js" or similar
    match = re.search(r'src="(main-[A-Z0-9]+\.js)"', html)
    if not match:
        # Try finding inside <script> tags if it's module loading
        match = re.search(r'"(main-[A-Z0-9]+\.js)"', html)
    
    if not match:
        logger.error("Could not find main.js reference in HTML.")
        # Debug: print script tags
        scripts = re.findall(r'<script.*?>', html)
        logger.info(f"Found scripts tags: {scripts}")
        return

    js_filename = match.group(1)
    logger.info(f"Found JS file: {js_filename}")

    # Step 3: Fetch JS Content
    js_url = f"{BASE_URL}/{js_filename}"
    logger.info(f"Downloading JS: {js_url}")
    js_resp = session.get(js_url, verify=False)
    
    if js_resp.status_code != 200:
        logger.error("Failed to fetch JS file.")
        return

    js_content = js_resp.text

    # Step 4: Analyze JS for API patterns
    logger.info("Analyzing JS for API endpoints...")
    
    # Look for variable assignments that look like API configs
    # e.g., apiBaseUrl:"..." or path:"/api/..."
    
    api_patterns = [
        r'api/v\d+/[a-zA-Z0-9\-_/]+',
        r'"https://[^"]*api[^"]*"',
        r'baseUrl:"([^"]+)"',
        r'apiUrl:"([^"]+)"',
        r'/api/[a-zA-Z0-9\-_/]+'
    ]

    found_something = False
    for pattern in api_patterns:
        matches = re.findall(pattern, js_content)
        if matches:
            found_something = True
            logger.info(f"MATCH FOUND for '{pattern}':")
            # Limit output to unique matches
            unique_matches = list(set(matches))
            for m in unique_matches[:10]: # Show top 10
                logger.info(f"  - {m}")

    if not found_something:
        logger.warning("No obvious API patterns found. The code might be heavily obfuscated or using relative paths constructed at runtime.")
        
        # Fallback: Look for "prices" or "product" context
        logger.info("Deep search for 'product' related calls...")
        context_matches = re.findall(r'[\w\.]+\.get\("([^"]*product[^"]*)"\)', js_content)
        for m in context_matches:
             logger.info(f"  - Possible GET call: {m}")

if __name__ == "__main__":
    reverse_engineer()
