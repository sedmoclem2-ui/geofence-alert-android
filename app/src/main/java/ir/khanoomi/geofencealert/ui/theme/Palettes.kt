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
    );

    companion object {
        fun fromKey(key: String): AppPalette =
            entries.firstOrNull { it.key == key } ?: TEAL
    }
}
