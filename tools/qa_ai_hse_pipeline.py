from pathlib import Path
import re, sys
ROOT=Path(__file__).resolve().parents[1]
files={p.name:p.read_text(encoding='utf-8') for p in (ROOT/'app/src/main/java/com/ekbatan/hse').glob('*.java')}
errors=[]
def check(name,cond):
    print(('PASS ' if cond else 'FAIL ')+name)
    if not cond: errors.append(name)
lib=files['LibraryDb.java']; repo=files['HseDataRepository.java']; main=files['MainActivity.java']; vision=files['VisionAiFeature.java']; parser=files['HseAiFinding.java']; pdf=files['ExportPrintManager.java']; backup=files['BackupManager.java']
check('DB schema version 13','VER = 13' in lib)
check('finding table onCreate','CREATE TABLE inspection_findings' in lib)
check('finding table migration','CREATE TABLE IF NOT EXISTS inspection_findings' in lib)
check('finding risk foreign key','FOREIGN KEY(risk_id) REFERENCES risks(id) ON DELETE SET NULL' in lib)
check('finding persistence method','addInspectionFinding' in lib and 'listInspectionFindings' in lib)
check('repository finding boundary','addInspectionFinding' in repo and 'linkFindingToRisk' in repo)
check('structured AI parser','class HseAiFinding' in parser and 'parse(String raw)' in parser)
check('structured parser preserves consequence','splitHazardConsequence' in parser and 'f.consequence=parts[1]' in parser)
check('human approval button','تأیید و ثبت یافته' in vision)
check('risk approval warning','پیشنهاد اولیه هوش مصنوعی' in main)
check('AI finding to risk link','linkFindingToRisk' in main)
check('inspection report builder','buildAiInspectionReport' in main)
check('direct PDF creation','createPdf(File target' in pdf)
check('PDF save flow','CREATE_AI_PDF' in main and 'saveAiPdf' in main)
check('AI UI advertises full pipeline','تصویر → تحلیل HSE → تأیید انسانی' in vision)
check('backup schema 13','DB_VERSION = 13' in backup)
check('backup validates findings','inspection_findings' in backup)
# Guardrail: no automatic risk insertion during parsing/analysis
check('no automatic risk in parser','addRisk' not in parser)
print('\nAI→HSE→RISK→REPORT→PDF QA','PASS' if not errors else f'FAIL errors={len(errors)}')
if errors: sys.exit(1)
