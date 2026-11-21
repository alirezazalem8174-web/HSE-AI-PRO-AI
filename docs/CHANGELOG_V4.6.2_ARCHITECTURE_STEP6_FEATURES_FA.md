# HSE AI PRO 4.6.2 — Architecture Step 6

## هدف
تکمیل استخراج قابلیت‌های اصلی از `MainActivity` و تبدیل Activity به یک Shell/Composition Host سبک‌تر، بدون مهاجرت پرریسک همزمان به Compose/Hilt/Room.

## قابلیت‌های استخراج‌شده
- `LibraryFeature`
- `GlossaryFeature`
- `NotesFeature`
- `ReminderFeature`
- `RiskFeature`
- `IncidentFeature`
- `BackupFeature`
- `VisionAiFeature`

## تغییر معماری
- `MainActivity` دیگر builderهای اصلی Library/Glossary/Notes/Reminder/Backup را نگه نمی‌دارد.
- ساخت و ثبت قابلیت‌ها از طریق `HseFeatureCoordinator` انجام می‌شود.
- ثبت صریح Navigator برای جلوگیری از هدف‌گیری اشتباه `Space` یا Viewهای جانبی حفظ شده است.
- وضعیت مشترک و lifecycle حساس، مخصوصاً Reminder و AI image lifecycle، همچنان در Activity به‌صورت کنترل‌شده نگهداری می‌شود تا در این مرحله رفتار موجود تغییر نکند.
- `HostFeatureAdapter` فعلاً به‌عنوان لایه سازگاری نسخه‌های قبلی نگه داشته شده و در ثبت قابلیت‌های فعلی استفاده نمی‌شود.

## پایداری
- QA اختصاصی استخراج Feature اضافه شد.
- QAهای syntax، architecture، persistence، migration، backup/restore، concurrency، reminder، AI، HSE knowledge و adversarial مجدداً اجرا شدند.
- تست واقعی Android Gradle Build در این محیط انجام نشده است؛ SDK/Gradle محلی برای build واقعی در دسترس نیست. Workflow GitHub Actions برای build واقعی تنظیم شده است.
