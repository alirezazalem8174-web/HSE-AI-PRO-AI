from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
main=(ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java').read_text()
svc=(ROOT/'app/src/main/java/com/ekbatan/hse/HseAppServices.java').read_text()
errors=[]
def check(cond,msg):
    print(('PASS ' if cond else 'FAIL ')+msg)
    if not cond: errors.append(msg)
check('HseAppServices oldServices=services' in main,'restore captures current service before disposal')
check('oldServices.close();oldServices.restoreBackup(f);services=null;' in main,'restore closes then delegates restore before nulling service')
check('if(oldServices==null)throw new IllegalStateException("services unavailable");' in main,'restore fails closed when service boundary is unavailable')
check('services.restoreBackup(f);' not in main,'restore does not dereference mutable service field after nulling')
check('BackupManager.restore(activity, stagedZip);' in svc,'service restore delegates to BackupManager')
check('db.close();db=null;' in main,'database closes before physical restore')
check('db=new LibraryDb(this);data=new HseDataRepository(db);' in main,'database/repository recreated after restore')
check('services=new HseAppServices(this,data);' in main,'service recreated after restore')
print('RESTORE LIFECYCLE QA:', 'PASS' if not errors else f'FAIL errors={len(errors)}')
raise SystemExit(1 if errors else 0)
