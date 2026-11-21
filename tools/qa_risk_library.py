from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
lib=(ROOT/'app/src/main/java/com/ekbatan/hse/LibraryDb.java').read_text()
repo=(ROOT/'app/src/main/java/com/ekbatan/hse/HseDataRepository.java').read_text()
feat=(ROOT/'app/src/main/java/com/ekbatan/hse/RiskLibraryFeature.java').read_text()
main=(ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java').read_text()
backup=(ROOT/'app/src/main/java/com/ekbatan/hse/BackupManager.java').read_text()
errors=[]
def check(n,c):
 print(('PASS ' if c else 'FAIL ')+n)
 if not c: errors.append(n)
check('schema version 13','VER = 13' in lib)
check('NCR table onCreate','CREATE TABLE ncrs' in lib)
check('NCR table migration','CREATE TABLE IF NOT EXISTS ncrs' in lib)
check('NCR persistence','addNcr' in lib and 'listNcrs' in lib and 'deleteNcr' in lib)
check('repository NCR boundary','addNcr' in repo and 'listNcrs' in repo)
check('risk library feature','class RiskLibraryFeature' in feat)
check('finding filtering','listInspectionFindings' in feat and 'جست‌وجوی یافته' in feat)
check('two-image compare','compareSelected' in feat and 'مقایسه دو تصویر' in feat)
check('NCR UI','showNcr' in feat and 'ایجاد گزارش عدم انطباق' in feat)
check('feature registered','RiskLibraryFeature::new' in main)
check('backup validates NCR','ncrs' in backup)
check('no paid service in feature','Firebase' not in feat and 'http' not in feat)
print('\nRISK LIBRARY → IMAGE COMPARE → NCR QA','PASS' if not errors else f'FAIL errors={len(errors)}')
if errors: sys.exit(1)
