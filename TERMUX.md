# البناء من التلفون عبر Termux (بدون كمبيوتر)

الفكرة: **التلفون هيجهّز الملفات بس، والبناء هيحصل على سيرفرات GitHub المجانية**، وبعدين تنزّل الـ APK وتثبّته.
يعني مش محتاج Android Studio ولا SDK ولا مساحة كبيرة على التلفون.

## 1) ثبّت الأدوات في Termux
```bash
pkg update -y && pkg upgrade -y
pkg install -y git gh unzip
```

## 2) سجّل دخول GitHub
```bash
gh auth login
```
اختر بالترتيب:
`GitHub.com` → `HTTPS` → `Login with a web browser` → انسخ الكود اللي هيظهر وافتح المتصفح وسجّل دخولك.
(لو معندكش حساب، اعمل حساب مجاني من متصفح التلفون أولًا — غالبًا هتحتاجه عشان تنزّل APK.)

ثم:
```bash
gh auth setup-git
```

## 3) جهّز المشروع كـ Git repo
```bash
cd /root/1
git init -b main
git config user.name "اسمك"
git config user.email "بريدك@example.com"
git add .
git commit -m "first build"
```
> لو `cd /root/1` ما اشتغلش، استخدم `pwd` و `ls` عشان تعرف مكان المشروع عندك في Termux.

## 4) اعمل مستودع وارفع الكود
```bash
gh repo create mini-erp --private --source=. --remote=origin --push
```
أي اسم تاني عادي لو الاسم ده مستخدم.

## 5) استنى البناء وتابعّه
```bash
gh run watch
```
أو افتح الرابط: `https://github.com/اسمك/mini-erp/actions`

## 6) نزّل الـ APK وثبّته
```bash
gh run download --name app-debug
unzip -o app-debug.zip
find . -name "*.apk"
```
هتلاقي ملف اسمه `app-debug.apk` (داخل مجلد باسم الأرتيبفكت أو مباشرة). افتحه:
```bash
termux-open app-debug.apk
```
أو استخدم المسار اللي ظهر في `find`.
لو طُلب السماح: فعّل **تثبيت تطبيقات من مصادر غير معروفة** للتطبيق اللي بيفتح الملف (Termux أو مدير الملفات).

## لو حصل خطأ في البناء
```bash
gh run view --log-failed
```
انسخ آخر 20 سطر وابعتهالي، وأصلّحها لك.

## ملاحظات
- الـ APK الناتج **موقّع بمفتاح Debug** ويتثبّت عادي على التلفون (مناسب للاستخدام الشخصي).
- ملفات البناء على GitHub بتتعمل في دقائق، ومفيش أي حاجة بتتنزّل على التلفون غير الـ APK.
- كل تعديل أعمله في الكود، هتعمل `git add . && git commit -m "update" && git push` && هيتبنى APK جديد تاني.
