# مسیر نهایی Build — HSE AI PRO 4.6.2

1. محتویات این بسته را مستقیماً در Root یک GitHub Repository قرار دهید؛ پوشه‌ی `rootaudit/` نباید ایجاد شود.
2. Actions را باز کنید.
3. Workflow با نام `Build HSE AI PRO APK` را اجرا کنید.
4. Workflow ابتدا JDK/SDK/Gradle را آماده می‌کند، QA را اجرا می‌کند و سپس با `./gradlew` پروژه را Build می‌کند.
5. پس از موفقیت، Artifact با نام `HSE_AI_PRO-v4.6.2-debug` را دریافت کنید.
