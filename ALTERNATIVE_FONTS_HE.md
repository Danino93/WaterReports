# 🔤 פונטים חלופיים לעברית ב-PDF

## 🚨 הבעיה

Rubik מציג עברית אבל **האותיות הפוכות!**

זה קורה כי לא כל הפונטים עובדים טוב עם RTL ב-iText7.

---

## ✅ פתרון: פונטים שעובדים טוב יותר

### אופציה 1: **Noto Sans Hebrew** (מומלץ ביותר! ✨)

**למה?**
- ✅ עוצב במיוחד לעברית
- ✅ עובד מצוין עם RTL
- ✅ תמיכה מלאה ב-iText7
- ✅ חינמי (OFL)

**איפה להוריד:**
- 🔗 https://fonts.google.com/noto/specimen/Noto+Sans+Hebrew
- או: https://github.com/notofonts/hebrew/tree/main/fonts/ttf

**איזה קובץ:**
- `NotoSansHebrew-Regular.ttf`
- או: `NotoSansHebrew[wght].ttf` (variable font)

---

### אופציה 2: **Assistant**

**למה?**
- ✅ פונט עברי פופולרי
- ✅ עובד טוב עם PDF
- ✅ נקי וקריא

**איפה להוריד:**
- 🔗 https://fonts.google.com/specimen/Assistant

**איזה קובץ:**
- `Assistant-Regular.ttf`

---

### אופציה 3: **Heebo**

**למה?**
- ✅ פונט מודרני
- ✅ תומך RTL מעולה
- ✅ יפה ומקצועי

**איפה להוריד:**
- 🔗 https://fonts.google.com/specimen/Heebo

**איזה קובץ:**
- `Heebo-Regular.ttf`

---

## 🔧 איך לשנות את הפונט בקוד

### אם תוריד `NotoSansHebrew-Regular.ttf`:

1. **שים את הקובץ ב**:
```
app/src/main/assets/fonts/NotoSansHebrew-Regular.ttf
```

2. **שנה את הקוד** ב-`PdfGenerator.kt`:

מצא את השורה:
```kotlin
val fontStream = context.assets.open("fonts/Rubik-Regular.ttf")
```

שנה ל:
```kotlin
val fontStream = context.assets.open("fonts/NotoSansHebrew-Regular.ttf")
```

3. **Build > Rebuild Project**

4. **נסה שוב!**

---

## 🎯 המלצה שלי

**תוריד את `NotoSansHebrew-Regular.ttf`** - זה הפונט הכי טוב לעברית ב-PDF!

הוא עוצב במיוחד לעברית ועובד מצוין עם iText7.

---

## 📥 קישור ישיר להורדה

🔗 **Noto Sans Hebrew**:
https://github.com/notofonts/hebrew/raw/main/fonts/ttf/NotoSansHebrew-Regular.ttf

לחץ ימין > "Save link as..." > שמור ב-`assets/fonts/`

---

## 🚀 זה יפתור את הבעיה!

Noto Sans Hebrew תומך **מושלם** ב-RTL ו-BIDI!

