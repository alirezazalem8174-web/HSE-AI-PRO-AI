#!/usr/bin/env python3
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main/java/com/ekbatan/hse/MainActivity.java'
BACKUP = ROOT / 'app/src/main/java/com/ekbatan/hse/BackupManager.java'

errors=[]
main=MAIN.read_text(encoding='utf-8')
backup=BACKUP.read_text(encoding='utf-8')

def require(text, needle, label):
    if needle not in text:
        errors.append(f'missing: {label}')

def forbid(text, needle, label):
    if needle in text:
        errors.append(f'forbidden: {label}')

# MainActivity: all async DB/UI work goes through lifecycle-aware gates.
forbid(main, 'io.execute(', 'direct executor submission outside submitIo()')

# Only the centralized postUi() helper may call Activity.runOnUiThread().
ui_calls = [m.start() for m in re.finditer(r'runOnUiThread\(', main)]
if len(ui_calls) != 1:
    errors.append(f'expected exactly one runOnUiThread() call (inside postUi), found {len(ui_calls)}')
require(main, 'volatile boolean destroyed=false;', 'destroyed lifecycle flag')
require(main, 'void submitIo(Runnable r)', 'centralized IO submission')
require(main, 'void postUi(Runnable r)', 'centralized UI posting')
require(main, 'executor.shutdown();', 'graceful executor shutdown')
require(main, 'executor.awaitTermination(30,TimeUnit.SECONDS)', 'wait for in-flight DB work')
require(main, 'executor.shutdownNow();', 'bounded shutdown fallback')
require(main, 'if(terminated){', 'DB close only after executor termination')
require(main, 'LibraryDb d=db;\n                        db=null;', 'single lifecycle close handoff')
require(main, 'volatile boolean backupFlowActive=false;', 'backup flow serialization state')
require(main, 'if(backupFlowActive){toast("یک عملیات پشتیبان‌گیری در حال انجام است.");return;}', 'duplicate backup guard')
require(main, 'if(backupFlowActive){toast("ابتدا عملیات ذخیره پشتیبان جاری را تمام یا لغو کنید.");return;}', 'restore blocked while backup save is pending')
require(main, 'pendingBackupFile=null;backupFlowActive=false;', 'backup cancellation cleanup')

# Restore must not race with another backup/restore operation in-process.
require(backup, 'private static final Object BACKUP_LOCK = new Object();', 'backup/restore process lock')
require(backup, 'synchronized(BACKUP_LOCK)', 'serialized backup/restore entry point')
require(backup, 'copyVerified(dbf,staged)', 'durable staged DB copy')
require(backup, 'copyVerified(staged,oldDb)', 'durable DB install fallback')

# The database connection used by MainActivity is closed before physical DB replacement.
restore_start=main.find('void restore(Uri u)')
if restore_start < 0:
    errors.append('restore() not found')
else:
    restore_body=main[restore_start:]
    close_pos=restore_body.find('if(db!=null){db.close();db=null;}')
    backup_pos=restore_body.find('oldServices.restoreBackup(f);')
    service=ROOT / 'app/src/main/java/com/ekbatan/hse/HseAppServices.java'
    service_text=service.read_text(encoding='utf-8')
    if close_pos < 0 or backup_pos < 0 or close_pos > backup_pos:
        errors.append('restore() does not close the active DB before service restore')
    if 'BackupManager.restore(activity, stagedZip);' not in service_text:
        errors.append('service restore boundary does not delegate to BackupManager.restore()')

# No lifecycle path may silently instantiate a fresh DB merely because restore failed.
if 'db=new LibraryDb(this);' not in restore_body:
    errors.append('restore success path missing DB reopen')
if 'candidate=new LibraryDb(this)' not in restore_body:
    errors.append('restore failure recovery candidate missing')
if 'finish();' not in restore_body:
    errors.append('restore failure does not terminate safely when recovery is unavailable')

# Ensure there is no obvious direct DB close from MainActivity outside restore() or onDestroy().
restore_start=main.find('void restore(Uri u)')
destroy_start=main.find('@Override protected void onDestroy()')
for m in re.finditer(r'\bdb\.close\(\)', main):
    pos=m.start()
    if not ((restore_start >= 0 and restore_start <= pos < destroy_start) or (destroy_start >= 0 and pos >= destroy_start)):
        line=main.count('\n',0,pos)+1
        errors.append(f'unexpected db.close() at MainActivity line {line}')

if errors:
    print('CONCURRENCY HARDENING QA: FAIL')
    for e in errors: print(' -',e)
    sys.exit(1)
print('CONCURRENCY HARDENING QA: PASS')
print(' - lifecycle-aware IO gate present')
print(' - lifecycle-aware UI gate present')
print(' - executor drains before DB close')
print(' - backup/restore operations serialized in-process')
print(' - durable DB copy fallbacks used for restore staging/install')
print(' - restore closes active DB before physical replacement')
