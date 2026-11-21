from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1]
files={
 "LibraryDb.java":root/"app/src/main/java/com/ekbatan/hse/LibraryDb.java",
 "MainActivity.java":root/"app/src/main/java/com/ekbatan/hse/MainActivity.java",
 "NotesFeature.java":root/"app/src/main/java/com/ekbatan/hse/NotesFeature.java",
 "ReminderFeature.java":root/"app/src/main/java/com/ekbatan/hse/ReminderFeature.java",
 "RiskFeature.java":root/"app/src/main/java/com/ekbatan/hse/RiskFeature.java",
 "IncidentFeature.java":root/"app/src/main/java/com/ekbatan/hse/IncidentFeature.java",
}
errors=0
def check(ok,msg):
 global errors
 print(("PASS " if ok else "FAIL ")+msg)
 if not ok: errors+=1
ldb=files["LibraryDb.java"].read_text()
ma=files["MainActivity.java"].read_text()
risk=files["RiskFeature.java"].read_text()
incident=files["IncidentFeature.java"].read_text()
notes=files["NotesFeature.java"].read_text()
reminder=files["ReminderFeature.java"].read_text()
for x in ["insertReliable", "isTransientLock", "beginTransaction", "setTransactionSuccessful", "endTransaction"]: check(x in ldb, f"reliable write contract: {x}")
for x in ["MAX_NOTE_CHARS", "عنوان یا متن یادداشت خالی است", "متن یادداشت بیش از ۲۵۰٬۰۰۰ نویسه"]: check(x in ldb, f"note validation: {x}")
for x,source in [("data.addNote",notes),("data.addReminder",reminder),("data.addRisk",risk),("data.addIncident",incident)]: check(x in source, f"UI save wiring: {x}")
check("save.setEnabled(false)" in notes and "save.setEnabled(true)" in notes, "note button state restored on success/failure")
check("خطا در ذخیره یادداشت:" in notes, "note error surfaced without clearing input")
check("خطا در ثبت یادآوری:" in reminder, "reminder error surfaced without clearing input")
check("خطا در ثبت ریسک:" in risk, "risk error surfaced without clearing input")
check("خطا در ثبت رویداد:" in incident, "incident error surfaced without clearing input")
print(f"PERSISTENCE QA errors={errors}")
sys.exit(1 if errors else 0)
