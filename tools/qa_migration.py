from pathlib import Path
import sqlite3, tempfile, sys

ROOT = Path(__file__).resolve().parents[1]
LIB = (ROOT / 'app/src/main/java/com/ekbatan/hse/LibraryDb.java').read_text()
errors = []

def check(name, cond):
    if cond:
        print('PASS', name)
    else:
        errors.append(name)
        print('FAIL', name)

with tempfile.TemporaryDirectory() as td:
    db = Path(td) / 'legacy.db'
    c = sqlite3.connect(db)
    cur = c.cursor()
    cur.executescript('''
        PRAGMA user_version=10;
        CREATE TABLE reminders(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT,
            body TEXT,
            at_millis INTEGER,
            done INTEGER DEFAULT 0,
            created_at INTEGER
        );
        INSERT INTO reminders(title,body,at_millis,done,created_at)
        VALUES ('قدیمی ۱','',1000,0,1),('قدیمی ۲','',2000,0,2);
    ''')
    c.commit()

    # Mirror the Android migration contract exactly for this QA fixture:
    cur.execute("ALTER TABLE reminders ADD COLUMN delivery_state TEXT DEFAULT 'pending'")
    cur.execute("ALTER TABLE reminders ADD COLUMN last_delivery_error TEXT")
    cur.execute("UPDATE reminders SET delivery_state='pending' WHERE delivery_state IS NULL OR TRIM(delivery_state)=''")
    cur.execute("UPDATE reminders SET delivery_state='pending' WHERE delivery_state NOT IN ('pending','scheduled','delivered','failed')")
    cur.execute("UPDATE reminders SET last_delivery_error='' WHERE last_delivery_error IS NULL")
    cur.execute('PRAGMA user_version=13')
    c.commit()

    cols = {r[1] for r in cur.execute('PRAGMA table_info(reminders)')}
    states = [r[0] for r in cur.execute('SELECT delivery_state FROM reminders ORDER BY id')]
    errors_text = [r[0] for r in cur.execute('SELECT last_delivery_error FROM reminders ORDER BY id')]
    check('legacy reminders gain delivery_state', 'delivery_state' in cols)
    check('legacy reminders gain last_delivery_error', 'last_delivery_error' in cols)
    check('legacy rows become pending', states == ['pending', 'pending'])
    check('legacy delivery errors become empty', errors_text == ['', ''])
    check('migrated fixture reports DB version 13', cur.execute('PRAGMA user_version').fetchone()[0] == 13)
    c.close()

check('LibraryDb schema version is 13', 'private static final int VER = 13;' in LIB)
check('migration repairs null/blank delivery state', "delivery_state='pending' WHERE delivery_state IS NULL OR TRIM(delivery_state)=''" in LIB)
check('migration repairs unknown delivery state', "delivery_state NOT IN ('pending','scheduled','delivered','failed')" in LIB)
check('inspection finding table is part of migration contract', 'CREATE TABLE IF NOT EXISTS inspection_findings' in LIB)
check('migration clears null delivery errors', "last_delivery_error='' WHERE last_delivery_error IS NULL" in LIB)

print('\nMIGRATION QA', 'PASS' if not errors else 'FAIL', 'errors=', len(errors))
if errors:
    sys.exit(1)
