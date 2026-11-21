from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'app/src/main/java/com/ekbatan/hse'
errors=0
def check(ok,msg):
 global errors
 print(('PASS ' if ok else 'FAIL ')+msg)
 if not ok: errors+=1
repo=(ROOT/'HseDataRepository.java').read_text()
ma=(ROOT/'MainActivity.java').read_text()
features='\n'.join(p.read_text() for p in ROOT.glob('*Feature.java'))
check((ROOT/'HseDataRepository.java').is_file(),'repository exists')
for x in ['private final LibraryDb db','addNote','addReminder','addRisk','addIncident','listDocuments','searchHits','stats','integrityOk']:
 check(x in repo,f'repository contract: {x}')
check('host.db.' not in features,'features do not access LibraryDb directly')
check('HseDataRepository data' in ma,'MainActivity composes repository')
check('data=new HseDataRepository(db)' in ma,'repository initialized with DB')
check('data=new HseDataRepository(db);if(!data.integrityOk())' in ma,'repository recreated after restore')
check('db.close();' in ma,'MainActivity remains owner of DB lifecycle')
check('Room' not in repo and 'Hilt' not in repo,'no risky framework migration in this step')
print(f'DATA LAYER QA errors={errors}')
raise SystemExit(1 if errors else 0)
