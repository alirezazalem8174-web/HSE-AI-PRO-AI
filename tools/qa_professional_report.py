from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
errors=0
def ok(x): print("PASS",x)
def fail(x):
 global errors; errors+=1; print("FAIL",x)
p=(ROOT/"app/src/main/java/com/ekbatan/hse/ProfessionalReportManager.java").read_text()
checks=[
 ("RTL right alignment", "Paint.Align.RIGHT" in p and "drawRtl" in p),
 ("wrapped report fields", "drawWrappedRtl" in p and "StaticLayout" in p),
 ("report text bounded", "clean(value" in p and "ثبت نشده" in p),
 ("user approval disclaimer", "تأیید نهایی با مسئول HSE" in p),
]
for n,c in checks:
 (ok(n) if c else fail(n))
print("PROFESSIONAL REPORT QA errors=",errors)
raise SystemExit(1 if errors else 0)
