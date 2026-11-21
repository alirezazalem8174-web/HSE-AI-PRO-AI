#!/usr/bin/env python3
from pathlib import Path
import re
ROOT=Path(__file__).resolve().parents[1]
coord=(ROOT/'app/src/main/java/com/ekbatan/hse/ui/HseFeatureCoordinator.java').read_text(encoding='utf-8')
nav=(ROOT/'app/src/main/java/com/ekbatan/hse/ui/HseSectionNavigator.java').read_text(encoding='utf-8')
main=(ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java').read_text(encoding='utf-8')
checks=[
('coordinator prefers explicit registration','final View registered = navigator.get(key);' in coord),
('coordinator fallback only when absent','if (registered == null)' in coord),
('duplicate feature protection','features.containsKey(key)' in coord),
('navigator duplicate protection','sections.containsKey(key)' in nav),
('navigator lifecycle guard','view.getParent() == null' in nav),
('coordinator clear','public void clear()' in coord),
('navigator clear','public void clear()' in nav),
('eight extracted features',set(re.findall(r'features\.add\([^\n]*?(LibraryFeature|GlossaryFeature|NotesFeature|ReminderFeature|RiskFeature|IncidentFeature|BackupFeature|VisionAiFeature)',main)) >= {'LibraryFeature','GlossaryFeature','NotesFeature','ReminderFeature','RiskFeature','IncidentFeature','BackupFeature','VisionAiFeature'}),
]
for name,ok in checks:
    if not ok: raise SystemExit('FAIL: '+name)
print('PASS: architecture hardening contracts')
