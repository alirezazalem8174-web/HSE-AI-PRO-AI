# Build Android — HSE AI PRO 4.6.2

برای Build محلی، Android SDK 35، Build Tools 35.0.0، JDK 17 و Gradle 8.7 لازم است.

فایل `gradlew` در این نسخه به‌صورت launcher سازگار با Gradle نصب‌شده قرار داده شده است تا Workflowهای قدیمی که `chmod +x gradlew` اجرا می‌کنند نیز با خطای «No such file or directory» متوقف نشوند. GitHub Actions همچنان Gradle 8.7 را نصب و استفاده می‌کند.
