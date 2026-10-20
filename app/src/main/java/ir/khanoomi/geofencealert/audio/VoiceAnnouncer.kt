package ir.khanoomi.geofencealert.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * اعلام صوتی نام محدوده با موتور متن‌به‌گفتار خودِ گوشی (کامپیوتری، بدون
 * هوش مصنوعی و بدون اینترنت). زبان گفتار از زبان انتخاب‌شده در تنظیمات
 * برنامه گرفته می‌شود. اگر داده صوتی زبان نصب نباشد، اعلام در سکوت رد
 * می‌شود — این هیچ‌وقت باعث کرش نمی‌شود.
 *
 * از نسخه ۲.۳.۰ موتور در شروع برنامه با [init] گرم می‌شود و مسیر اصلی
 * [announceNameTwice] است: نام محدوده «دو بار» خوانده می‌شود و پایان
 * خواندن دوم با callback خبر داده می‌شود تا صدای هشدار «بعد» از گفتار
 * شروع شود ([ir.khanoomi.geofencealert.alert.AlertController] ترتیب را
 * مدیریت می‌کند و یک مهلت سخت هم دارد تا صدا هیچ‌وقت گروگان گفتار نماند).
 */
object VoiceAnnouncer {
    private const val TAG = "VoiceAnnouncer"
    private const val UTTERANCE_ID = "zone_entry"
    private const val UTTERANCE_ID_2 = "zone_entry_2"

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    @Volatile
    private var pending: Pair<String, Locale>? = null

    /** درخواست «خواندن دو باره» که منتظر آماده شدن موتور است. */
    private class TwiceRequest(
        val text: String,
        val locale: Locale,
        val onFinished: () -> Unit
    )

    @Volatile
    private var pendingTwice: TwiceRequest? = null

    private fun localeFor(lang: String): Locale = when (lang) {
        "en" -> Locale.US
        "ar" -> Locale("ar")
        else -> Locale("fa", "IR")
    }

    private fun textFor(lang: String, zoneName: String): String = when (lang) {
        "en" -> "You have entered the zone $zoneName"
        "ar" -> "دخلت منطقة $zoneName"
        else -> "وارد محدودهٔ $zoneName شدید"
    }

    /**
     * گرم کردن زودهنگام موتور گفتار (از GeofenceAlertApp.onCreate صدا زده
     * می‌شود) تا هنگام ورود به محدوده، خواندن نام بدون تأخیرِ راه‌اندازی
     * موتور شروع شود. ساخت موتور ناهمگام است و نتیجه‌اش در callback می‌آید.
     */
    fun init(context: Context) {
        ensureEngine(context)
    }

    /** ساخت یک‌باره موتور (اگر هنوز ساخته نشده)؛ امن برای فراخوانی تکراری. */
    private fun ensureEngine(context: Context) {
        if (tts != null) return
        synchronized(this) {
            if (tts != null) return
            try {
                val appContext = context.applicationContext
                tts = TextToSpeech(appContext) { status ->
                    try {
                        if (status == TextToSpeech.SUCCESS) {
                            ready = true
                            pending?.let { (t, l) ->
                                tts?.let { speak(it, t, l) }
                            }
                            pending = null
                            pendingTwice?.let { req ->
                                pendingTwice = null
                                tts?.let { speakTwice(it, req) }
                            }
                        } else {
                            Log.w(TAG, "TTS init failed with status $status")
                            // موتوری نیست که بخواند؛ منتظر نمان — صدای هشدار
                            // باید راه بیفتد (مهلت AlertController هم هست).
                            pendingTwice?.let { req ->
                                pendingTwice = null
                                fireFinished(req.onFinished)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "TTS callback failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS engine creation failed", e)
            }
        }
    }

    /**
     * خواندن نام محدوده «دو بار» پشت سر هم و خبر دادن پایان آن با
     * [onFinished] (پایان یا خطای خواندن دوم، یا نبود داده صوتی زبان).
     * [lang] کد زبان فعلی برنامه است («fa»/«en»/«ar»). اگر موتور هنوز
     * آماده نباشد، درخواست نگه داشته می‌شود تا callback آماده‌سازی آن را
     * اجرا کند؛ فراخواننده (AlertController) علاوه بر این یک مهلت سخت
     * ~۵ ثانیه‌ای دارد، پس صدای هشدار در هیچ حالتی معطل نمی‌ماند.
     */
    fun announceNameTwice(context: Context, zoneName: String, lang: String, onFinished: () -> Unit) {
        try {
            val request = TwiceRequest(textFor(lang, zoneName), localeFor(lang), onFinished)
            val engine = tts
            if (engine != null && ready) {
                speakTwice(engine, request)
                return
            }
            pendingTwice = request
            ensureEngine(context)
        } catch (e: Exception) {
            Log.e(TAG, "announceNameTwice failed", e)
            fireFinished(onFinished)
        }
    }

    private fun speakTwice(engine: TextToSpeech, request: TwiceRequest) {
        try {
            val result = engine.setLanguage(request.locale)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Log.w(TAG, "TTS language unavailable: ${request.locale}")
                fireFinished(request.onFinished)
                return
            }
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                }

                override fun onDone(utteranceId: String?) {
                    if (utteranceId == UTTERANCE_ID_2) {
                        fireFinished(request.onFinished)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    // خطای خواندن اول یا دوم: دیگر منتظر گفتار نمان.
                    if (utteranceId == UTTERANCE_ID || utteranceId == UTTERANCE_ID_2) {
                        fireFinished(request.onFinished)
                    }
                }
            })
            engine.speak(request.text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            engine.speak(request.text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID_2)
            Log.i(TAG, "Speaking entry announcement twice")
        } catch (e: Exception) {
            Log.e(TAG, "speakTwice failed", e)
            fireFinished(request.onFinished)
        }
    }

    private fun fireFinished(onFinished: () -> Unit) {
        try {
            onFinished()
        } catch (e: Exception) {
            Log.e(TAG, "onFinished callback failed", e)
        }
    }

    /**
     * خواندن یک‌باره نام محدوده (مسیر قدیمی، برای سازگاری نگه داشته شده).
     * [lang] همان زبان انتخاب‌شده در تنظیمات برنامه است.
     */
    fun speakEntry(context: Context, zoneName: String, lang: String) {
        try {
            val locale = localeFor(lang)
            val text = textFor(lang, zoneName)
            val engine = tts
            if (engine != null && ready) {
                speak(engine, text, locale)
                return
            }
            pending = text to locale
            ensureEngine(context)
        } catch (e: Exception) {
            Log.e(TAG, "speakEntry failed", e)
        }
    }

    private fun speak(engine: TextToSpeech, text: String, locale: Locale) {
        try {
            engine.language = locale
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
            Log.i(TAG, "Speaking entry announcement")
        } catch (e: Exception) {
            Log.e(TAG, "speak failed", e)
        }
    }
}
