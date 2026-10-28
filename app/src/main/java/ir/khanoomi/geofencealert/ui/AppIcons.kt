package ir.khanoomi.geofencealert.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * آیکن‌های سفارشی برنامه که در material-icons-core 1.7.0 نیستند.
 *
 * هر بردار دقیقاً از روی رشته مسیر (path) رسمی آیکن‌های Material رونویسی
 * شده است؛ رشته اصلی برای ممیزی در KDoc بالای هر بردار نگه داشته شده و
 * نگاشت SVG به DSL چنین است: M→moveTo، m→moveToRelative، H→horizontalLineTo،
 * h→horizontalLineToRelative، V→verticalLineTo، v→verticalLineToRelative،
 * C→curveTo، c→curveToRelative، S→reflectiveCurveTo، s→reflectiveCurveToRelative،
 * z→close. اندازه پیش‌فرض 24.dp و ویوپورت 24×24 است.
 */
object AppIcons {

    /**
     * آیکن «کپی» (ContentCopy رسمی Material).
     *
     * رشته مسیر اصلی:
     * "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"
     *
     * نکته ممیزی: پس از هر close نقطه جاری به ابتدای همان زیرمسیر برمی‌گردد،
     * پس moveToRelative(3f, 4f) برابر (19, 5) و moveToRelative(0f, 16f)
     * برابر (19, 21) است.
     */
    val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 1f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                horizontalLineToRelative(2f)
                verticalLineTo(3f)
                horizontalLineToRelative(12f)
                verticalLineTo(1f)
                close()
                moveToRelative(3f, 4f)
                horizontalLineTo(8f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(14f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(11f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(7f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveToRelative(0f, 16f)
                horizontalLineTo(8f)
                verticalLineTo(7f)
                horizontalLineToRelative(11f)
                verticalLineToRelative(14f)
                close()
            }
        }.build()
    }

    /**
     * آیکن «الصاق» (ContentPaste رسمی Material).
     *
     * رشته مسیر اصلی:
     * "M19 2h-4.18C14.4.84 13.3 0 12 0c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm7 18H5V4h2v3h10V4h2v16z"
     *
     * نکته ممیزی: «C14.4.84» یعنی curveTo مطلق با نقاط (14.4, 0.84) و
     * (13.3, 0) و مقصد (12, 0)؛ و پس از close نقطه جاری (12, 2) است، پس
     * moveToRelative(-7f, 0f) برابر (12, 2) و moveToRelative(7f, 18f)
     * برابر (19, 20) است.
     */
    val Paste: ImageVector by lazy {
        ImageVector.Builder(
            name = "Paste",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 2f)
                horizontalLineToRelative(-4.18f)
                curveTo(14.4f, 0.84f, 13.3f, 0f, 12f, 0f)
                curveToRelative(-1.3f, 0f, -2.4f, 0.84f, -2.82f, 2f)
                horizontalLineTo(5f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(16f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveToRelative(-7f, 0f)
                curveToRelative(0.55f, 0f, 1f, 0.45f, 1f, 1f)
                reflectiveCurveToRelative(-0.45f, 1f, -1f, 1f)
                reflectiveCurveToRelative(-1f, -0.45f, -1f, -1f)
                reflectiveCurveToRelative(0.45f, -1f, 1f, -1f)
                close()
                moveToRelative(7f, 18f)
                horizontalLineTo(5f)
                verticalLineTo(4f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(3f)
                horizontalLineToRelative(10f)
                verticalLineTo(4f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(16f)
                close()
            }
        }.build()
    }
}
