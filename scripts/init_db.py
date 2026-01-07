import sqlite3
import os

db_path = 'data/tearsmind.db'

# DDL Schema
schema = """
CREATE TABLE IF NOT EXISTS daily_analysis (
    date TEXT PRIMARY KEY,
    data JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS quant_memory (
    date TEXT PRIMARY KEY,
    data JSON NOT NULL
);
"""

try:
    # Connect (creates file if not exists)
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # Execute DDL
    cursor.executescript(schema)
    
    conn.commit()
    conn.close()
    print(f"SUCCESS: Database created/verified at {os.path.abspath(db_path)}")
    print("Tables 'daily_analysis' and 'quant_memory' are ready.")
    
except Exception as e:
    print(f"ERROR: {e}")
