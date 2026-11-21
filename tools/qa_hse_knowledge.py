from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def ok(n): print('PASS',n)
def fail(n,m): errors.append(f'{n}: {m}'); print('FAIL',n,m)
ai=(ROOT/'app/src/main/java/com/ekbatan/hse/HseVisionAi.java').read_text()
know=(ROOT/'app/src/main/java/com/ekbatan/hse/HseHazardKnowledge.java').read_text()
main=(ROOT/'app/src/main/java/com/ekbatan/hse/MainActivity.java').read_text()
aiui=(ROOT/'app/src/main/java/com/ekbatan/hse/VisionAiFeature.java').read_text()
for term in ['HseHazardKnowledge.inspectionRubric()','HseHazardKnowledge.outputContract()','فقط بر اساس شواهد قابل مشاهده','قابل تشخیص نیست']:
    ok('HSE prompt '+term) if term in ai else fail('HSE prompt '+term,'missing')
for term in ['سقوط از ارتفاع','داربست','شیروانی','حفاظ ناقص','برق','تجهیزات حفاظت فردی','حریق و انفجار','Housekeeping','جابه‌جایی و تردد','حفاری','مواد و انبارش','محیط صنعتی سیمان']:
    ok('HSE rubric '+term) if term in know else fail('HSE rubric '+term,'missing')
for term in ['دسته HSE:','شاهد قابل مشاهده:','خطر و پیامد:','اقدام فوری:','اقدام اصلاحی پیشنهادی:','اولویت:','اطمینان:']:
    ok('HSE output '+term) if term in know else fail('HSE output '+term,'missing')
if 'لایه بازرسی HSE' in aiui: ok('HSE UI disclosure')
else: fail('HSE UI disclosure','missing')
if not errors:
    print('TOTAL PASS errors=0')
else:
    print('TOTAL FAIL errors=',len(errors)); sys.exit(1)
