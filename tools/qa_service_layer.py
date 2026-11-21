from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def ok(x): print('PASS',x)
def fail(x): errors.append(x); print('FAIL',x)
main=(ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java').read_text()
svc=(ROOT/'app/src/main/java/com/ekbatan/hse/HseAppServices.java').read_text()
repo=(ROOT/'app/src/main/java/com/ekbatan/hse/HseDataRepository.java').read_text()
checks=[
 ('service class', 'public final class HseAppServices'),
 ('service owns AI', 'private HseVisionAi visionAi'),
 ('service backup create', 'BackupManager.create'),
 ('service backup restore', 'BackupManager.restore'),
 ('service PDF import', 'PdfImporter.importPdf'),
 ('service reminder scheduling', 'ReminderScheduler.schedule'),
 ('service reminder cancel', 'ReminderScheduler.cancel'),
 ('service AI analysis', 'visionAi.analyzeImage'),
 ('service close', 'public void close()'),
]
for name,n in checks:
 ok(name) if n in svc else fail(name)
for name,n in [('main composes services','services=new HseAppServices(this,data)'),('main AI delegates','services.analyzeImage'),('main backup delegates','services.createBackup'),('main restore recreates services','services=new HseAppServices(this,data)')]:
 ok(name) if n in main else fail(name)
if 'rawDatabaseForService()' in repo and 'LibraryDb rawDatabaseForService()' in repo: ok('repository service bridge')
else: fail('repository service bridge')
if 'visionAi.analyzeImage' not in main: ok('MainActivity does not execute AI engine directly')
else: fail('MainActivity direct AI execution remains')
if 'BackupManager.create' not in main and 'BackupManager.restore' not in main: ok('MainActivity does not execute backup engine directly')
else: fail('MainActivity direct backup execution remains')
print('SERVICE LAYER QA:', 'PASS' if not errors else 'FAIL', 'errors=',len(errors))
if errors: raise SystemExit(1)
