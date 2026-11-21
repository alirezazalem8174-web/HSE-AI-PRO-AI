# HSE AI PRO 4.6.2 — Backup/Restore Hardening

این مرحله با تمرکز ویژه بر خطاهای بازیابی، خرابی فایل پشتیبان، قطع عملیات و جلوگیری از از دست رفتن داده‌ها انجام شد.

## اصلاحات

- اعتبارسنجی `integrity_check` و `foreign_key_check` قبل/بعد از بازیابی.
- پذیرش Backupهای قدیمی‌تر و واگذاری Migration عادی به `LibraryDb`.
- رد کردن Backup با schema آینده.
- محدودیت تعداد Entry، حجم هر Entry و حجم کل Archive.
- جلوگیری از Path Traversal و مسیرهای Canonical تکراری در ZIP.
- استفاده از فایل staged برای دیتابیس قبل از Commit.
- حفظ نسخه قبلی دیتابیس و کتابخانه تا پایان Verify.
- Rollback در شکست Commit/Verify؛ در صورت شکست Rollback، نسخه موقت حذف نمی‌شود.
- تأیید کپی فایل Backup با اندازه و `fsync` در مسیر fallback.
- جلوگیری از ساخت ناخواسته یک دیتابیس خالی پس از شکست Restore در UI.
- اصلاح نمایش نسخه برنامه از 4.6.1 به 4.6.2.
- توسعه QA مخصوص Backup/Restore Hardening.

## وضعیت تست

- Backup/Restore Hardening: PASS
- Preflight: PASS
- Adversarial: PASS
- Migration: PASS
- Reminder Hardening: PASS
- Persistence: PASS

> Build واقعی Android باید در محیط CI با Android SDK و Gradle تعریف‌شده در Workflow اجرا شود.
