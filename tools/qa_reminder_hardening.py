from pathlib import Path
import sys
ROOT=Path(__file__).resolve().parents[1]
errors=[]
def check(name, cond):
    print(('PASS ' if cond else 'FAIL ')+name)
    if not cond: errors.append(name)
rs=(ROOT/'app/src/main/java/com/ekbatan/hse/ReminderScheduler.java').read_text()
db=(ROOT/'app/src/main/java/com/ekbatan/hse/LibraryDb.java').read_text()
rr=(ROOT/'app/src/main/java/com/ekbatan/hse/ReminderReceiver.java').read_text()
boot=(ROOT/'app/src/main/java/com/ekbatan/hse/BootReceiver.java').read_text()
check('scheduler refuses invalid reminder ids','if(id<=0||when<=0)return false;' in rs)
check('scheduler checks exact-alarm capability','canScheduleExactAlarms()' in rs)
check('scheduler uses idle-safe exact alarms','setExactAndAllowWhileIdle' in rs)
check('scheduler catches schedule security errors','catch (SecurityException | IllegalArgumentException e)' in rs)
check('cancel is best-effort','Cancellation is best-effort' in rs)
check('cancel catches OS rejection','catch(SecurityException | IllegalArgumentException ignored)' in rs)
check('delivery states are normalized','"pending".equals(state)||"scheduled".equals(state)||"delivered".equals(state)||"failed".equals(state)' in db)
check('receiver checks notification permission','POST_NOTIFICATIONS' in rr)
check('receiver checks notification setting','areNotificationsEnabled' in rr)
check('receiver leaves permission failures retryable','markReminderDelivery(id,"pending",failure)' in rr)
check('boot receiver uses goAsync','goAsync()' in boot)
check('boot receiver reschedules pending reminders','pendingReminders(System.currentTimeMillis())' in boot)
print('REMINDER HARDENING', 'PASS' if not errors else 'FAIL', 'errors=',len(errors))
if errors: sys.exit(1)
