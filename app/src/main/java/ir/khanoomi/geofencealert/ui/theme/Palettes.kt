package ir.khanoomi.geofencealert.ui.theme

import androidx.compose.ui.graphics.Color

/** زیرمجموعه رنگ‌های یک پالت که تم روشن/تیره از آن ساخته می‌شود. */
data class PaletteColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val tertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color
)

/** کلید تم شخصی‌سازی‌شده توسط کاربر (مقدار theme_palette). */
const val CUSTOM_THEME_KEY = "custom"

/** پالت‌های رنگی قابل انتخاب توسط کاربر. */
enum class AppPalette(
    val key: String,
    val titleFa: String,
    val light: PaletteColors,
    val dark: PaletteColors
) {
    TEAL(
        key = "teal",
        titleFa = "سبزآبی",
        light = PaletteColors(
            primary = TealPrimary,
            onPrimary = TealOnPrimary,
            primaryContainer = TealContainer,
            onPrimaryContainer = TealOnContainer,
            secondary = AmberSecondary,
            tertiary = AmberSecondary,
            background = PaperBackground,
            surface = Color.White,
            surfaceVariant = SandSurfaceVariant
        ),
        dark = PaletteColors(
            primary = TealPrimaryDark,
            onPrimary = TealOnPrimaryDark,
            primaryContainer = TealContainerDark,
            onPrimaryContainer = TealContainer,
            secondary = AmberSecondary,
            tertiary = AmberSecondary,
            background = NightBackground,
            surface = NightSurface,
            surfaceVariant = NightSurfaceVariant
        )
    ),
    INDIGO(
        key = "indigo",
        titleFa = "نیلی",
        light = PaletteColors(
            primary = Color(0xFF4C5FD5),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDE2FF),
            onPrimaryContainer = Color(0xFF1A2456),
            secondary = AmberSecondary,
            tertiary = Color(0xFF7C8CF8),
            background = PaperBackground,
            surface = Color.White,
            surfaceVariant = SandSurfaceVariant
        ),
        dark = PaletteColors(
            primary = Color(0xFF9AA5FF),
            onPrimary = Color(0xFF16204A),
            primaryContainer = Color(0xFF2E3A7A),
            onPrimaryContainer = Color(0xFFDDE2FF),
            secondary = AmberSecondary,
            tertiary = Color(0xFF9AA5FF),
            background = Color(0xFF111527),
            surface = Color(0xFF181E38),
            surfaceVariant = Color(0xFF232A4D)
        )
    ),
    ROSE(
        key = "rose",
        titleFa = "رز",
        light = PaletteColors(
            primary = Color(0xFFC2436B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD9E2),
            onPrimaryContainer = Color(0xFF5C1230),
            secondary = AmberSecondary,
            tertiary = Color(0xFFF27D9D),
            background = PaperBackground,
            surface = Color.White,
            surfaceVariant = SandSurfaceVariant
        ),
        dark = PaletteColors(
            primary = Color(0xFFFF8FAB),
            onPrimary = Color(0xFF4A0E22),
            primaryContainer = Color(0xFF7A2E4A),
            onPrimaryContainer = Color(0xFFFFD9E2),
            secondary = AmberSecondary,
            tertiary = Color(0xFFFF8FAB),
            background = Color(0xFF1F1216),
            surface = Color(0xFF2B171D),
            surfaceVariant = Color(0xFF3A2029)
        )
    ),
    GALAXY(
        key = "galaxy",
        titleFa = "بنفش کهکشانی",
        light = PaletteColors(
            primary = Color(0xFF7B2FBE),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE9D5FF),
            onPrimaryContainer = Color(0xFF2A0A45),
            secondary = Color(0xFF00A89A),
            tertiary = Color(0xFFB794FF),
            background = Color(0xFFFBF8FF),
            surface = Color.White,
            surfaceVariant = Color(0xFFF3EDFA)
        ),
        dark = PaletteColors(
            primary = Color(0xFFC77DFF),
            onPrimary = Color(0xFF2A0A45),
            primaryContainer = Color(0xFF4A1D6E),
            onPrimaryContainer = Color(0xFFE9D5FF),
            secondary = Color(0xFF00D4C8),
            tertiary = Color(0xFFC77DFF),
            background = Color(0xFF120A1F),
            surface = Color(0xFF1B1030),
            surfaceVariant = Color(0xFF2A1B45)
        )
    ),
    SUNSET(
        key = "sunset",
        titleFa = "غروب نارنجی",
        light = PaletteColors(
            primary = Color(0xFFE0641F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE0C7),
            onPrimaryContainer = Color(0xFF4A1F00),
            secondary = Color(0xFFB23A8E),
            tertiary = Color(0xFFFFB300),
            background = Color(0xFFFFFAF3),
            surface = Color.White,
            surfaceVariant = Color(0xFFF7EDE0)
        ),
        dark = PaletteColors(
            primary = Color(0xFFFF9E57),
            onPrimary = Color(0xFF3A1A00),
            primaryContainer = Color(0xFF7A3A0E),
            onPrimaryContainer = Color(0xFFFFE0C7),
            secondary = Color(0xFFF27DAE),
            tertiary = Color(0xFFFFB300),
            background = Color(0xFF201009),
            surface = Color(0xFF2E1810),
            surfaceVariant = Color(0xFF422418)
        )
    ),
    GOLD(
        key = "gold",
        titleFa = "طلایی سلطنتی",
        light = PaletteColors(
            primary = Color(0xFF9A7B1E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFF7E8B8),
            onPrimaryContainer = Color(0xFF3A2D05),
            secondary = Color(0xFF0E6B5C),
            tertiary = Color(0xFFD4AF37),
            background = Color(0xFFFCFAF2),
            surface = Color.White,
            surfaceVariant = Color(0xFFF5EEDC)
        ),
        dark = PaletteColors(
            primary = Color(0xFFD4AF37),
            onPrimary = Color(0xFF2A2100),
            primaryContainer = Color(0xFF5C4A0E),
            onPrimaryContainer = Color(0xFFF7E8B8),
            secondary = Color(0xFF7FD6B5),
            tertiary = Color(0xFFD4AF37),
            background = Color(0xFF1A1408),
            surface = Color(0xFF241C0C),
            surfaceVariant = Color(0xFF352A12)
        )
    ),
    OCEAN(
        key = "ocean",
        titleFa = "اقیانوس عمیق",
        light = PaletteColors(
            primary = Color(0xFF0B5FA5),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCDE9FF),
            onPrimaryContainer = Color(0xFF062E52),
            secondary = Color(0xFF00B4A2),
            tertiary = Color(0xFF66C2FF),
            background = Color(0xFFF4F9FD),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8F1F8)
        ),
        dark = PaletteColors(
            primary = Color(0xFF66B8F0),
            onPrimary = Color(0xFF06203A),
            primaryContainer = Color(0xFF144A7A),
            onPrimaryContainer = Color(0xFFCDE9FF),
            secondary = Color(0xFF4FD8C4),
            tertiary = Color(0xFF66B8F0),
            background = Color(0xFF08131F),
            surface = Color(0xFF0E1D2E),
            surfaceVariant = Color(0xFF16293F)
        )
    ),
    FOREST(
        key = "forest",
        titleFa = "جنگل سبز",
        light = PaletteColors(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCDEACD),
            onPrimaryContainer = Color(0xFF0E3A12),
            secondary = AmberSecondary,
            tertiary = Color(0xFF81C784),
            background = Color(0xFFF6FAF4),
            surface = Color.White,
            surfaceVariant = Color(0xFFEAF2E6)
        ),
        dark = PaletteColors(
            primary = Color(0xFF81C784),
            onPrimary = Color(0xFF0E2A10),
            primaryContainer = Color(0xFF1E4A24),
            onPrimaryContainer = Color(0xFFCDEACD),
            secondary = AmberSecondary,
            tertiary = Color(0xFF81C784),
            background = Color(0xFF0C160D),
            surface = Color(0xFF122114),
            surfaceVariant = Color(0xFF1B3120)
        )
    ),
    CHERRY(
        key = "cherry",
        titleFa = "چری",
        light = PaletteColors(
            primary = Color(0xFFD81B60),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFD3E0),
            onPrimaryContainer = Color(0xFF4A0A20),
            secondary = Color(0xFF7B2FBE),
            tertiary = Color(0xFFFF8FB0),
            background = Color(0xFFFEF6F8),
            surface = Color.White,
            surfaceVariant = Color(0xFFF8EAF0)
        ),
        dark = PaletteColors(
            primary = Color(0xFFFF7DA8),
            onPrimary = Color(0xFF40081C),
            primaryContainer = Color(0xFF7A1E3E),
            onPrimaryContainer = Color(0xFFFFD3E0),
            secondary = Color(0xFFC77DFF),
            tertiary = Color(0xFFFF7DA8),
            background = Color(0xFF1E0D13),
            surface = Color(0xFF2C131B),
            surfaceVariant = Color(0xFF3E1C26)
        )
    ),
    MIDNIGHT(
        key = "midnight",
        titleFa = "شب نیمه",
        light = PaletteColors(
            primary = Color(0xFF37474F),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD3E0E6),
            onPrimaryContainer = Color(0xFF10222B),
            secondary = Color(0xFFFFB300),
            tertiary = Color(0xFF90A4AE),
            background = Color(0xFFF7F9FA),
            surface = Color.White,
            surfaceVariant = Color(0xFFECEFF1)
        ),
        dark = PaletteColors(
            primary = Color(0xFF8AB4F8),
            onPrimary = Color(0xFF0A1A2E),
            primaryContainer = Color(0xFF1E3A5C),
            onPrimaryContainer = Color(0xFFD3E6FF),
            secondary = Color(0xFFFFB300),
            tertiary = Color(0xFF8AB4F8),
            background = Color(0xFF050B14),
            surface = Color(0xFF0A1420),
            surfaceVariant = Color(0xFF12202F)
        )
    );

    companion object {
        fun fromKey(key: String): AppPalette =
            entries.firstOrNull { it.key == key } ?: TEAL
    }
}

/** ترکیب خطی دو رنگ (ratio = سهم رنگ دوم). */
private fun mixColor(a: Color, b: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = a.red * (1 - r) + b.red * r,
        green = a.green * (1 - r) + b.green * r,
        blue = a.blue * (1 - r) + b.blue * r,
        alpha = 1f
    )
}

/**
 * ساخت پالت شخصی‌سازی‌شده از یک رنگ اصلی (مشتق‌سازی ساده‌ی تونال):
 * روشن → پس‌زمینه‌های نزدیک به سفید؛ تیره → سطوح نزدیک به مشکی.
 */
fun buildCustomPalette(primaryArgb: Long, dark: Boolean): PaletteColors {
    val primary = Color(primaryArgb.toULong())
    val white = Color.White
    val black = Color.Black
    return if (!dark) {
        PaletteColors(
            primary = primary,
            onPrimary = white,
            primaryContainer = mixColor(primary, white, 0.82f),
            onPrimaryContainer = mixColor(primary, black, 0.55f),
            secondary = mixColor(primary, white, 0.25f),
            tertiary = mixColor(primary, white, 0.45f),
            background = mixColor(primary, white, 0.965f),
            surface = white,
            surfaceVariant = mixColor(primary, white, 0.92f)
        )
    } else {
        val lightPrimary = mixColor(primary, white, 0.35f)
        PaletteColors(
            primary = lightPrimary,
            onPrimary = mixColor(primary, black, 0.7f),
            primaryContainer = mixColor(primary, black, 0.45f),
            onPrimaryContainer = mixColor(primary, white, 0.85f),
            secondary = mixColor(primary, white, 0.4f),
            tertiary = lightPrimary,
            background = mixColor(primary, black, 0.92f),
            surface = mixColor(primary, black, 0.85f),
            surfaceVariant = mixColor(primary, black, 0.72f)
        )
    }
}
