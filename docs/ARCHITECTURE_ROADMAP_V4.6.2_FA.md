# نقشه راه فنی HSE AI PRO — نسخه 4.6.2

## اصل حاکم
پایداری نسخه موجود بر اضافه‌کردن قابلیت‌های پرریسک مقدم است. تغییرات معماری بزرگ مانند Room/Hilt/Modularization/Firebase فقط پس از داشتن تست رگرسیون و مسیر بازگشت انجام می‌شوند.

## انجام‌شده در 4.6.2
- سخت‌سازی Migration دیتابیس 11 و بازگردانی `delivery_state` رکوردهای قدیمی به `pending`.
- جلوگیری از باقی‌ماندن `delivery_state` ناشناخته در رکوردهای قدیمی.
- Index تکمیلی برای وضعیت و زمان Reminderها.
- جلوگیری از Crash ناشی از SecurityException/IllegalArgumentException در زمان زمان‌بندی Alarm.
- خطای مجوز/کانال اعلان Reminder را به وضعیت retryable (`pending`) تبدیل کردیم تا یادآوری بی‌دلیل برای همیشه از چرخه خارج نشود.
- CI/CD: اجرای QA، lint، build APK و build AAB در GitHub Actions.

## عمداً فعلاً اضافه نشده
### Firebase Crashlytics / Analytics
به دلیل وابستگی شبکه/سرویس خارجی و ملاحظات حریم خصوصی، هسته برنامه نباید به Firebase وابسته باشد. این قابلیت در فاز اختیاری Cloud/Telemetry اضافه می‌شود.

### Room / Hilt / Clean Architecture / Modularization
بازنویسی هم‌زمان این بخش‌ها روی نسخه پایدار فعلی ریسک Regression بالایی دارد. ابتدا Repository/Coroutine و تست‌های واقعی اضافه می‌شوند، سپس مهاجرت مرحله‌ای انجام خواهد شد.

### Location / Weather / Google Calendar / Cloud Sync
این موارد به مجوزها، سرویس‌های خارجی و سیاست حریم خصوصی نیاز دارند و تا آماده‌شدن لایه abstraction وارد هسته نمی‌شوند.

### R8/ProGuard
Release AAB در CI ساخته می‌شود، اما Minification تا زمانی که قواعد سازگاری کتابخانه PDFBox و اجزای بازتابی تأیید نشوند فعال نمی‌شود.

## انجام‌شده در NEXT2
- سخت‌سازی Backup/Restore برای پذیرش Backupهای قدیمی‌تر و مهاجرت آن‌ها توسط `LibraryDb`.
- رد صریح Backupهای با schema آینده.
- افزودن QA اختصاصی Backup/Restore و اتصال آن به CI.

## معیار پذیرش هر تغییر آینده
1. نسخه قبلی داده‌ها باید بدون Crash باز شود.
2. Migration باید idempotent و قابل تکرار باشد.
3. Reminderهای قدیمی نباید گم شوند.
4. Backup/Restore باید بعد از تغییر همچنان معتبر باشد.
5. QA و lint و build باید در CI سبز باشند.
