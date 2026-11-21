# تغییرات HSE AI PRO 4.6.2 — Hardening NEXT2

- مسیر Backup/Restore بازبینی و سخت‌سازی شد.
- Backupهای دیتابیس نسخه‌های قدیمی‌تر از نسخه فعلی، در صورت معتبر بودن، دیگر بی‌دلیل رد نمی‌شوند و برای Migration عادی به `LibraryDb` سپرده می‌شوند.
- Backup با نسخه دیتابیس آینده همچنان رد می‌شود تا از ناسازگاری جلوگیری شود.
- برای دیتابیس نسخه فعلی، کنترل ستون‌های جدید `delivery_state` و `last_delivery_error` همچنان اجباری است.
- پس از Restore، `integrity_check` و `foreign_key_check` و سلامت `LibraryDb` بررسی می‌شود.
- در صورت شکست Restore، مکانیزم Rollback حفظ شده است.
- تست اختصاصی `qa_backup_restore.py` اضافه و در GitHub Actions فعال شد.
- QAهای Preflight، Adversarial، Migration، Reminder Hardening، Persistence، Backup/Restore و Build Environment با موفقیت PASS شدند.

## نکته
Build واقعی Android در این محیط اجرا نشده است؛ Workflow گیت‌هاب همچنان مسئول Build و Lint نهایی است.
