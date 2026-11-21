#!/usr/bin/env python3
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
main=ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java'
feat=ROOT/'app/src/main/java/com/ekbatan/hse/ui/HseFeature.java'
coord=ROOT/'app/src/main/java/com/ekbatan/hse/ui/HseFeatureCoordinator.java'
for p in (main,feat,coord):
    if not p.is_file(): raise SystemExit(f'FAIL missing {p}')
s=main.read_text(encoding='utf-8')
for x in ['HseFeatureCoordinator','LibraryFeature','GlossaryFeature','NotesFeature','ReminderFeature','RiskFeature','IncidentFeature','BackupFeature','VisionAiFeature']:
    if x not in s: raise SystemExit(f'FAIL missing architecture marker: {x}')
cs=coord.read_text(encoding='utf-8')
for x in ['Map<String, HseFeature>','navigator.get(key)','public boolean contains(String key)','public void clear()']:
    if x not in cs: raise SystemExit(f'FAIL coordinator contract: {x}')
expected={'LibraryFeature','GlossaryFeature','NotesFeature','ReminderFeature','RiskFeature','IncidentFeature','BackupFeature','VisionAiFeature'}
keys=set(re.findall(r'features\.add\([^\n]*?(LibraryFeature|GlossaryFeature|NotesFeature|ReminderFeature|RiskFeature|IncidentFeature|BackupFeature|VisionAiFeature)',s))
if keys != expected: raise SystemExit(f'FAIL expected feature set {expected}, got {keys}')
print('PASS: architecture step 3 feature coordinator')
