# Build روی GitHub — HSE AI PRO 4.6.2

ساختار این بسته مستقیماً در Root پروژه قرار دارد و پوشه‌ی اضافی `rootaudit/` حذف شده است.

Workflow پروژه JDK 17، Android SDK 35 و Gradle 8.7 را آماده می‌کند، QA سورس را اجرا می‌کند، سپس Lint و `assembleDebug` را با `./gradlew` اجرا می‌کند.

بعد از Build، Artifact زیر در بخش Actions قابل دریافت است:
`HSE_AI_PRO-v4.6.2-debug`
