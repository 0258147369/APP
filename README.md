# AliLink

אפליקציית Android שמקבלת **כל קישור מוצר ספציפי של AliExpress** ומבקשת מהשרת שלך ליצור עבור אותו URL קישור Affiliate מתאים.

## איך זה עובד

1. משתפים קישור מוצר AliExpress ל־AliLink, או מדביקים אותו.
2. האפליקציה שולחת את ה־URL הספציפי ל־/api/convert.
3. השרת קורא ל־AliExpress Affiliate Link Generator עם ה־Tracking ID שלך.
4. מתקבל קישור Affiliate שנוצר עבור אותו מוצר.
5. אפשר להעתיק, לשתף או לפתוח את הקישור.

הגישה הזו עדיפה על שמירת App Secret בתוך ה־APK: המפתחות נשארים בשרת.

## Backend

הקובץ api/convert.js מיועד לפריסה ב־Vercel או בסביבת Node דומה.

יש להגדיר בשרת את משתני הסביבה:

- ALIEXPRESS_APP_KEY
- ALIEXPRESS_APP_SECRET
- ALIEXPRESS_TRACKING_ID

**לא להכניס את הערכים האלה לקוד ולא לשלוח אותם בצ'אט.**

לאחר הפריסה, הכתובת שתוזן באפליקציה היא לדוגמה:

https://YOUR-DOMAIN.vercel.app/api/convert

## הערה על AliExpress API

התיעוד של AliExpress מציג את aliexpress.affiliate.link.generate עם source_values ו־tracking_id, ומחזיר promotion_link. בדפי התיעוד הנוכחיים מופיעה גם אינדיקציה שה־Affiliate API הוא ממשק ותיק/מוצא משימוש, ולכן כדאי לבדוק שהגישה פעילה עבור חשבון ה־Portals שלך לפני פריסה מלאה.

## Android

האפליקציה:

- תומכת בשיתוף קישור ישירות אליה.
- תומכת בפתיחת קישור AliExpress.
- שומרת היסטוריה מקומית.
- מאפשרת העתקה/שיתוף/פתיחה של התוצאה.
- אינה שומרת את App Secret במכשיר.

GitHub Actions בונה APK Debug.
