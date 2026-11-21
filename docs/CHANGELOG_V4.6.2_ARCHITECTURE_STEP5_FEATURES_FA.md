# HSE AI PRO 4.6.2 — Architecture Step 5

## هدف
ادامه جداسازی معماری بدون مهاجرت پرریسک به Compose/Hilt/Room و بدون تغییر قرارداد دیتابیس.

## تغییرات اصلی
- استخراج `RiskFeature` از `MainActivity`.
- استخراج `IncidentFeature` از `MainActivity`.
- استخراج `VisionAiFeature` از `MainActivity`.
- ایجاد `HostFeatureAdapter` برای قابلیت‌هایی که هنوز به‌صورت تدریجی در حال استخراج هستند.
- حفظ `HseFeature` و `HseFeatureCoordinator` به‌عنوان قرارداد مرکزی Feature.
- ثبت صریح `glossary` و `backup` در Navigator تا هیچ Feature به‌صورت تصادفی روی `Space` یا آخرین child هدایت نشود.
- اضافه‌شدن callback آماده‌شدن تصویر برای AI تا دکمه تحلیل فقط پس از آماده‌شدن فایل تصویر فعال شود.
- پاک‌سازی callback تصویر در `onDestroy` برای جلوگیری از نگه‌داشتن Reference به Viewهای Activity.
- منطق ریسک، حادثه و AI دیگر در `MainActivity` به‌صورت Builderهای بزرگ قرار ندارد.

## اصل پایداری
این مرحله عمداً از بازنویسی گسترده UI، Compose، Hilt و Room خودداری می‌کند تا ریسک Regression پایین بماند. دیتابیس، Backup، Reminder و مدل AI موجود دست‌نخورده باقی مانده‌اند.

## QA
QAهای منبع، معماری، AI محلی، دانش HSE، Migration، Persistence، Backup/Restore، Concurrency، Reminder و Adversarial پس از استخراج Featureها باید همگی PASS باشند.

## محدودیت Build
در محیط فعلی Android SDK/Gradle واقعی برای اجرای `assembleDebug` موجود نیست؛ بنابراین PASSهای این مرحله شامل Source/Static/Contract QA هستند و به معنی تولید APK واقعی در همین محیط نیستند. Build واقعی از طریق GitHub Actions با JDK 17، Gradle 8.7 و SDK 35 انجام می‌شود.
