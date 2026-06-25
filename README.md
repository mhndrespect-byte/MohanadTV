# Mohanad TV — مشغل IPTV خفيف جداً

تطبيق Android أصلي (Kotlin) لمشغل IPTV، مبني بفلسفة "خفيف كالريشة":
بدون إعلانات، بدون مكتبات تتبع، بدون أي تشفير أو حماية ضد التعديل.

---

## 📂 هيكل المشروع

```
MohanadTV/
├── app/
│   ├── build.gradle                  ← إعدادات البناء (Single APK، minify بدون obfuscate)
│   ├── proguard-rules.pro            ← قواعد تصغير الحجم فقط (dontobfuscate مفعّلة)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mohanad/tv/
│       │   ├── model/
│       │   │   ├── Channel.kt        ← نموذج بيانات القناة
│       │   │   └── PlaylistParser.kt ← محلل M3U يدوي (بدون مكتبات)
│       │   ├── player/
│       │   │   └── PlayerFactory.kt  ← ⭐ تهيئة ExoPlayer للشبكات الضعيفة
│       │   ├── ui/
│       │   │   ├── MainActivity.kt   ← الشاشة الرئيسية
│       │   │   ├── PlayerActivity.kt ← شاشة المشغل
│       │   │   ├── ChannelAdapter.kt
│       │   │   └── CategoryAdapter.kt
│       │   └── util/
│       │       ├── NetworkUtil.kt    ← فحص حالة الشبكة
│       │       └── PrefsManager.kt   ← حفظ آخر رابط (SharedPreferences فقط)
│       └── res/                      ← XML layouts + ألوان Terminal + drawables بسيطة
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## 🚀 طريقة البناء

### الطريقة الموصى بها (Android Studio)
1. افتح Android Studio.
2. `Open` ثم اختر مجلد `MohanadTV`.
3. انتظر مزامنة Gradle (سيُولّد Android Studio ملفات الـ wrapper تلقائياً إن لم تكن مكتملة).
4. `Build > Generate Signed Bundle / APK > APK` لإنتاج ملف APK واحد صلب.

### عبر سطر الأوامر (إن توفر Gradle لديك)
```bash
cd MohanadTV
gradle assembleRelease
```
الناتج: `app/build/outputs/apk/release/app-release.apk`

> **ملاحظة:** المشروع مهيأ بالكامل لإنتاج **APK واحد فقط** بدون أي تقسيم
> (`splits { abi { enable false } density { enable false } }`)، تماماً كما طُلب.

---

## ⭐ كيف يعمل التعامل مع الإنترنت الضعيف؟ (`PlayerFactory.kt`)

هذا هو الملف الأهم في المشروع. آلية العمل:

| الإعداد | القيمة | الهدف |
|---|---|---|
| `minBufferMs` | 30 ثانية | لا يتوقف التشغيل إلا بعد نفاذ 30 ثانية من المخزون |
| `maxBufferMs` | 60 ثانية | يخزّن دقيقة كاملة مسبقاً عند توفر الشبكة |
| `bufferForPlaybackMs` | 4 ثوان | ينتظر فقط 4 ثوان قبل أول تشغيل (تجربة سريعة) |
| `bufferForPlaybackAfterRebufferMs` | 6 ثوان | بعد أي تقطيع، ينتظر 6 ثوان قبل إعادة المحاولة (لتفادي التقطيع المتكرر) |
| `bandwidthFraction` | 0.7 | يحجز هامش أمان 30% من سرعة الإنترنت الحقيقية لتفادي القفزات المفاجئة في الجودة |

**وضع توفير البيانات (LDM):** زر "LDM" في شاشة المشغل (أعلى يمين الشاشة) يفرض
أدنى جودة متاحة دائماً (`setForceLowestBitrate(true)`) — مفيد لشبكات الجوال المحدودة.

---

## 🎨 التصميم

ألوان نمط Terminal/Kali Linux معرّفة في `res/values/colors.xml`:
- الخلفية: `black_royal` (#0A0E0A)
- اللون المميز: `green_terminal` (#00FF41)

كل الأشكال (الأزرار، حقول الإدخال، البطاقات) هي ملفات `drawable` بصيغة XML
(`<shape>`) — **لا توجد أي صور PNG/JPG** في المشروع، فقط Vector وShape، مما
يقلل الحجم النهائي للـ APK إلى أدنى حد ممكن.

---

## 🔧 التعديل لاحقاً بـ MT Manager

تم تعطيل أي حماية أو تشفير عمداً:
- `proguard-rules.pro` يحتوي على `-dontobfuscate` (لا تُغيَّر أسماء الكلاسات).
- لا يوجد أي فحص توقيع (Signature Verification) أو فحص بيئة Root/Debug.
- أسماء الحزم والكلاسات والمتغيرات تبقى كما هي بعد البناء.

أهم الملفات للتعديل السريع:
- **الألوان:** `res/values/colors.xml`
- **النصوص:** `res/values/strings.xml`
- **سلوك الـ Buffer:** الثوابت في أعلى `PlayerFactory.kt`
- **اسم التطبيق:** `res/values/strings.xml` → `app_name`، و`applicationId` في `app/build.gradle`

---

## ⚠️ ملاحظة لإصدار الإنتاج النهائي

ملف `app/build.gradle` يستخدم حالياً `signingConfig signingConfigs.debug` لتسهيل
البناء المباشر دون إعداد مفتاح توقيع. إن كنت تنوي نشر التطبيق على Google Play
أو توزيعه رسمياً، يُنصح بإنشاء `keystore` خاص بك واستخدامه بدلاً من توقيع الـ debug.
لتعديلك الشخصي عبر MT Manager فهذا غير مطلوب أصلاً.
