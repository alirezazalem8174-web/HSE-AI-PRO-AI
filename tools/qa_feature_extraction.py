from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/ekbatan/hse'
main=(src/'MainActivity.java').read_text()
classes=['LibraryFeature','GlossaryFeature','NotesFeature','ReminderFeature','RiskFeature','IncidentFeature','BackupFeature','VisionAiFeature']
checks=[(f'{c} class',(src/f'{c}.java').is_file()) for c in classes]
checks += [
('main no legacy library builder','void buildLibrary(' not in main),
('main no legacy glossary builder','void buildGlossary(' not in main),
('main no legacy notes builder','void buildNotes(' not in main),
('main no legacy reminder builder','void buildReminder(' not in main),
('main no legacy backup builder','void buildBackup(' not in main),
('risk factory','features.add(RiskFeature::new);' in main),
('incident factory','features.add(IncidentFeature::new);' in main),
('vision factory','features.add(VisionAiFeature::new);' in main),
('library factory','features.add(LibraryFeature::new);' in main),
('glossary factory','features.add(GlossaryFeature::new);' in main),
('notes factory','features.add(NotesFeature::new);' in main),
('backup factory','features.add(BackupFeature::new);' in main),
('reminder feature factory','new ReminderFeature(this)' in main),
('AI image callback cleanup','aiImageReadyAction=null;' in main),
]
errors=[name for name,ok in checks if not ok]
for name,ok in checks: print(('PASS' if ok else 'FAIL'),name)
print('FEATURE EXTRACTION QA:', 'PASS' if not errors else f'FAIL errors={len(errors)}')
raise SystemExit(1 if errors else 0)
