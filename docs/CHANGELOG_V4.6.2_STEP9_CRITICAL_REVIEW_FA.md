# HSE AI PRO 4.6.2 — Step 9 Critical Review

## هدف
بازنگری نسخه FINAL_HANDOFF و اصلاح خطای حیاتی در چرخه بازیابی Backup.

## اصلاح حیاتی
در نسخه قبل، در مسیر Restore ابتدا `services` به `null` تنظیم می‌شد و سپس متد `restoreBackup` روی همان reference فراخوانی می‌شد. این مسیر می‌توانست در بازیابی واقعی به `NullPointerException` منجر شود.

در Step 9 ترتیب عملیات اصلاح شد:
1. بستن Database فعلی
2. گرفتن reference از Service فعلی
3. بستن موتور AI از طریق Service
4. اجرای Restore قبل از null کردن Service
5. ایجاد دوباره Database
6. ایجاد دوباره Repository
7. بررسی سلامت Database
8. ایجاد دوباره Application Services
9. زمان‌بندی مجدد Reminderها

## QA جدید
`tools/qa_restore_lifecycle.py` برای جلوگیری از Regression این مسیر اضافه شد و در GitHub Actions نیز اجرا می‌شود.

## اصل مهم
این مرحله عمداً مهاجرت بزرگ جدیدی به Room/Hilt/Compose انجام نمی‌دهد؛ هدف حذف یک خطای lifecycle واقعی و کاهش ریسک نسخه تحویلی است.
