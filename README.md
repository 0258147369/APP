# AliLink

אפליקציית Android להמרת קישורי AliExpress לקישורי Affiliate/Deep Link.

## מה האפליקציה עושה

- מקבלת קישורי AliExpress דרך שיתוף, הדבקה או פתיחה של קישור.
- עוטפת את כתובת היעד ב־`s.click.aliexpress.com/deep_link.htm` עם `aff_short_key`.
- מאפשרת העתקה, שיתוף ופתיחה של הקישור שנוצר.
- שומרת היסטוריה מקומית של ההמרות האחרונות.
- אינה אורזת מחדש קישור שכבר נראה כמו קישור affiliate.

## הגדרה

בהפעלה הראשונה מזינים את ה־`aff_short_key` מחשבון AliExpress Portals. הקוד נשמר מקומית במכשיר.

## Build

הבנייה מבוצעת ב־GitHub Actions ומפיקה APK להתקנה.
