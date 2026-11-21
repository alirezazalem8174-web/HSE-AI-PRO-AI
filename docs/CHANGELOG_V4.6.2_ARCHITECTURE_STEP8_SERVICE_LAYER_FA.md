# HSE AI PRO 4.6.2 — Architecture Step 8

## Service Layer Hardening

در این مرحله عملیات طولانی و بین‌قابلیتی از MainActivity خارج شد و در `HseAppServices` متمرکز شد.

### موارد منتقل‌شده
- ورود PDF
- ایجاد و ذخیره Backup
- بازیابی Backup
- زمان‌بندی و لغو یادآوری
- باززمان‌بندی یادآوری‌های pending
- اجرای Gemma/LiteRT-LM برای تحلیل تصویر

### اصول حفظ‌شده
- MainActivity همچنان مالک lifecycle دیتابیس و Executor است.
- Repository مرز دسترسی Featureها به داده باقی مانده است.
- Restore پس از جایگزینی DB، Repository و Service را دوباره ایجاد می‌کند.
- موتور AI توسط Service مالکیت و lifecycle می‌شود.
- مهاجرت به Hilt/Room/Compose در این مرحله انجام نشد تا ریسک regression کنترل شود.

### QA
Service Layer QA، Data Layer QA، Feature Extraction QA، Local AI QA و تمام QAهای قبلی باید قبل از تحویل PASS باشند.

### Build note
این محیط Android SDK/Gradle واقعی برای اجرای `assembleDebug` ندارد؛ بنابراین PASSهای این مرحله به معنی موفقیت Build واقعی APK نیستند. Build واقعی از مسیر GitHub Actions با JDK 17، Gradle 8.7 و Android SDK 35 انجام می‌شود.
