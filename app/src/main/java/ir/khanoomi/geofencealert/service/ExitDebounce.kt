package ir.khanoomi.geofencealert.service

/**
 * تصمیم‌گیری خالص دیبانس خروج دقیق (بدون وابستگی به اندروید تا در تست
 * واحد JVM قابل آزمون بماند). برای جلوگیری از خروج کاذب بر اثر یک فیکس
 * بد GPS در پس‌زمینه/صفحه قفل، خروج واقعی نیازمند [REQUIRED_OUTSIDE_STREAK]
 * مشاهده پیاپی در بیرون محدوده است.
 */
object ExitDebounce {

    const val REQUIRED_OUTSIDE_STREAK = 2

    enum class Action {
        NONE,
        ENTER,
        EXIT
    }

    data class Result(
        val nextInside: Boolean,
        val nextOutsideStreak: Int,
        val action: Action
    )

    fun evaluate(prevInside: Boolean?, isInside: Boolean, outsideStreak: Int): Result {
        if (prevInside == null) {
            return Result(
                nextInside = isInside,
                nextOutsideStreak = 0,
                action = Action.NONE
            )
        }
        if (isInside) {
            return Result(
                nextInside = true,
                nextOutsideStreak = 0,
                action = if (!prevInside) Action.ENTER else Action.NONE
            )
        }
        if (!prevInside) {
            return Result(
                nextInside = false,
                nextOutsideStreak = 0,
                action = Action.NONE
            )
        }
        val nextStreak = outsideStreak + 1
        return if (nextStreak >= REQUIRED_OUTSIDE_STREAK) {
            Result(
                nextInside = false,
                nextOutsideStreak = 0,
                action = Action.EXIT
            )
        } else {
            Result(
                nextInside = true,
                nextOutsideStreak = nextStreak,
                action = Action.NONE
            )
        }
    }
}
