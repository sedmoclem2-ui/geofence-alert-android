package ir.khanoomi.geofencealert.ui

import androidx.compose.runtime.compositionLocalOf

/** زبان‌های پشتیبانی‌شده برنامه: فقط فارسی، انگلیسی و عربی. */
enum class AppLang { FA, EN, AR }

/**
 * رشته‌های اصلی رابط کاربری در هر سه زبان.
 * هر کلید یک متن کامل است؛ هر جا رشته‌ای در این فهرست نیست، متن فارسی
 * پیش‌فرض در خود صفحه باقی می‌ماند.
 */
data class UiStrings(
    val appTitle: String,
    val monitoringOn: String,
    val monitoringOff: String,
    val activeZonesSuffix: String,
    val activeAlertsSuffix: String,
    val fixIssue: String,
    val addZone: String,
    val newZone: String,
    val editZone: String,
    val save: String,
    val saveZone: String,
    val cancel: String,
    val delete: String,
    val deleteZoneTitle: String,
    val settingsTitle: String,
    val theme: String,
    val customizeTheme: String,
    val language: String,
    val soundAndVibration: String,
    val volume: String,
    val vibrationIntensity: String,
    val backup: String,
    val backupExport: String,
    val backupImport: String,
    val backupDone: String,
    val backupFailed: String,
    val history: String,
    val statistics: String,
    val weeklyStats: String,
    val monthlyStats: String,
    val totalEntries: String,
    val totalExits: String,
    val entriesPerZone: String,
    val mapPickerTitle: String,
    val confirm: String,
    val back: String,
    val currentLocation: String,
    val latitude: String,
    val longitude: String,
    val alertSound: String,
    val schedule: String,
    val quietHours: String,
    val batteryGuide: String,
    val limitations: String,
    val version: String,
    val stopAlert: String,
    val alertActive: String,
    val lastEntry: String,
    val active: String,
    val inactive: String,
    val waitingEntry: String,
    val radius: String,
    val zoneName: String,
    val specifications: String,
    val coordinates: String,
    val selectOnMap: String,
    val exitBehaviorTitle: String,
    val status: String,
    val zoneActive: String,
    val approachAlert: String,
    val stayReminder: String,
    val alertType: String,
    val voiceAnnounce: String,
    val sms: String,
    val silentWindow: String,
    val enterZone: String,
    val exitZone: String,
    val approachEvent: String,
    val csvExport: String,
    val csvExportDone: String,
    val noZonesYet: String,
    val noZonesYetSub: String,
    val historyEmpty: String,
    val statsEmpty: String,
    // صحنه انتخاب زبان و صحنه‌های معرفی نخستین اجرا
    val langSceneTitle: String,
    val ob1Title: String,
    val ob1Desc: String,
    val ob2Title: String,
    val ob2Desc: String,
    val ob3Title: String,
    val ob3Desc: String,
    val getStarted: String,
    val next: String,
    val skip: String,
    val previous: String,
    // ردیف‌های تازه پنل تنظیمات: مخاطبین، باتری، روشن شدن صفحه و چراغ قوه
    val permContacts: String,
    val permBattery: String,
    val batteryUnrestricted: String,
    val batteryRestricted: String,
    val wakeScreen: String,
    val wakeScreenSub: String,
    val flashlight: String,
    val flashOff: String,
    val flashSteady: String,
    val flashBlink: String,
    // چیپ‌های وضعیت کارت پایش، پنل مرتب‌سازی و دکمه‌های کارت (نسخه ۲.۶.۰)
    val chipLocation: String,
    val chipBackground: String,
    val chipBattery: String,
    val chipNotif: String,
    val chipSms: String,
    val dragHint: String,
    val deleteZoneConfirm: String,
    val editCd: String,
    val sortTitle: String,
    val sortManual: String,
    val sortManualCaption: String,
    val sortName: String,
    val sortNameAsc: String,
    val sortNameDesc: String,
    val sortCreated: String,
    val sortNewest: String,
    val sortOldest: String,
    val sortLargest: String,
    val sortSmallest: String,
    val exitImmediate: String,
    val exitPattern: String,
    val exitUntilManual: String,
    // کانال‌ها و اعلان‌ها، ویجت‌ها، کاشی و سرویس‌ها (لایه‌های غیرکامپوزی، نسخه ۲.۷.۰)
    val chanAlerts: String,
    val chanAlertsDesc: String,
    val chanStatus: String,
    val chanStatusDesc: String,
    val chanProximity: String,
    val chanProximityDesc: String,
    val chanGps: String,
    val chanGpsDesc: String,
    val chanQuiet: String,
    val chanQuietDesc: String,
    val notifAlertTitle: String,
    val notifAlertBody: String,
    val notifApproachTitle: String,
    val notifStayTitle: String,
    val notifStayBody: String,
    val notifStatusTitle: String,
    val notifGpsOffTitle: String,
    val notifGpsOffBody: String,
    val notifEnableGps: String,
    val notifDisableMonitoring: String,
    val proxIdleZone: String,
    val proxPreciseTitle: String,
    val proxApproachingSmall: String,
    val widgetActiveZones: String,
    val widgetMonitoringOn: String,
    val widgetMonitoringOff: String,
    val widgetStopMonitoring: String,
    val widgetStartMonitoring: String,
    val widgetAlertPlaying: String,
    val widgetToggle: String,
    val widgetZoneDeleted: String,
    val widgetZoneDeletedSub: String,
    val widgetZoneNotSelected: String,
    val ext: UiStringsExt
) {
    companion object {
        val FA = UiStrings(
            appTitle = "محدوده هشدار",
            monitoringOn = "پایش فعال است",
            monitoringOff = "پایش متوقف است",
            activeZonesSuffix = "محدوده فعال",
            activeAlertsSuffix = "هشدار فعال",
            fixIssue = "رفع مشکل",
            addZone = "محدوده جدید",
            newZone = "محدوده جدید",
            editZone = "ویرایش محدوده",
            save = "ذخیره",
            saveZone = "ذخیره محدوده",
            cancel = "انصراف",
            delete = "حذف",
            deleteZoneTitle = "حذف محدوده",
            settingsTitle = "تنظیمات و مجوزها",
            theme = "تم رنگی",
            customizeTheme = "شخصی‌سازی",
            language = "زبان برنامه",
            soundAndVibration = "صدا و لرزش",
            volume = "بلندی صدای هشدار",
            vibrationIntensity = "شدت لرزش",
            backup = "پشتیبان‌گیری",
            backupExport = "خروجی پشتیبان",
            backupImport = "ورودی پشتیبان",
            backupDone = "پشتیبان‌گیری انجام شد.",
            backupFailed = "خطا در پشتیبان‌گیری رخ داد.",
            history = "تاریخچه رویدادها",
            statistics = "آمار",
            weeklyStats = "آمار هفته",
            monthlyStats = "آمار ماهانه",
            totalEntries = "مجموع ورودها",
            totalExits = "مجموع خروج‌ها",
            entriesPerZone = "ورودها به تفکیک محدوده",
            mapPickerTitle = "انتخاب روی نقشه",
            confirm = "تأیید",
            back = "بازگشت",
            currentLocation = "موقعیت فعلی",
            latitude = "عرض",
            longitude = "طول",
            alertSound = "صدای هشدار",
            schedule = "زمان‌بندی",
            quietHours = "ساعات سکوت",
            batteryGuide = "راهنمای مصرف باتری (سامسونگ)",
            limitations = "محدودیت‌های شناخته‌شده",
            version = "نسخه",
            stopAlert = "توقف هشدار",
            alertActive = "هشدار فعال",
            lastEntry = "آخرین ورود",
            active = "فعال",
            inactive = "غیرفعال",
            waitingEntry = "در انتظار ورود",
            radius = "شعاع",
            zoneName = "نام محدوده",
            specifications = "مشخصات",
            coordinates = "مختصات",
            selectOnMap = "انتخاب روی نقشه",
            exitBehaviorTitle = "پس از خروج از محدوده",
            status = "وضعیت",
            zoneActive = "محدوده فعال باشد",
            approachAlert = "هشدار نزدیک شدن",
            stayReminder = "یادآور ماندن",
            alertType = "نوع هشدار",
            voiceAnnounce = "اعلام صوتی نام محدوده",
            sms = "پیامک اطلاع‌رسانی",
            silentWindow = "اعلان بی‌صدا در بازه زمانی",
            enterZone = "ورود به محدوده",
            exitZone = "خروج از محدوده",
            approachEvent = "نزدیک‌شدن",
            csvExport = "خروجی CSV",
            csvExportDone = "خروجی CSV ساخته شد.",
            noZonesYet = "هنوز محدوده‌ای نساختی",
            noZonesYetSub = "اولین محدوده‌ات را بساز تا ورود به آن را به تو خبر بدهم",
            historyEmpty = "هنوز رویدادی ثبت نشده است.",
            statsEmpty = "در این بازه رویدادی ثبت نشده است.",
            langSceneTitle = "زبان برنامه را انتخاب کنید",
            ob1Title = "محدوده‌ات را تعریف کن",
            ob1Desc = "نقطه‌ای را که برایت مهم است روی نقشه انتخاب کن؛ خانه، محل کار، مدرسه یا هر جای دیگر. شعاعش را هم خودت تعیین می‌کنی.",
            ob2Title = "مجوزها را بده",
            ob2Desc = "برای این‌که ورودت را حتی وقتی برنامه باز نیست تشخیص بدهم، به دسترسی مکان «همیشه» و اعلان نیاز دارم؛ همه‌چیز فقط روی گوشی خودت می‌ماند.",
            ob3Title = "وارد که شدی، خبرت می‌کنم",
            ob3Desc = "با ورود به محدوده، هشدار صوتی و لرزش پخش می‌شود و نام محدوده هم خوانده می‌شود؛ توقفش هم دست خودت است.",
            getStarted = "شروع",
            next = "بعدی",
            skip = "رد کردن",
            previous = "قبلی",
            permContacts = "مخاطبین",
            permBattery = "مصرف باتری",
            batteryUnrestricted = "بدون محدودیت",
            batteryRestricted = "محدود شده",
            wakeScreen = "روشن شدن صفحه هنگام هشدار",
            wakeScreenSub = "هنگام شروع هشدار، صفحه قفل برای چند ثانیه روشن می‌شود.",
            flashlight = "چراغ قوه هنگام هشدار",
            flashOff = "خاموش",
            flashSteady = "ثابت",
            flashBlink = "چشمک‌زن",
            chipLocation = "مکان",
            chipBackground = "پس‌زمینه",
            chipBattery = "باتری",
            chipNotif = "اعلان",
            chipSms = "پیامک",
            dragHint = "برای جابه‌جایی، کارت را نگه دار و بکش",
            deleteZoneConfirm = "«%1\$s» حذف شود؟",
            editCd = "ویرایش",
            sortTitle = "مرتب‌سازی",
            sortManual = "دستی",
            sortManualCaption = "ترتیب دلخواه من",
            sortName = "نام",
            sortNameAsc = "ترتیب الفبا",
            sortNameDesc = "برعکس الفبا",
            sortCreated = "ایجاد",
            sortNewest = "تازه‌ترین",
            sortOldest = "قدیمی‌ترین",
            sortLargest = "بزرگ‌ترین",
            sortSmallest = "کوچک‌ترین",
            exitImmediate = "توقف فوری",
            exitPattern = "الگوی تکراری",
            exitUntilManual = "تا توقف دستی",
            chanAlerts = "هشدارها",
            chanAlertsDesc = "اعلان هشدار ورود به محدوده",
            chanStatus = "وضعیت پایش",
            chanStatusDesc = "وضعیت کلی پایش محدوده‌ها",
            chanProximity = "پایش دقیق محدوده",
            chanProximityDesc = "پایش دقیق محدوده‌های کوچک هنگام نزدیک شدن",
            chanGps = "هشدار مکان",
            chanGpsDesc = "هشدار خاموش بودن مکان‌یابی گوشی هنگام فعال بودن پایش",
            chanQuiet = "پایش پس‌زمینه",
            chanQuietDesc = "اعلان بی‌صدای سرویس پایش در حالت بیکار؛ بدون آیکن نوار وضعیت",
            notifAlertTitle = "هشدار ورود به محدوده",
            notifAlertBody = "%1\$s — برای توقف هشدار، دکمه زیر را بزنید",
            notifApproachTitle = "نزدیک شدن به محدوده",
            notifStayTitle = "یادآور ماندن",
            notifStayBody = "بیش از %1\$s دقیقه است که در محدودهٔ %2\$s هستید.",
            notifStatusTitle = "پایش محدوده‌ها",
            notifGpsOffTitle = "جی‌پی‌اس خاموش است",
            notifGpsOffBody = "پایش فعال است اما مکان‌یابی گوشی خاموش است؛ هشدارها کار نمی‌کنند.",
            notifEnableGps = "فعال کردن جی پی اس",
            notifDisableMonitoring = "غیر فعال کردن پایش",
            proxIdleZone = "در حال پایش محدوده‌ها",
            proxPreciseTitle = "پایش دقیق فعال است",
            proxApproachingSmall = "نزدیک شدن به محدوده‌های کوچک",
            widgetActiveZones = "%1\$s محدوده فعال",
            widgetMonitoringOn = "پایش: فعال",
            widgetMonitoringOff = "پایش: غیرفعال",
            widgetStopMonitoring = "توقف پایش",
            widgetStartMonitoring = "شروع پایش",
            widgetAlertPlaying = "هشدار در حال پخش است",
            widgetToggle = "فعال/غیرفعال",
            widgetZoneDeleted = "محدوده حذف شده",
            widgetZoneDeletedSub = "این محدوده دیگر وجود ندارد",
            widgetZoneNotSelected = "محدوده انتخاب نشده",
            ext = UiStringsExt.FA
        )

        val EN = UiStrings(
            appTitle = "Zone Alert",
            monitoringOn = "Monitoring is on",
            monitoringOff = "Monitoring is off",
            activeZonesSuffix = "active zones",
            activeAlertsSuffix = "active alerts",
            fixIssue = "Fix issue",
            addZone = "New zone",
            newZone = "New zone",
            editZone = "Edit zone",
            save = "Save",
            saveZone = "Save zone",
            cancel = "Cancel",
            delete = "Delete",
            deleteZoneTitle = "Delete zone",
            settingsTitle = "Settings & permissions",
            theme = "Color theme",
            customizeTheme = "Customize",
            language = "App language",
            soundAndVibration = "Sound & vibration",
            volume = "Alert volume",
            vibrationIntensity = "Vibration intensity",
            backup = "Backup",
            backupExport = "Export backup",
            backupImport = "Import backup",
            backupDone = "Backup completed.",
            backupFailed = "Backup failed.",
            history = "Event history",
            statistics = "Statistics",
            weeklyStats = "Weekly stats",
            monthlyStats = "Monthly stats",
            totalEntries = "Total entries",
            totalExits = "Total exits",
            entriesPerZone = "Entries by zone",
            mapPickerTitle = "Pick on map",
            confirm = "Confirm",
            back = "Back",
            currentLocation = "Current location",
            latitude = "Lat",
            longitude = "Lng",
            alertSound = "Alert sound",
            schedule = "Schedule",
            quietHours = "Quiet hours",
            batteryGuide = "Battery guide (Samsung)",
            limitations = "Known limitations",
            version = "Version",
            stopAlert = "Stop alert",
            alertActive = "Alert active",
            lastEntry = "Last entry",
            active = "Active",
            inactive = "Inactive",
            waitingEntry = "Waiting for entry",
            radius = "Radius",
            zoneName = "Zone name",
            specifications = "Details",
            coordinates = "Coordinates",
            selectOnMap = "Select on map",
            exitBehaviorTitle = "After leaving the zone",
            status = "Status",
            zoneActive = "Zone is active",
            approachAlert = "Approach alert",
            stayReminder = "Stay reminder",
            alertType = "Alert type",
            voiceAnnounce = "Voice announcement",
            sms = "SMS notification",
            silentWindow = "Silent notification window",
            enterZone = "Entered zone",
            exitZone = "Exited zone",
            approachEvent = "Approaching",
            csvExport = "CSV export",
            csvExportDone = "CSV export created.",
            noZonesYet = "No zones yet",
            noZonesYetSub = "Create your first zone and we will alert you when you enter it",
            historyEmpty = "No events recorded yet.",
            statsEmpty = "No events in this period.",
            langSceneTitle = "Choose your language",
            ob1Title = "Define your zone",
            ob1Desc = "Pick any place that matters to you on the map — home, work, school or anywhere else — and set its radius yourself.",
            ob2Title = "Grant the permissions",
            ob2Desc = "To detect your arrival even when the app is closed, I need «Always» location access and notifications. Everything stays only on your phone.",
            ob3Title = "Get alerted on arrival",
            ob3Desc = "When you enter a zone, a sound and vibration alert plays and the zone name is announced — and stopping it is up to you.",
            getStarted = "Get started",
            next = "Next",
            skip = "Skip",
            previous = "Back",
            permContacts = "Contacts",
            permBattery = "Battery usage",
            batteryUnrestricted = "Unrestricted",
            batteryRestricted = "Restricted",
            wakeScreen = "Wake screen on alert",
            wakeScreenSub = "When an alert starts, the lock screen turns on for a few seconds.",
            flashlight = "Flashlight during alert",
            flashOff = "Off",
            flashSteady = "Steady",
            flashBlink = "Blinking",
            chipLocation = "Location",
            chipBackground = "Background",
            chipBattery = "Battery",
            chipNotif = "Alerts",
            chipSms = "SMS",
            dragHint = "To reorder, long-press a card and drag it",
            deleteZoneConfirm = "Delete “%1\$s”?",
            editCd = "Edit",
            sortTitle = "Sort",
            sortManual = "Manual",
            sortManualCaption = "My custom order",
            sortName = "Name",
            sortNameAsc = "Alphabetical",
            sortNameDesc = "Reverse alphabetical",
            sortCreated = "Created",
            sortNewest = "Newest first",
            sortOldest = "Oldest first",
            sortLargest = "Largest first",
            sortSmallest = "Smallest first",
            exitImmediate = "Stop immediately",
            exitPattern = "Repeating pattern",
            exitUntilManual = "Until stopped manually",
            chanAlerts = "Alerts",
            chanAlertsDesc = "Zone entry alert notifications",
            chanStatus = "Monitoring status",
            chanStatusDesc = "Overall zone monitoring status",
            chanProximity = "Precise zone monitoring",
            chanProximityDesc = "Precise monitoring of small zones when approaching",
            chanGps = "Location alert",
            chanGpsDesc = "Alert when device location is off while monitoring is active",
            chanQuiet = "Background monitoring",
            chanQuietDesc = "Silent monitoring service notification while idle; no status bar icon",
            notifAlertTitle = "Zone entry alert",
            notifAlertBody = "%1\$s — tap the button below to stop the alert",
            notifApproachTitle = "Approaching a zone",
            notifStayTitle = "Stay reminder",
            notifStayBody = "You have been in %2\$s for over %1\$s minutes.",
            notifStatusTitle = "Zone monitoring",
            notifGpsOffTitle = "GPS is off",
            notifGpsOffBody = "Monitoring is on but device location is off; alerts won't work.",
            notifEnableGps = "Turn on GPS",
            notifDisableMonitoring = "Turn off monitoring",
            proxIdleZone = "Monitoring zones",
            proxPreciseTitle = "Precise monitoring is on",
            proxApproachingSmall = "Approaching small zones",
            widgetActiveZones = "%1\$s active zones",
            widgetMonitoringOn = "Monitoring: on",
            widgetMonitoringOff = "Monitoring: off",
            widgetStopMonitoring = "Stop monitoring",
            widgetStartMonitoring = "Start monitoring",
            widgetAlertPlaying = "Alert is playing",
            widgetToggle = "On/Off",
            widgetZoneDeleted = "Zone deleted",
            widgetZoneDeletedSub = "This zone no longer exists",
            widgetZoneNotSelected = "No zone selected",
            ext = UiStringsExt.EN
        )

        val AR = UiStrings(
            appTitle = "تنبيه المناطق",
            monitoringOn = "المراقبة مفعّلة",
            monitoringOff = "المراقبة متوقفة",
            activeZonesSuffix = "مناطق نشطة",
            activeAlertsSuffix = "تنبيهات نشطة",
            fixIssue = "إصلاح المشكلة",
            addZone = "منطقة جديدة",
            newZone = "منطقة جديدة",
            editZone = "تحرير المنطقة",
            save = "حفظ",
            saveZone = "حفظ المنطقة",
            cancel = "إلغاء",
            delete = "حذف",
            deleteZoneTitle = "حذف المنطقة",
            settingsTitle = "الإعدادات والأذونات",
            theme = "المظهر",
            customizeTheme = "التخصيص",
            language = "لغة التطبيق",
            soundAndVibration = "الصوت والاهتزاز",
            volume = "مستوى صوت التنبيه",
            vibrationIntensity = "شدة الاهتزاز",
            backup = "النسخ الاحتياطي",
            backupExport = "تصدير النسخة الاحتياطية",
            backupImport = "استيراد النسخة الاحتياطية",
            backupDone = "اكتمل النسخ الاحتياطي.",
            backupFailed = "فشل النسخ الاحتياطي.",
            history = "سجل الأحداث",
            statistics = "الإحصائيات",
            weeklyStats = "إحصائيات الأسبوع",
            monthlyStats = "إحصائيات الشهر",
            totalEntries = "مجموع الدخول",
            totalExits = "مجموع الخروج",
            entriesPerZone = "الدخول حسب المنطقة",
            mapPickerTitle = "الاختيار على الخريطة",
            confirm = "تأكيد",
            back = "رجوع",
            currentLocation = "الموقع الحالي",
            latitude = "العرض",
            longitude = "الطول",
            alertSound = "صوت التنبيه",
            schedule = "الجدول الزمني",
            quietHours = "ساعات الهدوء",
            batteryGuide = "دليل البطارية (سامسونج)",
            limitations = "القيود المعروفة",
            version = "الإصدار",
            stopAlert = "إيقاف التنبيه",
            alertActive = "تنبيه نشط",
            lastEntry = "آخر دخول",
            active = "مفعّل",
            inactive = "غير مفعّل",
            waitingEntry = "بانتظار الدخول",
            radius = "نصف القطر",
            zoneName = "اسم المنطقة",
            specifications = "المواصفات",
            coordinates = "الإحداثيات",
            selectOnMap = "الاختيار على الخريطة",
            exitBehaviorTitle = "بعد مغادرة المنطقة",
            status = "الحالة",
            zoneActive = "المنطقة مفعّلة",
            approachAlert = "تنبيه الاقتراب",
            stayReminder = "تذكير البقاء",
            alertType = "نوع التنبيه",
            voiceAnnounce = "الإعلان الصوتي لاسم المنطقة",
            sms = "إشعار الرسائل القصيرة",
            silentWindow = "نافذة الإشعار الصامت",
            enterZone = "دخول المنطقة",
            exitZone = "الخروج من المنطقة",
            approachEvent = "الاقتراب",
            csvExport = "تصدير CSV",
            csvExportDone = "تم إنشاء ملف CSV.",
            noZonesYet = "لا توجد مناطق بعد",
            noZonesYetSub = "أنشئ منطقتك الأولى وسننبّهك عند دخولها",
            historyEmpty = "لم تُسجَّل أي أحداث بعد.",
            statsEmpty = "لا توجد أحداث في هذه الفترة.",
            langSceneTitle = "اختر لغة التطبيق",
            ob1Title = "عرّف منطقتك",
            ob1Desc = "اختر على الخريطة أي مكان يهمّك؛ البيت أو العمل أو المدرسة أو أي مكان آخر، وحدّد نصف قطر المنطقة بنفسك.",
            ob2Title = "امنح الأذونات",
            ob2Desc = "لأكتشف وصولك حتى عندما يكون التطبيق مغلقاً، أحتاج إلى إذن الموقع «دائماً» والتنبيهات؛ كل شيء يبقى على هاتفك فقط.",
            ob3Title = "تنبيه عند الوصول",
            ob3Desc = "عند دخولك المنطقة يعمل تنبيه صوتي مع اهتزاز ويُقرأ اسم المنطقة؛ وإيقافه بيدك.",
            getStarted = "ابدأ",
            next = "التالي",
            skip = "تخطي",
            previous = "السابق",
            permContacts = "جهات الاتصال",
            permBattery = "استهلاك البطارية",
            batteryUnrestricted = "بدون قيود",
            batteryRestricted = "مقيّد",
            wakeScreen = "إضاءة الشاشة عند التنبيه",
            wakeScreenSub = "عند بدء التنبيه، تُضاء شاشة القفل لبضع ثوانٍ.",
            flashlight = "المصباح أثناء التنبيه",
            flashOff = "إيقاف",
            flashSteady = "ثابت",
            flashBlink = "وامض",
            chipLocation = "الموقع",
            chipBackground = "الخلفية",
            chipBattery = "البطارية",
            chipNotif = "التنبيهات",
            chipSms = "الرسائل القصيرة",
            dragHint = "لإعادة الترتيب، اضغط مطوّلًا على البطاقة واسحبها",
            deleteZoneConfirm = "حذف «%1\$s»؟",
            editCd = "تحرير",
            sortTitle = "الفرز",
            sortManual = "يدوي",
            sortManualCaption = "ترتيبي المخصّص",
            sortName = "الاسم",
            sortNameAsc = "أبجدي",
            sortNameDesc = "عكس الأبجدي",
            sortCreated = "الإنشاء",
            sortNewest = "الأحدث",
            sortOldest = "الأقدم",
            sortLargest = "الأكبر",
            sortSmallest = "الأصغر",
            exitImmediate = "إيقاف فوري",
            exitPattern = "نمط متكرر",
            exitUntilManual = "حتى الإيقاف اليدوي",
            chanAlerts = "التنبيهات",
            chanAlertsDesc = "إشعارات تنبيه دخول المنطقة",
            chanStatus = "حالة المراقبة",
            chanStatusDesc = "الحالة العامة لمراقبة المناطق",
            chanProximity = "مراقبة دقيقة للمنطقة",
            chanProximityDesc = "مراقبة دقيقة للمناطق الصغيرة عند الاقتراب",
            chanGps = "تنبيه الموقع",
            chanGpsDesc = "تنبيه عند إيقاف موقع الجهاز أثناء تفعيل المراقبة",
            chanQuiet = "المراقبة في الخلفية",
            chanQuietDesc = "إشعار صامت لخدمة المراقبة في الخمول؛ بدون أيقونة في شريط الحالة",
            notifAlertTitle = "تنبيه دخول المنطقة",
            notifAlertBody = "%1\$s — اضغط الزر أدناه لإيقاف التنبيه",
            notifApproachTitle = "الاقتراب من منطقة",
            notifStayTitle = "تذكير البقاء",
            notifStayBody = "أنت في منطقة %2\$s منذ أكثر من %1\$s دقيقة.",
            notifStatusTitle = "مراقبة المناطق",
            notifGpsOffTitle = "GPS متوقف",
            notifGpsOffBody = "المراقبة مفعّلة لكن موقع الجهاز متوقف؛ لن تعمل التنبيهات.",
            notifEnableGps = "تشغيل GPS",
            notifDisableMonitoring = "إيقاف المراقبة",
            proxIdleZone = "مراقبة المناطق",
            proxPreciseTitle = "المراقبة الدقيقة مفعّلة",
            proxApproachingSmall = "الاقتراب من مناطق صغيرة",
            widgetActiveZones = "%1\$s مناطق نشطة",
            widgetMonitoringOn = "المراقبة: مفعّلة",
            widgetMonitoringOff = "المراقبة: متوقفة",
            widgetStopMonitoring = "إيقاف المراقبة",
            widgetStartMonitoring = "بدء المراقبة",
            widgetAlertPlaying = "التنبيه يعمل الآن",
            widgetToggle = "تشغيل/إيقاف",
            widgetZoneDeleted = "تم حذف المنطقة",
            widgetZoneDeletedSub = "هذه المنطقة لم تعد موجودة",
            widgetZoneNotSelected = "لم تُختر منطقة",
            ext = UiStringsExt.AR
        )
    }

    // کلیدهای بخش دوم جدول رشته‌ها (UiStringsExt) از اینجا ارجاع داده می‌شوند
    // تا محل‌های استفاده در سراسر برنامه بدون تغییر بمانند.
    val widgetZoneNotSelectedSub: String get() = ext.widgetZoneNotSelectedSub
    val widgetPickTitle: String get() = ext.widgetPickTitle
    val widgetNoZones: String get() = ext.widgetNoZones
    val widgetNoZonesSub: String get() = ext.widgetNoZonesSub
    val zoneFallbackName: String get() = ext.zoneFallbackName
    val settingsCd: String get() = ext.settingsCd
    val createZone: String get() = ext.createZone
    val batteryWarning: String get() = ext.batteryWarning
    val batteryWarningAction: String get() = ext.batteryWarningAction
    val backupRestored: String get() = ext.backupRestored
    val sectionPermissions: String get() = ext.sectionPermissions
    val sectionAlerts: String get() = ext.sectionAlerts
    val sectionAppearance: String get() = ext.sectionAppearance
    val sectionBackupData: String get() = ext.sectionBackupData
    val sectionHelpAbout: String get() = ext.sectionHelpAbout
    val permIntro: String get() = ext.permIntro
    val permPreciseLocation: String get() = ext.permPreciseLocation
    val permPreciseLocationDesc: String get() = ext.permPreciseLocationDesc
    val permBackgroundLocation: String get() = ext.permBackgroundLocation
    val permBackgroundLocationDesc: String get() = ext.permBackgroundLocationDesc
    val permNotificationsDesc: String get() = ext.permNotificationsDesc
    val permSmsTitle: String get() = ext.permSmsTitle
    val permSmsDesc: String get() = ext.permSmsDesc
    val smsPermGranted: String get() = ext.smsPermGranted
    val contactsPermGranted: String get() = ext.contactsPermGranted
    val permContactsDesc: String get() = ext.permContactsDesc
    val permBatteryDesc: String get() = ext.permBatteryDesc
    val permAdjust: String get() = ext.permAdjust
    val permRequest: String get() = ext.permRequest
    val permGranted: String get() = ext.permGranted
    val permNotGranted: String get() = ext.permNotGranted
    val openAppSettings: String get() = ext.openAppSettings
    val quietHoursSub: String get() = ext.quietHoursSub
    val fromLabel: String get() = ext.fromLabel
    val toLabel: String get() = ext.toLabel
    val later: String get() = ext.later
    val smsBlockedTitle: String get() = ext.smsBlockedTitle
    val smsBlockedBody: String get() = ext.smsBlockedBody
    val customThemeTitle: String get() = ext.customThemeTitle
    val primaryColor: String get() = ext.primaryColor
    val preview: String get() = ext.preview
    val complementColorAuto: String get() = ext.complementColorAuto
    val baseTheme: String get() = ext.baseTheme
    val lightTheme: String get() = ext.lightTheme
    val darkTheme: String get() = ext.darkTheme
    val limit1: String get() = ext.limit1
    val limit2: String get() = ext.limit2
    val limit3: String get() = ext.limit3
    val limit4: String get() = ext.limit4
    val limit5: String get() = ext.limit5
    val limit6: String get() = ext.limit6
    val limit7: String get() = ext.limit7
    val backupAutoNote: String get() = ext.backupAutoNote
    val backupReplaceWarning: String get() = ext.backupReplaceWarning
    val langNameFa: String get() = ext.langNameFa
    val langNameEn: String get() = ext.langNameEn
    val langNameAr: String get() = ext.langNameAr
    val langSubEn: String get() = ext.langSubEn
    val langSubAr: String get() = ext.langSubAr
    val langSubFa: String get() = ext.langSubFa
    val clearHistory: String get() = ext.clearHistory
    val clearHistoryConfirm: String get() = ext.clearHistoryConfirm
    val clearAction: String get() = ext.clearAction
    val historyEmptySub: String get() = ext.historyEmptySub
    val statsEmptySub: String get() = ext.statsEmptySub
    val statsWeekEmpty: String get() = ext.statsWeekEmpty
    val statsNoEvents: String get() = ext.statsNoEvents
    val statsEnterLabel: String get() = ext.statsEnterLabel
    val statsExitLabel: String get() = ext.statsExitLabel
    val rangeTo: String get() = ext.rangeTo
    val monthJan: String get() = ext.monthJan
    val monthFeb: String get() = ext.monthFeb
    val monthMar: String get() = ext.monthMar
    val monthApr: String get() = ext.monthApr
    val monthMay: String get() = ext.monthMay
    val monthJun: String get() = ext.monthJun
    val monthJul: String get() = ext.monthJul
    val monthAug: String get() = ext.monthAug
    val monthSep: String get() = ext.monthSep
    val monthOct: String get() = ext.monthOct
    val monthNov: String get() = ext.monthNov
    val monthDec: String get() = ext.monthDec
    val mapLocationPermNeeded: String get() = ext.mapLocationPermNeeded
    val mapLocationFailed: String get() = ext.mapLocationFailed
    val pickFromContacts: String get() = ext.pickFromContacts
    val contactsNoAccess: String get() = ext.contactsNoAccess
    val loading: String get() = ext.loading
    val contactsSearchLabel: String get() = ext.contactsSearchLabel
    val contactsNoneFound: String get() = ext.contactsNoneFound
    val done: String get() = ext.done
    val selectedCd: String get() = ext.selectedCd
    val batteryGuideTitle: String get() = ext.batteryGuideTitle
    val batteryGuideIntro: String get() = ext.batteryGuideIntro
    val guide1Title: String get() = ext.guide1Title
    val guide1Step1: String get() = ext.guide1Step1
    val guide1Step2: String get() = ext.guide1Step2
    val guide1Step3: String get() = ext.guide1Step3
    val guide1Step4: String get() = ext.guide1Step4
    val guide2Title: String get() = ext.guide2Title
    val guide2Step1: String get() = ext.guide2Step1
    val guide2Step2: String get() = ext.guide2Step2
    val guide2Step3: String get() = ext.guide2Step3
    val guide2Step4: String get() = ext.guide2Step4
    val guide3Title: String get() = ext.guide3Title
    val guide3Step1: String get() = ext.guide3Step1
    val guide3Step2: String get() = ext.guide3Step2
    val guide3Step3: String get() = ext.guide3Step3
    val soundAlarm: String get() = ext.soundAlarm
    val soundRingtone: String get() = ext.soundRingtone
    val soundNotification: String get() = ext.soundNotification
    val locNotFound: String get() = ext.locNotFound
    val locPermNeeded: String get() = ext.locPermNeeded
    val customSoundSelected: String get() = ext.customSoundSelected
    val clipboardEmpty: String get() = ext.clipboardEmpty
    val pasteFormatError: String get() = ext.pasteFormatError
    val pasteInvalidCoords: String get() = ext.pasteInvalidCoords
    val approachDistanceError: String get() = ext.approachDistanceError
    val stayDurationError: String get() = ext.stayDurationError
    val maxActiveZonesError: String get() = ext.maxActiveZonesError
    val saveError: String get() = ext.saveError
    val zoneNameHint: String get() = ext.zoneNameHint
    val latitudeFull: String get() = ext.latitudeFull
    val longitudeFull: String get() = ext.longitudeFull
    val coordsCopied: String get() = ext.coordsCopied
    val copyCoords: String get() = ext.copyCoords
    val paste: String get() = ext.paste
    val coordsGuide: String get() = ext.coordsGuide
    val meterUnit: String get() = ext.meterUnit
    val kmUnit: String get() = ext.kmUnit
    val gpsAccuracyWarning: String get() = ext.gpsAccuracyWarning
    val checkIntervalTitle: String get() = ext.checkIntervalTitle
    val checkIntervalSub: String get() = ext.checkIntervalSub
    val checkIntervalLabel: String get() = ext.checkIntervalLabel
    val unitSecond: String get() = ext.unitSecond
    val unitMinute: String get() = ext.unitMinute
    val unitContinuous: String get() = ext.unitContinuous
    val continuousNote: String get() = ext.continuousNote
    val scheduleEnable: String get() = ext.scheduleEnable
    val scheduleEnableSub: String get() = ext.scheduleEnableSub
    val daySat: String get() = ext.daySat
    val daySun: String get() = ext.daySun
    val dayMon: String get() = ext.dayMon
    val dayTue: String get() = ext.dayTue
    val dayWed: String get() = ext.dayWed
    val dayThu: String get() = ext.dayThu
    val dayFri: String get() = ext.dayFri
    val fromHour: String get() = ext.fromHour
    val toHour: String get() = ext.toHour
    val scheduleExample: String get() = ext.scheduleExample
    val exitInsideNote: String get() = ext.exitInsideNote
    val exitStopSub: String get() = ext.exitStopSub
    val exitSequenceSub: String get() = ext.exitSequenceSub
    val exitContinueTitle: String get() = ext.exitContinueTitle
    val exitContinueSub: String get() = ext.exitContinueSub
    val seqCountLabel: String get() = ext.seqCountLabel
    val seqDurationLabel: String get() = ext.seqDurationLabel
    val seqGapLabel: String get() = ext.seqGapLabel
    val seqManualStopNote: String get() = ext.seqManualStopNote
    val pickCustomSound: String get() = ext.pickCustomSound
    val previewCustomSoundCd: String get() = ext.previewCustomSoundCd
    val deleteCustomSoundCd: String get() = ext.deleteCustomSoundCd
    val customSoundReplaces: String get() = ext.customSoundReplaces
    val alertBothTitle: String get() = ext.alertBothTitle
    val alertBothSub: String get() = ext.alertBothSub
    val alertSoundOnlyTitle: String get() = ext.alertSoundOnlyTitle
    val alertSoundOnlySub: String get() = ext.alertSoundOnlySub
    val alertVibrateOnlyTitle: String get() = ext.alertVibrateOnlyTitle
    val alertVibrateOnlySub: String get() = ext.alertVibrateOnlySub
    val alertNoneTitle: String get() = ext.alertNoneTitle
    val alertNoneSub: String get() = ext.alertNoneSub
    val voiceAnnounceTitle: String get() = ext.voiceAnnounceTitle
    val voiceAnnounceSub: String get() = ext.voiceAnnounceSub
    val approachTitle: String get() = ext.approachTitle
    val approachSub: String get() = ext.approachSub
    val approachDistanceHint: String get() = ext.approachDistanceHint
    val approachDistanceLabel: String get() = ext.approachDistanceLabel
    val stayTitle: String get() = ext.stayTitle
    val staySub: String get() = ext.staySub
    val stayDurationHint: String get() = ext.stayDurationHint
    val stayMinutesLabel: String get() = ext.stayMinutesLabel
    val smsNumbersLabel: String get() = ext.smsNumbersLabel
    val smsMessageLabel: String get() = ext.smsMessageLabel
    val smsNote: String get() = ext.smsNote
    val silentWindowTitle: String get() = ext.silentWindowTitle
    val silentWindowSub: String get() = ext.silentWindowSub
    val errZoneName: String get() = ext.errZoneName
    val errLatitude: String get() = ext.errLatitude
    val errLongitude: String get() = ext.errLongitude
    val errRadiusPositive: String get() = ext.errRadiusPositive
    val errRadiusTooBig: String get() = ext.errRadiusTooBig
    val errInvalidNumber: String get() = ext.errInvalidNumber
    val errSeqCount: String get() = ext.errSeqCount
    val errSeqDuration: String get() = ext.errSeqDuration
    val errSeqGap: String get() = ext.errSeqGap
    val errCheckRange: String get() = ext.errCheckRange
    val relJustNow: String get() = ext.relJustNow
    val relMinutesAgo: String get() = ext.relMinutesAgo
    val relHoursAgo: String get() = ext.relHoursAgo
    val relYesterday: String get() = ext.relYesterday
    val relDaysAgo: String get() = ext.relDaysAgo
    val langSceneSubtitle: String get() = ext.langSceneSubtitle
    val langSampleFa: String get() = ext.langSampleFa
    val langSampleEn: String get() = ext.langSampleEn
    val langSampleAr: String get() = ext.langSampleAr
}

/**
 * بخش دوم جدول رشته‌های رابط کاربری.
 * به‌علت سقف ۲۵۵ ورودیِ سازنده در JVM، کلیدهای باقی‌مانده اینجا نگه‌داری
 * می‌شوند و UiStrings آن‌ها را با ارجاع ساده در دسترس می‌گذارد.
 */
data class UiStringsExt(
    val widgetZoneNotSelectedSub: String,
    val widgetPickTitle: String,
    val widgetNoZones: String,
    val widgetNoZonesSub: String,
    val zoneFallbackName: String,
    // کلیدهای جاروی عمیق صفحه‌های داخلی (نسخه ۲.۹.۰)
    val settingsCd: String,
    val createZone: String,
    val batteryWarning: String,
    val batteryWarningAction: String,
    val backupRestored: String,
    val sectionPermissions: String,
    val sectionAlerts: String,
    val sectionAppearance: String,
    val sectionBackupData: String,
    val sectionHelpAbout: String,
    val permIntro: String,
    val permPreciseLocation: String,
    val permPreciseLocationDesc: String,
    val permBackgroundLocation: String,
    val permBackgroundLocationDesc: String,
    val permNotificationsDesc: String,
    val permSmsTitle: String,
    val permSmsDesc: String,
    val smsPermGranted: String,
    val contactsPermGranted: String,
    val permContactsDesc: String,
    val permBatteryDesc: String,
    val permAdjust: String,
    val permRequest: String,
    val permGranted: String,
    val permNotGranted: String,
    val openAppSettings: String,
    val quietHoursSub: String,
    val fromLabel: String,
    val toLabel: String,
    val later: String,
    val smsBlockedTitle: String,
    val smsBlockedBody: String,
    val customThemeTitle: String,
    val primaryColor: String,
    val preview: String,
    val complementColorAuto: String,
    val baseTheme: String,
    val lightTheme: String,
    val darkTheme: String,
    val limit1: String,
    val limit2: String,
    val limit3: String,
    val limit4: String,
    val limit5: String,
    val limit6: String,
    val limit7: String,
    val backupAutoNote: String,
    val backupReplaceWarning: String,
    val langNameFa: String,
    val langNameEn: String,
    val langNameAr: String,
    val langSubEn: String,
    val langSubAr: String,
    val langSubFa: String,
    val clearHistory: String,
    val clearHistoryConfirm: String,
    val clearAction: String,
    val historyEmptySub: String,
    val statsEmptySub: String,
    val statsWeekEmpty: String,
    val statsNoEvents: String,
    val statsEnterLabel: String,
    val statsExitLabel: String,
    val rangeTo: String,
    val monthJan: String,
    val monthFeb: String,
    val monthMar: String,
    val monthApr: String,
    val monthMay: String,
    val monthJun: String,
    val monthJul: String,
    val monthAug: String,
    val monthSep: String,
    val monthOct: String,
    val monthNov: String,
    val monthDec: String,
    val mapLocationPermNeeded: String,
    val mapLocationFailed: String,
    val pickFromContacts: String,
    val contactsNoAccess: String,
    val loading: String,
    val contactsSearchLabel: String,
    val contactsNoneFound: String,
    val done: String,
    val selectedCd: String,
    val batteryGuideTitle: String,
    val batteryGuideIntro: String,
    val guide1Title: String,
    val guide1Step1: String,
    val guide1Step2: String,
    val guide1Step3: String,
    val guide1Step4: String,
    val guide2Title: String,
    val guide2Step1: String,
    val guide2Step2: String,
    val guide2Step3: String,
    val guide2Step4: String,
    val guide3Title: String,
    val guide3Step1: String,
    val guide3Step2: String,
    val guide3Step3: String,
    val soundAlarm: String,
    val soundRingtone: String,
    val soundNotification: String,
    val locNotFound: String,
    val locPermNeeded: String,
    val customSoundSelected: String,
    val clipboardEmpty: String,
    val pasteFormatError: String,
    val pasteInvalidCoords: String,
    val approachDistanceError: String,
    val stayDurationError: String,
    val maxActiveZonesError: String,
    val saveError: String,
    val zoneNameHint: String,
    val latitudeFull: String,
    val longitudeFull: String,
    val coordsCopied: String,
    val copyCoords: String,
    val paste: String,
    val coordsGuide: String,
    val meterUnit: String,
    val kmUnit: String,
    val gpsAccuracyWarning: String,
    val checkIntervalTitle: String,
    val checkIntervalSub: String,
    val checkIntervalLabel: String,
    val unitSecond: String,
    val unitMinute: String,
    val unitContinuous: String,
    val continuousNote: String,
    val scheduleEnable: String,
    val scheduleEnableSub: String,
    val daySat: String,
    val daySun: String,
    val dayMon: String,
    val dayTue: String,
    val dayWed: String,
    val dayThu: String,
    val dayFri: String,
    val fromHour: String,
    val toHour: String,
    val scheduleExample: String,
    val exitInsideNote: String,
    val exitStopSub: String,
    val exitSequenceSub: String,
    val exitContinueTitle: String,
    val exitContinueSub: String,
    val seqCountLabel: String,
    val seqDurationLabel: String,
    val seqGapLabel: String,
    val seqManualStopNote: String,
    val pickCustomSound: String,
    val previewCustomSoundCd: String,
    val deleteCustomSoundCd: String,
    val customSoundReplaces: String,
    val alertBothTitle: String,
    val alertBothSub: String,
    val alertSoundOnlyTitle: String,
    val alertSoundOnlySub: String,
    val alertVibrateOnlyTitle: String,
    val alertVibrateOnlySub: String,
    val alertNoneTitle: String,
    val alertNoneSub: String,
    val voiceAnnounceTitle: String,
    val voiceAnnounceSub: String,
    val approachTitle: String,
    val approachSub: String,
    val approachDistanceHint: String,
    val approachDistanceLabel: String,
    val stayTitle: String,
    val staySub: String,
    val stayDurationHint: String,
    val stayMinutesLabel: String,
    val smsNumbersLabel: String,
    val smsMessageLabel: String,
    val smsNote: String,
    val silentWindowTitle: String,
    val silentWindowSub: String,
    val errZoneName: String,
    val errLatitude: String,
    val errLongitude: String,
    val errRadiusPositive: String,
    val errRadiusTooBig: String,
    val errInvalidNumber: String,
    val errSeqCount: String,
    val errSeqDuration: String,
    val errSeqGap: String,
    val errCheckRange: String,
    val relJustNow: String,
    val relMinutesAgo: String,
    val relHoursAgo: String,
    val relYesterday: String,
    val relDaysAgo: String,
    val langSceneSubtitle: String,
    val langSampleFa: String,
    val langSampleEn: String,
    val langSampleAr: String
) {
    companion object {
        val FA = UiStringsExt(
            widgetZoneNotSelectedSub = "برای انتخاب محدوده بزنید",
            widgetPickTitle = "انتخاب محدوده برای ویجت",
            widgetNoZones = "هنوز محدوده‌ای نساخته‌اید.",
            widgetNoZonesSub = "اول در برنامه یک محدوده بسازید، بعد ویجت را اضافه کنید.",
            zoneFallbackName = "محدوده",
            settingsCd = "تنظیمات",
            createZone = "ساخت محدوده",
            batteryWarning = "بهینه‌سازی باتری فعال است؛ ممکن است هشدارها با تأخیر برسند یا متوقف شوند.",
            batteryWarningAction = "غیرفعال کردن بهینه‌سازی",
            backupRestored = "محدوده‌ها از فایل پشتیبان بازیابی شدند",
            sectionPermissions = "دسترسی‌ها و مجوزها",
            sectionAlerts = "هشدار و اعلان",
            sectionAppearance = "ظاهر و زبان",
            sectionBackupData = "پشتیبان و داده‌ها",
            sectionHelpAbout = "راهنما و درباره",
            permIntro = "برای کارکرد درست هشدارها، مجوزهای زیر لازم است. اگر مجوزی رد شده، از همین‌جا دوباره درخواست کنید یا به تنظیمات برنامه بروید.",
            permPreciseLocation = "مکان دقیق",
            permPreciseLocationDesc = "برای تشخیص ورود به محدوده لازم است.",
            permBackgroundLocation = "مکان در پس‌زمینه",
            permBackgroundLocationDesc = "برای هشدار وقتی برنامه باز نیست (اندروید ۱۰ به بالا).",
            permNotificationsDesc = "برای نمایش هشدار و دکمه توقف (اندروید ۱۳ به بالا).",
            permSmsTitle = "پیامک",
            permSmsDesc = "برای ارسال پیامک اطلاع‌رسانی هنگام ورود به محدوده، فقط به شماره‌هایی که خودتان در هر محدوده ثبت می‌کنید.",
            smsPermGranted = "مجوز پیامک فعال است",
            contactsPermGranted = "مجوز مخاطبین فعال است",
            permContactsDesc = "برای انتخاب شماره پیامک از دفترچه تلفن، هنگام ویرایش محدوده.",
            permBatteryDesc = "بدون محدودیت باتری، پایش در پس‌زمینه متوقف نمی‌شود.",
            permAdjust = "تنظیم",
            permRequest = "درخواست",
            permGranted = "داده شده",
            permNotGranted = "داده نشده",
            openAppSettings = "باز کردن تنظیمات برنامه",
            quietHoursSub = "در این بازه، هشدار فقط با اعلان و بدون صدا نمایش داده می‌شود.",
            fromLabel = "از",
            toLabel = "تا",
            later = "بعداً",
            smsBlockedTitle = "مجوز پیامک مسدود شده است",
            smsBlockedBody = "به نظر می‌رسد اندروید درخواست مجوز پیامک را مسدود کرده است؛ این اتفاق برای برنامه‌هایی که خارج از فروشگاه نصب می‌شوند طبیعی است و جای نگرانی نیست. برای فعال کردن مجوز این کارها را انجام دهید:\n۱. روی «باز کردن تنظیمات برنامه» در پایین همین پیام بزنید.\n۲. در صفحه‌ای که باز می‌شود، روی سه‌نقطهٔ بالای صفحه بزنید.\n۳. گزینهٔ «اجازه به تنظیمات محدود» (Allow restricted settings) را انتخاب و تأیید کنید.\n۴. سپس به بخش «مجوزها» (Permissions) بروید، «پیامک» (SMS) را انتخاب کنید و «اجازه دادن» (Allow) را بزنید.\n۵. به برنامه برگردید؛ وضعیت مجوز به‌صورت خودکار به‌روز می‌شود.",
            customThemeTitle = "شخصی‌سازی تم",
            primaryColor = "رنگ اصلی",
            preview = "پیش‌نمایش",
            complementColorAuto = "رنگ مکمل (خودکار)",
            baseTheme = "تم پایه",
            lightTheme = "روشن",
            darkTheme = "تیره",
            limit1 = "حداکثر ۱۰۰ ژئوفنس هم‌زمان توسط اندروید پشتیبانی می‌شود.",
            limit2 = "پس از «Force Stop» کردن برنامه از تنظیمات، ژئوفنس‌ها توسط سیستم حذف می‌شوند و تا باز شدن دوباره برنامه کار نمی‌کنند.",
            limit3 = "در حالت ذخیره باتری (Doze) یا بهینه‌سازی تهاجمی باتری — به‌ویژه در گوشی‌های سامسونگ — ممکن است رویداد ورود/خروج با تأخیر برسد.",
            limit4 = "برای هشدار در پس‌زمینه باید دسترسی مکان «همیشه» (Allow all the time) داده شود.",
            limit5 = "دقت تشخیص به GPS و شبکه بستگی دارد؛ داخل ساختمان ممکن است خطا داشته باشد.",
            limit6 = "اگر هنگام ثبت محدوده داخل آن باشید، هشداری فعال نمی‌شود؛ فقط «ورود جدید» هشدار می‌دهد.",
            limit7 = "خاموش بودن گوشی یا لغو مجوزها، تشخیص ورود را متوقف می‌کند؛ برنامه چنین چیزی را تضمین نمی‌کند.",
            backupAutoNote = "برنامه به‌طور خودکار از محدوده‌ها در پوشه Documents گوشی پشتیبان می‌گیرد؛ اگر برنامه را حذف و دوباره نصب کنید، محدوده‌ها خودبه‌خود برمی‌گردند.",
            backupReplaceWarning = "ورودی پشتیبان همه محدوده‌های فعلی را با محتوای فایل جایگزین می‌کند.",
            langNameFa = "فارسی",
            langNameEn = "English",
            langNameAr = "العربية",
            langSubEn = "English",
            langSubAr = "Arabic",
            langSubFa = "Persian",
            clearHistory = "پاک کردن تاریخچه",
            clearHistoryConfirm = "همه رویدادهای ثبت‌شده پاک شود؟",
            clearAction = "پاک شود",
            historyEmptySub = "با ورود و خروج از محدوده‌ها، اینجا ثبت می‌شود.",
            statsEmptySub = "با ورود و خروج از محدوده‌ها، آمار اینجا ساخته می‌شود.",
            statsWeekEmpty = "در این هفته رویدادی ثبت نشده است.",
            statsNoEvents = "بدون رویداد",
            statsEnterLabel = "ورود",
            statsExitLabel = "خروج",
            rangeTo = "تا",
            monthJan = "ژانویه",
            monthFeb = "فوریه",
            monthMar = "مارس",
            monthApr = "آوریل",
            monthMay = "مه",
            monthJun = "ژوئن",
            monthJul = "ژوئیه",
            monthAug = "اوت",
            monthSep = "سپتامبر",
            monthOct = "اکتبر",
            monthNov = "نوامبر",
            monthDec = "دسامبر",
            mapLocationPermNeeded = "برای این قابلیت مجوز مکان لازم است",
            mapLocationFailed = "دریافت موقعیت ناموفق بود",
            pickFromContacts = "انتخاب از مخاطبین",
            contactsNoAccess = "دسترسی به مخاطبین داده نشد؛ شماره را دستی وارد کنید.",
            loading = "در حال بارگذاری…",
            contactsSearchLabel = "جستجوی نام یا شماره",
            contactsNoneFound = "مخاطبی یافت نشد",
            done = "تمام",
            selectedCd = "انتخاب‌شده",
            batteryGuideTitle = "راهنمای مصرف باتری",
            batteryGuideIntro = "برای این‌که هشدار ورود به محدوده حتی وقتی برنامه باز نیست کار کند، اندروید نباید برنامه را به‌خاطر مصرف باتری متوقف کند. مراحل زیر برای گوشی‌های سامسونگ است؛ در برندهای دیگر مسیر مشابهی دارد.",
            guide1Title = "۱. برداشتن محدودیت باتری",
            guide1Step1 = "به «تنظیمات» گوشی بروید.",
            guide1Step2 = "«برنامه‌ها» را باز کنید و «محدوده هشدار» را پیدا کنید.",
            guide1Step3 = "وارد بخش «باتری» شوید.",
            guide1Step4 = "گزینه «بدون محدودیت» (Unrestricted) را انتخاب کنید.",
            guide2Title = "۲. خارج کردن از برنامه‌های در خواب",
            guide2Step1 = "در تنظیمات، به «مراقبت از باتری و دستگاه» بروید.",
            guide2Step2 = "«باتری» و سپس «محدودیت‌های استفاده پس‌زمینه» را باز کنید.",
            guide2Step3 = "«برنامه‌های در خواب عمیق» (Deep sleeping apps) را بررسی کنید.",
            guide2Step4 = "اگر «محدوده هشدار» آن‌جاست، آن را حذف کنید تا در خواب عمیق نرود.",
            guide3Title = "۳. نکات مهم",
            guide3Step1 = "از «Force Stop» (توقف اجباری) برنامه از تنظیمات خودداری کنید؛ پس از آن ژئوفنس‌ها تا باز شدن دوباره برنامه کار نمی‌کنند.",
            guide3Step2 = "دسترسی مکان را روی «همیشه» (Allow all the time) بگذارید.",
            guide3Step3 = "حالت «صرفه‌جویی در مصرف باتری» ممکن است رویدادها را با تأخیر برساند.",
            soundAlarm = "آژیر",
            soundRingtone = "زنگ",
            soundNotification = "اعلان",
            locNotFound = "موقعیت یافت نشد؛ از روشن بودن مکان دستگاه مطمئن شوید.",
            locPermNeeded = "برای دریافت موقعیت فعلی، مجوز مکان لازم است.",
            customSoundSelected = "صدای انتخاب‌شده",
            clipboardEmpty = "حافظه موقت خالی است.",
            pasteFormatError = "قالب باید «عرض،طول» باشد؛ مثلاً 35.7,51.4",
            pasteInvalidCoords = "مختصات الصاق‌شده معتبر نیست.",
            approachDistanceError = "فاصله هشدار نزدیک شدن باید عددی بین ۱۰۰ تا ۵۰۰۰ متر باشد.",
            stayDurationError = "مدت یادآور ماندن باید عددی بین ۵ تا ۱۴۴۰ دقیقه باشد.",
            maxActiveZonesError = "حداکثر ۱۰۰ محدوده فعال پشتیبانی می‌شود.",
            saveError = "خطا در ذخیره‌سازی رخ داد.",
            zoneNameHint = "نام محدوده (مثلاً خانه)",
            latitudeFull = "عرض جغرافیایی",
            longitudeFull = "طول جغرافیایی",
            coordsCopied = "مختصات کپی شد",
            copyCoords = "کپی مختصات",
            paste = "الصاق",
            coordsGuide = "راهنما: در گوگل‌مپ نقطه را لمس طولانی کنید تا مختصات نمایش داده شود؛ آن را کپی کنید (مثل 34.633719,50.886485) و با دکمه «الصاق» یا مستقیم در کادر عرض جغرافیایی بچسبانید.",
            meterUnit = "متر",
            kmUnit = "کیلومتر",
            gpsAccuracyWarning = "دقت مکان‌یابی گوشی حدود ۱۰ تا ۲۰ متر است؛ شعاع کمتر از ۵۰ متر ممکن است دقیق عمل نکند.",
            checkIntervalTitle = "فاصله بررسی دقیق",
            checkIntervalSub = "چون شعاع کمتر از ۲۰۰ متر است، موقعیت شما با همین فاصله زمانی بررسی می‌شود.",
            checkIntervalLabel = "هر چند وقت یک بار بررسی شود",
            unitSecond = "ثانیه",
            unitMinute = "دقیقه",
            unitContinuous = "مداوم",
            continuousNote = "در حالت مداوم، موقعیت تقریباً هر لحظه (هر ۲ ثانیه) بررسی می‌شود؛ مصرف باتری بیشتر است.",
            scheduleEnable = "فعال‌سازی زمان‌بندی",
            scheduleEnableSub = "هشدار فقط در روزها و ساعات انتخاب‌شده فعال باشد.",
            daySat = "شنبه",
            daySun = "یکشنبه",
            dayMon = "دوشنبه",
            dayTue = "سه‌شنبه",
            dayWed = "چهارشنبه",
            dayThu = "پنجشنبه",
            dayFri = "جمعه",
            fromHour = "از ساعت",
            toHour = "تا ساعت",
            scheduleExample = "مثال: برای محل کار، شنبه تا چهارشنبه از ۸ تا ۱۸.",
            exitInsideNote = "داخل محدوده، هشدار فقط با توقف دستی قطع می‌شود.",
            exitStopSub = "با خروج از محدوده، هشدار قطع می‌شود.",
            exitSequenceSub = "پس از خروج، الگوی تکراری پخش می‌شود.",
            exitContinueTitle = "ادامه تا توقف دستی",
            exitContinueSub = "هشدار تا توقف دستی ادامه دارد.",
            seqCountLabel = "تعداد دفعات",
            seqDurationLabel = "مدت هر آلارم (ثانیه)",
            seqGapLabel = "مکث بین آلارم‌ها (ثانیه)",
            seqManualStopNote = "اگر هر کدام را دستی متوقف کنید، آلارم‌های بعدی این محدوده به صدا درنمی‌آید.",
            pickCustomSound = "انتخاب صدای دلخواه از گوشی",
            previewCustomSoundCd = "پیشنمایش صدای دلخواه",
            deleteCustomSoundCd = "حذف صدای دلخواه",
            customSoundReplaces = "صدای دلخواه جایگزین صدای سیستمی بالا می‌شود.",
            alertBothTitle = "صدا و لرزش",
            alertBothSub = "صدای هشدار به‌همراه لرزش پخش می‌شود.",
            alertSoundOnlyTitle = "فقط صدا",
            alertSoundOnlySub = "فقط صدای هشدار، بدون لرزش.",
            alertVibrateOnlyTitle = "فقط لرزش",
            alertVibrateOnlySub = "فقط لرزش، بدون صدا.",
            alertNoneTitle = "هیچ‌کدام",
            alertNoneSub = "فقط اعلان نمایش داده می‌شود؛ بدون صدا و لرزش.",
            voiceAnnounceTitle = "خواندن نام محدوده",
            voiceAnnounceSub = "هنگام ورود، نام محدوده با صدای خود گوشی خوانده می‌شود.",
            approachTitle = "هشدار قبل از رسیدن",
            approachSub = "وقتی به فاصله مشخصی از محدوده رسیدید، یک اعلان نزدیک شدن می‌آید.",
            approachDistanceHint = "فاصله باید عددی بین ۱۰۰ تا ۵۰۰۰ متر باشد.",
            approachDistanceLabel = "فاصله هشدار (متر)",
            stayTitle = "یادآوری ماندن طولانی",
            staySub = "اگر بیشتر از مدت تعیین‌شده داخل محدوده ماندید، یک اعلان یادآوری می‌آید.",
            stayDurationHint = "مدت باید عددی بین ۵ تا ۱۴۴۰ دقیقه باشد.",
            stayMinutesLabel = "بعد از چند دقیقه یادآوری شود",
            smsNumbersLabel = "شماره‌های تلفن (هر خط یک شماره)",
            smsMessageLabel = "متن پیامک",
            smsNote = "با ورود به محدوده، این پیام برای شماره‌ها پیامک می‌شود.",
            silentWindowTitle = "بازه بی‌صدا",
            silentWindowSub = "در این بازه، ورود به محدوده فقط با اعلان (بدون صدا و لرزش) اطلاع داده می‌شود.",
            errZoneName = "نام محدوده را وارد کنید.",
            errLatitude = "عرض جغرافیایی باید عددی بین -۹۰ و +۹۰ باشد.",
            errLongitude = "طول جغرافیایی باید عددی بین -۱۸۰ و +۱۸۰ باشد.",
            errRadiusPositive = "شعاع باید عددی مثبت باشد.",
            errRadiusTooBig = "شعاع نمی‌تواند بیشتر از ۱۰۰۰ کیلومتر باشد.",
            errInvalidNumber = "عدد معتبر وارد کنید.",
            errSeqCount = "تعداد دفعات باید بین ۱ تا ۲۰ باشد.",
            errSeqDuration = "مدت هر آلارم باید بین ۳ تا ۱۲۰ ثانیه باشد.",
            errSeqGap = "مکث بین آلارم‌ها باید بین ۰ تا ۶۰۰ ثانیه باشد.",
            errCheckRange = "بازه مجاز: ۵ ثانیه تا ۶۰ دقیقه.",
            relJustNow = "لحظاتی پیش",
            relMinutesAgo = "%1\$s دقیقه پیش",
            relHoursAgo = "%1\$s ساعت پیش",
            relYesterday = "دیروز",
            relDaysAgo = "%1\$s روز پیش",
            langSceneSubtitle = "Choose your language · اختر لغتك",
            langSampleFa = "سلام! خوش آمدید",
            langSampleEn = "Hello! Welcome",
            langSampleAr = "مرحباً! أهلاً بك"
        )

        val EN = UiStringsExt(
            widgetZoneNotSelectedSub = "Tap to select a zone",
            widgetPickTitle = "Choose a zone for the widget",
            widgetNoZones = "You haven't created any zones yet.",
            widgetNoZonesSub = "Create a zone in the app first, then add the widget.",
            zoneFallbackName = "Zone",
            settingsCd = "Settings",
            createZone = "Create zone",
            batteryWarning = "Battery optimization is on; alerts may arrive late or be stopped.",
            batteryWarningAction = "Disable optimization",
            backupRestored = "Zones were restored from the backup file",
            sectionPermissions = "Access & permissions",
            sectionAlerts = "Alerts & notifications",
            sectionAppearance = "Appearance & language",
            sectionBackupData = "Backup & data",
            sectionHelpAbout = "Help & about",
            permIntro = "The permissions below are needed for alerts to work properly. If one was denied, request it again here or open the app settings.",
            permPreciseLocation = "Precise location",
            permPreciseLocationDesc = "Needed to detect zone entries.",
            permBackgroundLocation = "Background location",
            permBackgroundLocationDesc = "For alerts when the app is closed (Android 10+).",
            permNotificationsDesc = "To show alerts and the stop button (Android 13+).",
            permSmsTitle = "SMS",
            permSmsDesc = "To send an SMS on zone entry, only to numbers you add to each zone.",
            smsPermGranted = "SMS permission is granted",
            contactsPermGranted = "Contacts permission is granted",
            permContactsDesc = "To pick SMS numbers from your contacts when editing a zone.",
            permBatteryDesc = "With unrestricted battery, monitoring won't be stopped in the background.",
            permAdjust = "Adjust",
            permRequest = "Request",
            permGranted = "Granted",
            permNotGranted = "Not granted",
            openAppSettings = "Open app settings",
            quietHoursSub = "In this window, alerts are shown as notifications without sound.",
            fromLabel = "From",
            toLabel = "To",
            later = "Later",
            smsBlockedTitle = "SMS permission is blocked",
            smsBlockedBody = "It looks like Android has blocked the SMS permission request; this is normal for apps installed outside a store and is no cause for concern. To enable it:\n1. Tap “Open app settings” at the bottom of this message.\n2. On the page that opens, tap the three dots at the top.\n3. Choose “Allow restricted settings” and confirm.\n4. Then go to “Permissions”, choose “SMS” and tap “Allow”.\n5. Return to the app; the permission status updates automatically.",
            customThemeTitle = "Customize theme",
            primaryColor = "Primary color",
            preview = "Preview",
            complementColorAuto = "Accent color (automatic)",
            baseTheme = "Base theme",
            lightTheme = "Light",
            darkTheme = "Dark",
            limit1 = "Android supports at most 100 geofences at the same time.",
            limit2 = "After force-stopping the app from Settings, the system removes geofences and they won't work until the app is opened again.",
            limit3 = "In battery saving (Doze) or aggressive battery optimization — especially on Samsung phones — entry/exit events may arrive late.",
            limit4 = "Background alerts require the “Always” (Allow all the time) location permission.",
            limit5 = "Detection accuracy depends on GPS and network; indoors it may be off.",
            limit6 = "If you are inside a zone when you register it, no alert fires; only a new entry alerts.",
            limit7 = "Turning the phone off or revoking permissions stops entry detection; the app makes no guarantee against that.",
            backupAutoNote = "The app automatically backs up zones to the phone's Documents folder; if you uninstall and reinstall, your zones return on their own.",
            backupReplaceWarning = "Importing a backup replaces all current zones with the file's contents.",
            langNameFa = "فارسی",
            langNameEn = "English",
            langNameAr = "العربية",
            langSubEn = "English",
            langSubAr = "Arabic",
            langSubFa = "Persian",
            clearHistory = "Clear history",
            clearHistoryConfirm = "Delete all recorded events?",
            clearAction = "Delete",
            historyEmptySub = "Zone entries and exits are recorded here.",
            statsEmptySub = "Stats are built here from zone entries and exits.",
            statsWeekEmpty = "No events recorded this week.",
            statsNoEvents = "No events",
            statsEnterLabel = "In",
            statsExitLabel = "Out",
            rangeTo = "to",
            monthJan = "January",
            monthFeb = "February",
            monthMar = "March",
            monthApr = "April",
            monthMay = "May",
            monthJun = "June",
            monthJul = "July",
            monthAug = "August",
            monthSep = "September",
            monthOct = "October",
            monthNov = "November",
            monthDec = "December",
            mapLocationPermNeeded = "Location permission is needed for this feature",
            mapLocationFailed = "Couldn't get your location",
            pickFromContacts = "Pick from contacts",
            contactsNoAccess = "Contacts access wasn't granted; enter the number manually.",
            loading = "Loading…",
            contactsSearchLabel = "Search name or number",
            contactsNoneFound = "No contacts found",
            done = "Done",
            selectedCd = "Selected",
            batteryGuideTitle = "Battery usage guide",
            batteryGuideIntro = "For zone entry alerts to work even when the app is closed, Android must not stop the app for battery use. The steps below are for Samsung phones; other brands have a similar path.",
            guide1Title = "1. Remove the battery restriction",
            guide1Step1 = "Open your phone's Settings.",
            guide1Step2 = "Open “Apps” and find “Zone Alert”.",
            guide1Step3 = "Go to the “Battery” section.",
            guide1Step4 = "Select “Unrestricted”.",
            guide2Title = "2. Remove from sleeping apps",
            guide2Step1 = "In Settings, go to “Battery and device care”.",
            guide2Step2 = "Open “Battery” and then “Background usage limits”.",
            guide2Step3 = "Check “Deep sleeping apps”.",
            guide2Step4 = "If “Zone Alert” is there, remove it so it never deep-sleeps.",
            guide3Title = "3. Important notes",
            guide3Step1 = "Avoid force-stopping the app from Settings; after that, geofences won't work until the app is opened again.",
            guide3Step2 = "Set location access to “Always” (Allow all the time).",
            guide3Step3 = "Battery saver mode may deliver events late.",
            soundAlarm = "Siren",
            soundRingtone = "Ringtone",
            soundNotification = "Notification",
            locNotFound = "Location not found; make sure device location is on.",
            locPermNeeded = "Location permission is needed to get your current location.",
            customSoundSelected = "Selected sound",
            clipboardEmpty = "Clipboard is empty.",
            pasteFormatError = "Format must be “lat,lng”; e.g. 35.7,51.4",
            pasteInvalidCoords = "The pasted coordinates are invalid.",
            approachDistanceError = "Approach alert distance must be a number between 100 and 5000 meters.",
            stayDurationError = "Stay reminder duration must be a number between 5 and 1440 minutes.",
            maxActiveZonesError = "At most 100 active zones are supported.",
            saveError = "Saving failed.",
            zoneNameHint = "Zone name (e.g. Home)",
            latitudeFull = "Latitude",
            longitudeFull = "Longitude",
            coordsCopied = "Coordinates copied",
            copyCoords = "Copy coordinates",
            paste = "Paste",
            coordsGuide = "Tip: in Google Maps, long-press a point to show its coordinates; copy them (e.g. 34.633719,50.886485) and paste with the “Paste” button or directly into the latitude field.",
            meterUnit = "Meter",
            kmUnit = "Kilometer",
            gpsAccuracyWarning = "Phone location accuracy is about 10–20 m; a radius under 50 m may not work precisely.",
            checkIntervalTitle = "Precise check interval",
            checkIntervalSub = "Because the radius is under 200 m, your location is checked at this interval.",
            checkIntervalLabel = "Check interval",
            unitSecond = "Seconds",
            unitMinute = "Minutes",
            unitContinuous = "Continuous",
            continuousNote = "In continuous mode, location is checked almost constantly (every 2 seconds); battery use is higher.",
            scheduleEnable = "Enable schedule",
            scheduleEnableSub = "Alert only on the selected days and hours.",
            daySat = "Saturday",
            daySun = "Sunday",
            dayMon = "Monday",
            dayTue = "Tuesday",
            dayWed = "Wednesday",
            dayThu = "Thursday",
            dayFri = "Friday",
            fromHour = "From hour",
            toHour = "To hour",
            scheduleExample = "Example: for work, Saturday to Wednesday, 8 to 18.",
            exitInsideNote = "Inside the zone, the alert stops only manually.",
            exitStopSub = "On exit, the alert stops.",
            exitSequenceSub = "After exit, the repeating pattern plays.",
            exitContinueTitle = "Continue until manual stop",
            exitContinueSub = "The alert continues until you stop it.",
            seqCountLabel = "Repetitions",
            seqDurationLabel = "Each alarm (seconds)",
            seqGapLabel = "Pause between alarms (seconds)",
            seqManualStopNote = "If you stop one manually, later alarms for this zone won't sound.",
            pickCustomSound = "Pick a custom sound from the phone",
            previewCustomSoundCd = "Preview custom sound",
            deleteCustomSoundCd = "Remove custom sound",
            customSoundReplaces = "The custom sound replaces the system sound above.",
            alertBothTitle = "Sound & vibration",
            alertBothSub = "The alert sound plays with vibration.",
            alertSoundOnlyTitle = "Sound only",
            alertSoundOnlySub = "Alert sound only, no vibration.",
            alertVibrateOnlyTitle = "Vibration only",
            alertVibrateOnlySub = "Vibration only, no sound.",
            alertNoneTitle = "Neither",
            alertNoneSub = "Only a notification is shown; no sound or vibration.",
            voiceAnnounceTitle = "Read the zone name",
            voiceAnnounceSub = "On entry, the zone name is read aloud by the phone's voice.",
            approachTitle = "Alert before arrival",
            approachSub = "When you get within a set distance of the zone, an approach notification arrives.",
            approachDistanceHint = "Distance must be a number between 100 and 5000.",
            approachDistanceLabel = "Alert distance (m)",
            stayTitle = "Long-stay reminder",
            staySub = "If you stay in the zone longer than the set time, a reminder notification arrives.",
            stayDurationHint = "Duration must be a number between 5 and 1440.",
            stayMinutesLabel = "Remind after (minutes)",
            smsNumbersLabel = "Phone numbers (one per line)",
            smsMessageLabel = "SMS text",
            smsNote = "On entry, this message is texted to the numbers.",
            silentWindowTitle = "Silent window",
            silentWindowSub = "In this window, entry is reported by notification only (no sound or vibration).",
            errZoneName = "Enter a zone name.",
            errLatitude = "Latitude must be a number between -90 and +90.",
            errLongitude = "Longitude must be a number between -180 and +180.",
            errRadiusPositive = "Radius must be a positive number.",
            errRadiusTooBig = "Radius can't exceed 1000 km.",
            errInvalidNumber = "Enter a valid number.",
            errSeqCount = "Repetitions must be between 1 and 20.",
            errSeqDuration = "Each alarm must be between 3 and 120 seconds.",
            errSeqGap = "The pause between alarms must be between 0 and 600 seconds.",
            errCheckRange = "Allowed range: 5 seconds to 60 minutes.",
            relJustNow = "Just now",
            relMinutesAgo = "%1\$s min ago",
            relHoursAgo = "%1\$s h ago",
            relYesterday = "Yesterday",
            relDaysAgo = "%1\$s d ago",
            langSceneSubtitle = "Choose your language · اختر لغتك",
            langSampleFa = "سلام! خوش آمدید",
            langSampleEn = "Hello! Welcome",
            langSampleAr = "مرحباً! أهلاً بك"
        )

        val AR = UiStringsExt(
            widgetZoneNotSelectedSub = "اضغط لاختيار منطقة",
            widgetPickTitle = "اختر منطقة للودجت",
            widgetNoZones = "لم تنشئ أي مناطق بعد.",
            widgetNoZonesSub = "أنشئ منطقة في التطبيق أولًا، ثم أضف الودجت.",
            zoneFallbackName = "منطقة",
            settingsCd = "الإعدادات",
            createZone = "إنشاء منطقة",
            batteryWarning = "تحسين البطارية مفعّل؛ قد تصل التنبيهات متأخرة أو تتوقف.",
            batteryWarningAction = "تعطيل التحسين",
            backupRestored = "تمت استعادة المناطق من ملف النسخ الاحتياطي",
            sectionPermissions = "الوصول والأذونات",
            sectionAlerts = "التنبيهات والإشعارات",
            sectionAppearance = "المظهر واللغة",
            sectionBackupData = "النسخ الاحتياطي والبيانات",
            sectionHelpAbout = "المساعدة وحول",
            permIntro = "الأذونات التالية لازمة لعمل التنبيهات بشكل صحيح. إذا رُفض أحدها، اطلبه مجددًا من هنا أو افتح إعدادات التطبيق.",
            permPreciseLocation = "الموقع الدقيق",
            permPreciseLocationDesc = "لازم لاكتشاف دخول المنطقة.",
            permBackgroundLocation = "الموقع في الخلفية",
            permBackgroundLocationDesc = "للتنبيهات عندما يكون التطبيق مغلقًا (أندرويد 10 فأعلى).",
            permNotificationsDesc = "لإظهار التنبيهات وزر الإيقاف (أندرويد 13 فأعلى).",
            permSmsTitle = "الرسائل القصيرة",
            permSmsDesc = "لإرسال رسالة عند دخول المنطقة، فقط إلى الأرقام التي تضيفها لكل منطقة.",
            smsPermGranted = "إذن الرسائل مفعّل",
            contactsPermGranted = "إذن جهات الاتصال مفعّل",
            permContactsDesc = "لاختيار أرقام الرسائل من جهات الاتصال عند تحرير منطقة.",
            permBatteryDesc = "مع بطارية غير مقيّدة، لن تتوقف المراقبة في الخلفية.",
            permAdjust = "ضبط",
            permRequest = "طلب",
            permGranted = "ممنوح",
            permNotGranted = "غير ممنوح",
            openAppSettings = "فتح إعدادات التطبيق",
            quietHoursSub = "في هذه الفترة تظهر التنبيهات كإشعارات دون صوت.",
            fromLabel = "من",
            toLabel = "إلى",
            later = "لاحقًا",
            smsBlockedTitle = "إذن الرسائل محظور",
            smsBlockedBody = "يبدو أن أندرويد حظر طلب إذن الرسائل؛ هذا طبيعي للتطبيقات المثبّتة من خارج المتجر ولا يدعو للقلق. لتفعيله:\n١. اضغط «فتح إعدادات التطبيق» أسفل هذه الرسالة.\n٢. في الصفحة التي تفتح، اضغط النقاط الثلاث في الأعلى.\n٣. اختر «السماح بالإعدادات المقيّدة» (Allow restricted settings) وأكّد.\n٤. ثم انتقل إلى «الأذونات» (Permissions)، اختر «الرسائل» (SMS) واضغط «سماح» (Allow).\n٥. عد إلى التطبيق؛ ستتحدّث حالة الإذن تلقائيًا.",
            customThemeTitle = "تخصيص المظهر",
            primaryColor = "اللون الرئيسي",
            preview = "معاينة",
            complementColorAuto = "اللون المكمّل (تلقائي)",
            baseTheme = "المظهر الأساسي",
            lightTheme = "فاتح",
            darkTheme = "داكن",
            limit1 = "يدعم أندرويد 100 سياج جغرافي كحد أقصى في الوقت نفسه.",
            limit2 = "بعد الإيقاف القسري للتطبيق من الإعدادات، يزيل النظام الأسوار الجغرافية ولن تعمل حتى يُفتح التطبيق مجددًا.",
            limit3 = "في وضع توفير البطارية (Doze) أو تحسين البطارية العدواني — خاصة في هواتف سامسونج — قد تصل أحداث الدخول/الخروج متأخرة.",
            limit4 = "تنبيهات الخلفية تتطلب إذن الموقع «دائمًا» (Allow all the time).",
            limit5 = "دقة الاكتشاف تعتمد على GPS والشبكة؛ داخل المباني قد تخطئ.",
            limit6 = "إذا كنت داخل منطقة عند تسجيلها فلن يعمل أي تنبيه؛ الدخول الجديد فقط ينبّه.",
            limit7 = "إيقاف الهاتف أو إلغاء الأذونات يوقف اكتشاف الدخول؛ التطبيق لا يضمن خلاف ذلك.",
            backupAutoNote = "ينسخ التطبيق المناطق تلقائيًا إلى مجلد المستندات في الهاتف؛ إذا حذفت التطبيق وأعدت تثبيته تعود مناطقك تلقائيًا.",
            backupReplaceWarning = "استيراد نسخة يستبدل كل المناطق الحالية بمحتوى الملف.",
            langNameFa = "فارسی",
            langNameEn = "English",
            langNameAr = "العربية",
            langSubEn = "English",
            langSubAr = "Arabic",
            langSubFa = "Persian",
            clearHistory = "مسح السجل",
            clearHistoryConfirm = "حذف كل الأحداث المسجّلة؟",
            clearAction = "احذف",
            historyEmptySub = "تُسجَّل هنا عمليات دخول المناطق والخروج منها.",
            statsEmptySub = "تُبنى الإحصائيات هنا من دخول المناطق والخروج منها.",
            statsWeekEmpty = "لم تُسجَّل أحداث هذا الأسبوع.",
            statsNoEvents = "لا أحداث",
            statsEnterLabel = "دخول",
            statsExitLabel = "خروج",
            rangeTo = "إلى",
            monthJan = "يناير",
            monthFeb = "فبراير",
            monthMar = "مارس",
            monthApr = "أبريل",
            monthMay = "مايو",
            monthJun = "يونيو",
            monthJul = "يوليو",
            monthAug = "أغسطس",
            monthSep = "سبتمبر",
            monthOct = "أكتوبر",
            monthNov = "نوفمبر",
            monthDec = "ديسمبر",
            mapLocationPermNeeded = "إذن الموقع لازم لهذه الميزة",
            mapLocationFailed = "تعذّر الحصول على موقعك",
            pickFromContacts = "الاختيار من جهات الاتصال",
            contactsNoAccess = "لم يُمنح الوصول إلى جهات الاتصال؛ أدخل الرقم يدويًا.",
            loading = "جارٍ التحميل…",
            contactsSearchLabel = "ابحث بالاسم أو الرقم",
            contactsNoneFound = "لم يتم العثور على جهات اتصال",
            done = "تم",
            selectedCd = "محدّد",
            batteryGuideTitle = "دليل استهلاك البطارية",
            batteryGuideIntro = "لكي تعمل تنبيهات دخول المنطقة حتى عندما يكون التطبيق مغلقًا، يجب ألا يوقف أندرويد التطبيق بسبب البطارية. الخطوات التالية لهواتف سامسونج؛ للعلامات الأخرى مسار مشابه.",
            guide1Title = "١. إزالة قيد البطارية",
            guide1Step1 = "انتقل إلى «إعدادات» الهاتف.",
            guide1Step2 = "افتح «التطبيقات» وابحث عن «تنبيه المناطق».",
            guide1Step3 = "ادخل قسم «البطارية».",
            guide1Step4 = "اختر «بدون قيود» (Unrestricted).",
            guide2Title = "٢. الإخراج من التطبيقات النائمة",
            guide2Step1 = "في الإعدادات، انتقل إلى «العناية بالبطارية والجهاز».",
            guide2Step2 = "افتح «البطارية» ثم «حدود الاستخدام في الخلفية».",
            guide2Step3 = "تحقق من «التطبيقات في نوم عميق» (Deep sleeping apps).",
            guide2Step4 = "إذا كان «تنبيه المناطق» هناك، احذفه حتى لا يدخل في نوم عميق.",
            guide3Title = "٣. ملاحظات مهمة",
            guide3Step1 = "تجنّب الإيقاف القسري للتطبيق من الإعدادات؛ بعده لن تعمل الأسوار الجغرافية حتى يُفتح التطبيق مجددًا.",
            guide3Step2 = "اضبط الوصول إلى الموقع على «دائمًا» (Allow all the time).",
            guide3Step3 = "وضع توفير البطارية قد يوصل الأحداث متأخرة.",
            soundAlarm = "صفّارة",
            soundRingtone = "نغمة رنين",
            soundNotification = "إشعار",
            locNotFound = "لم يتم العثور على الموقع؛ تأكد من تشغيل موقع الجهاز.",
            locPermNeeded = "إذن الموقع لازم للحصول على موقعك الحالي.",
            customSoundSelected = "الصوت المحدّد",
            clipboardEmpty = "الحافظة فارغة.",
            pasteFormatError = "يجب أن تكون الصيغة «خط العرض،خط الطول»؛ مثال 35.7,51.4",
            pasteInvalidCoords = "الإحداثيات الملصقة غير صالحة.",
            approachDistanceError = "يجب أن تكون مسافة تنبيه الاقتراب رقمًا بين 100 و5000 متر.",
            stayDurationError = "يجب أن تكون مدة تذكير البقاء رقمًا بين 5 و1440 دقيقة.",
            maxActiveZonesError = "يُدعم 100 منطقة نشطة كحد أقصى.",
            saveError = "حدث خطأ أثناء الحفظ.",
            zoneNameHint = "اسم المنطقة (مثال: البيت)",
            latitudeFull = "خط العرض",
            longitudeFull = "خط الطول",
            coordsCopied = "تم نسخ الإحداثيات",
            copyCoords = "نسخ الإحداثيات",
            paste = "لصق",
            coordsGuide = "تلميح: في خرائط جوجل اضغط مطوّلًا على نقطة لعرض إحداثياتها؛ انسخها (مثال 34.633719,50.886485) والصقها بزر «لصق» أو مباشرة في حقل خط العرض.",
            meterUnit = "متر",
            kmUnit = "كيلومتر",
            gpsAccuracyWarning = "دقة موقع الهاتف حوالي 10–20 مترًا؛ قد لا يعمل نصف قطر أقل من 50 مترًا بدقة.",
            checkIntervalTitle = "فاصل الفحص الدقيق",
            checkIntervalSub = "لأن نصف القطر أقل من 200 متر، يُفحص موقعك بهذا الفاصل الزمني.",
            checkIntervalLabel = "فاصل الفحص",
            unitSecond = "ثوانٍ",
            unitMinute = "دقائق",
            unitContinuous = "مستمر",
            continuousNote = "في الوضع المستمر يُفحص الموقع باستمرار تقريبًا (كل ثانيتين)؛ استهلاك البطارية أعلى.",
            scheduleEnable = "تفعيل الجدول",
            scheduleEnableSub = "التنبيه فقط في الأيام والساعات المحدّدة.",
            daySat = "السبت",
            daySun = "الأحد",
            dayMon = "الاثنين",
            dayTue = "الثلاثاء",
            dayWed = "الأربعاء",
            dayThu = "الخميس",
            dayFri = "الجمعة",
            fromHour = "من الساعة",
            toHour = "إلى الساعة",
            scheduleExample = "مثال: للعمل، من السبت إلى الأربعاء من 8 إلى 18.",
            exitInsideNote = "داخل المنطقة لا يتوقف التنبيه إلا يدويًا.",
            exitStopSub = "عند الخروج يتوقف التنبيه.",
            exitSequenceSub = "بعد الخروج يعمل النمط المتكرر.",
            exitContinueTitle = "الاستمرار حتى الإيقاف اليدوي",
            exitContinueSub = "يستمر التنبيه حتى توقفه يدويًا.",
            seqCountLabel = "عدد المرات",
            seqDurationLabel = "مدة كل تنبيه (ثوانٍ)",
            seqGapLabel = "الفاصل بين التنبيهات (ثوانٍ)",
            seqManualStopNote = "إذا أوقفت أحدها يدويًا فلن تعمل التنبيهات اللاحقة لهذه المنطقة.",
            pickCustomSound = "اختر صوتًا مخصّصًا من الهاتف",
            previewCustomSoundCd = "معاينة الصوت المخصّص",
            deleteCustomSoundCd = "حذف الصوت المخصّص",
            customSoundReplaces = "يحل الصوت المخصّص محل صوت النظام أعلاه.",
            alertBothTitle = "الصوت والاهتزاز",
            alertBothSub = "يعمل صوت التنبيه مع الاهتزاز.",
            alertSoundOnlyTitle = "الصوت فقط",
            alertSoundOnlySub = "صوت التنبيه فقط، دون اهتزاز.",
            alertVibrateOnlyTitle = "الاهتزاز فقط",
            alertVibrateOnlySub = "اهتزاز فقط، دون صوت.",
            alertNoneTitle = "لا شيء",
            alertNoneSub = "يظهر إشعار فقط؛ دون صوت أو اهتزاز.",
            voiceAnnounceTitle = "قراءة اسم المنطقة",
            voiceAnnounceSub = "عند الدخول يُقرأ اسم المنطقة بصوت الهاتف.",
            approachTitle = "التنبيه قبل الوصول",
            approachSub = "عندما تصل إلى مسافة محدّدة من المنطقة يأتي إشعار اقتراب.",
            approachDistanceHint = "يجب أن تكون المسافة رقمًا بين 100 و5000.",
            approachDistanceLabel = "مسافة التنبيه (م)",
            stayTitle = "تذكير الإقامة الطويلة",
            staySub = "إذا بقيت داخل المنطقة أكثر من المدة المحدّدة يأتي إشعار تذكير.",
            stayDurationHint = "يجب أن تكون المدة رقمًا بين 5 و1440.",
            stayMinutesLabel = "التذكير بعد (دقائق)",
            smsNumbersLabel = "أرقام الهاتف (رقم في كل سطر)",
            smsMessageLabel = "نص الرسالة",
            smsNote = "عند الدخول تُرسل هذه الرسالة إلى الأرقام.",
            silentWindowTitle = "فترة الصمت",
            silentWindowSub = "في هذه الفترة يُبلَّغ عن الدخول بإشعار فقط (دون صوت أو اهتزاز).",
            errZoneName = "أدخل اسم المنطقة.",
            errLatitude = "يجب أن يكون خط العرض رقمًا بين -90 و+90.",
            errLongitude = "يجب أن يكون خط الطول رقمًا بين -180 و+180.",
            errRadiusPositive = "يجب أن يكون نصف القطر رقمًا موجبًا.",
            errRadiusTooBig = "لا يمكن أن يتجاوز نصف القطر 1000 كم.",
            errInvalidNumber = "أدخل رقمًا صالحًا.",
            errSeqCount = "يجب أن يكون عدد المرات بين 1 و20.",
            errSeqDuration = "يجب أن تكون مدة كل تنبيه بين 3 و120 ثانية.",
            errSeqGap = "يجب أن يكون الفاصل بين التنبيهات بين 0 و600 ثانية.",
            errCheckRange = "النطاق المسموح: 5 ثوانٍ إلى 60 دقيقة.",
            relJustNow = "قبل لحظات",
            relMinutesAgo = "قبل %1\$s دقيقة",
            relHoursAgo = "قبل %1\$s ساعة",
            relYesterday = "أمس",
            relDaysAgo = "قبل %1\$s يوم",
            langSceneSubtitle = "Choose your language · اختر لغتك",
            langSampleFa = "سلام! خوش آمدید",
            langSampleEn = "Hello! Welcome",
            langSampleAr = "مرحباً! أهلاً بك"
        )
    }
}

/** تبدیل کد زبان ذخیره‌شده به enum. */
fun appLangOf(key: String): AppLang = when (key) {
    "en" -> AppLang.EN
    "ar" -> AppLang.AR
    else -> AppLang.FA
}

/** کد زبان متناظر با enum. */
fun AppLang.key(): String = when (this) {
    AppLang.FA -> "fa"
    AppLang.EN -> "en"
    AppLang.AR -> "ar"
}

/** رشته‌های زبان انتخاب‌شده. */
fun stringsFor(lang: AppLang): UiStrings = when (lang) {
    AppLang.FA -> UiStrings.FA
    AppLang.EN -> UiStrings.EN
    AppLang.AR -> UiStrings.AR
}

/** رشته‌های فعال رابط کاربری؛ در MainActivity بر اساس زبان برنامه تأمین می‌شود. */
val LocalStrings = compositionLocalOf { UiStrings.FA }

/** زبان فعال برنامه برای لایه‌های کامپوزی (از MainActivity تأمین می‌شود). */
val LocalAppLang = compositionLocalOf { AppLang.FA }

// ---------------------------------------------------------------------------
// آینه هم‌گام زبان برای لایه‌های غیرکامپوزی (اعلان‌ها، ویجت‌ها، کاشی تنظیمات
// سریع و سرویس‌ها). این لایه‌ها نباید DataStore/Flow بخوانند؛ پس زبان
// انتخاب‌شده در یک فایل SharedPreferences کوچک هم آینه می‌شود:
// PrefsRepository هنگام هر تغییر زبان آن را می‌نویسد، MainActivity هنگام
// مشاهده زبان فعلی آن را تازه نگه می‌دارد و این‌جا به‌صورت هم‌گام خوانده
// می‌شود. نبود آینه یعنی هنوز زبانی انتخاب نشده است: پیش‌فرض فارسی.
// ---------------------------------------------------------------------------

/** نام فایل آینه زبان. */
const val LANG_MIRROR_PREFS = "lang_mirror"

/** کلید کد زبان در فایل آینه («fa»/«en»/«ar»). */
const val LANG_MIRROR_KEY = "app_lang"

/** نوشتن آینه زبان؛ هنگام ثبت زبان تازه صدا زده می‌شود. */
fun writeLangMirror(context: android.content.Context, langKey: String) {
    try {
        context.getSharedPreferences(LANG_MIRROR_PREFS, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(LANG_MIRROR_KEY, langKey)
            .apply()
    } catch (_: Exception) {
    }
}

/** زبان فعلی برنامه از روی آینه هم‌گام؛ پیش‌فرض فارسی (هم‌خوان با DataStore). */
fun appLangForContext(context: android.content.Context): AppLang {
    val key = try {
        context.getSharedPreferences(LANG_MIRROR_PREFS, android.content.Context.MODE_PRIVATE)
            .getString(LANG_MIRROR_KEY, "fa") ?: "fa"
    } catch (_: Exception) {
        "fa"
    }
    return appLangOf(key)
}

/** رشته‌های زبان فعلی برای لایه‌های غیرکامپوزی. */
fun stringsForContext(context: android.content.Context): UiStrings =
    stringsFor(appLangForContext(context))

private val AR_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

/** تبدیل ارقام لاتین متن به ارقام بومی زبان (فارسی/عربی)؛ در انگلیسی دست‌نخورده. */
fun localizedDigits(text: String, lang: AppLang): String {
    if (lang == AppLang.EN) return text
    val digits = if (lang == AppLang.AR) AR_DIGITS else FA_DIGITS
    val sb = StringBuilder(text.length)
    for (ch in text) {
        sb.append(if (ch in '0'..'9') digits[ch - '0'] else ch)
    }
    return sb.toString()
}

/** زمان نسبی بومی‌سازی‌شده (معادل [ir.khanoomi.geofencealert.domain.TimeFormat.formatRelativeTimeFa] برای هر سه زبان). */
fun formatRelativeTimeLocalized(
    strings: UiStrings,
    lang: AppLang,
    timestamp: Long,
    now: Long = System.currentTimeMillis()
): String {
    if (timestamp <= 0) return ""
    val diff = (now - timestamp).coerceAtLeast(0)
    return when {
        diff < 60_000 -> strings.relJustNow
        diff < 3_600_000 -> String.format(
            strings.relMinutesAgo, localizedDigits((diff / 60_000).toString(), lang)
        )
        diff < 86_400_000 -> String.format(
            strings.relHoursAgo, localizedDigits((diff / 3_600_000).toString(), lang)
        )
        diff < 172_800_000 -> strings.relYesterday
        else -> String.format(
            strings.relDaysAgo, localizedDigits((diff / 86_400_000).toString(), lang)
        )
    }
}

/** نمایش بومی‌سازی‌شده شعاع (متر/کیلومتر با ارقام بومی زبان). */
fun formatRadiusLocalized(meters: Double, strings: UiStrings, lang: AppLang): String {
    if (!meters.isFinite() || meters < 0) {
        return "${localizedDigits("0", lang)} ${strings.meterUnit}"
    }
    return if (meters >= 1000.0 && meters % 1000.0 == 0.0) {
        val km = (meters / 1000.0).toLong().toString()
        "${localizedDigits(km, lang)} ${strings.kmUnit}"
    } else {
        val m = if (meters % 1.0 == 0.0) meters.toLong().toString() else meters.toString()
        "${localizedDigits(m, lang)} ${strings.meterUnit}"
    }
}

/**
 * نگاشت پیام خطای فارسی لایه دامنه به متن بومی‌سازی‌شده در لایه رابط.
 * پیام‌های دامنه (ZoneValidator و دوستان) عمداً فارسی می‌مانند (تست‌های
 * واحد رویشان assert دارند)؛ این تابع بر اساس متن کامل فارسی، معادل
 * زبان انتخاب‌شده را برمی‌گرداند و برای پیام‌های ناشناخته همان متن ورودی
 * را دست‌نخورده رد می‌کند.
 */
fun localizeValidatorError(message: String, strings: UiStrings): String = when (message) {
    UiStrings.FA.errZoneName -> strings.errZoneName
    UiStrings.FA.errLatitude -> strings.errLatitude
    UiStrings.FA.errLongitude -> strings.errLongitude
    UiStrings.FA.errRadiusPositive -> strings.errRadiusPositive
    UiStrings.FA.errRadiusTooBig -> strings.errRadiusTooBig
    UiStrings.FA.errInvalidNumber -> strings.errInvalidNumber
    UiStrings.FA.errSeqCount -> strings.errSeqCount
    UiStrings.FA.errSeqDuration -> strings.errSeqDuration
    UiStrings.FA.errSeqGap -> strings.errSeqGap
    UiStrings.FA.errCheckRange -> strings.errCheckRange
    else -> message
}
