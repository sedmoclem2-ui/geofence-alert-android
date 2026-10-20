package ir.khanoomi.geofencealert.audio

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * اعلام صوتی نام محدوده با موتور متن‌به‌گفتار خودِ گوشی (کامپیوتری، بدون
 * هوش مصنوعی و بدون اینترنت). زبان گفتار از زبان انتخاب‌شده در تنظیمات
 * برنامه گرفته می‌شود. اگر داده صوتی زبان نصب نباشد، موتور گوشی ممکن است
 * سکوت کند — این هیچ‌وقت باعث کرش نمی‌شود.
 */
object VoiceAnnouncer {
    private const val TAG = "VoiceAnnouncer"
    private const val UTTERANCE_ID = "zone_entry"

    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    @Volatile
    private var pending: Pair<String, Locale>? = null

    private fun localeFor(lang: String): Locale = when (lang) {
        "en" -> Locale.US
        "ar" -> Locale("ar", "SA")
        else -> Locale("fa", "IR")
    }

    private fun textFor(lang: String, zoneName: String): String = when (lang) {
        "en" -> "You have entered the zone $zoneName"
        "ar" -> "دخلت منطقة $zoneName"
        else -> "وارد محدودهٔ $zoneName شدید"
    }

    /**
     * خواندن نام محدوده با صدای گوشی. [lang] همان زبان انتخاب‌شده در
     * تنظیمات برنامه («fa»/«en»/«ar») است. کل مسیر در try/catch است.
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
            if (engine == null) {
                val appContext = context.applicationContext
                tts = TextToSpeech(appContext) { status ->
                    try {
                        if (status == TextToSpeech.SUCCESS) {
                            ready = true
                            pending?.let { (t, l) ->
                                tts?.let { speak(it, t, l) }
                            }
                            pending = null
                        } else {
                            Log.w(TAG, "TTS init failed with status $status")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "TTS callback failed", e)
                    }
                }
            }
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
