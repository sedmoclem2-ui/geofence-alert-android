package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ir.khanoomi.geofencealert.domain.RadiusUnit

/**
 * قالب آماده محدوده: نام بومی‌سازی‌شده، آیکن، و مقادیری که با انتخاب قالب
 * روی فیلدهای ویومدل ویرایش محدوده نوشته می‌شوند.
 *
 * روزهای زمان‌بندی با قرارداد سراسری برنامه‌اند: ۱=شنبه … ۷=جمعه، پس
 * «شنبه تا چهارشنبه» برابر "1,2,3,4,5" است. ساعت‌ها به دقیقه از نیمه‌شب‌اند.
 * هر قالب همه فیلدهای شعاع، بازه بی‌صدا و زمان‌بندی را صراحتاً تعیین می‌کند
 * (قالب بدون زمان‌بندی/بی‌صدا، آن‌ها را خاموش و به پیش‌فرض برمی‌گرداند)، تا
 * انتخاب یک قالب پس از قالب دیگر، مقادیر کهنه قالب قبلی را باقی نگذارد.
 */
data class ZoneTemplate(
    val name: String,
    val icon: ImageVector,
    val radiusMeters: Int,
    val silentEnabled: Boolean,
    val silentStartMin: Int,
    val silentEndMin: Int,
    val scheduleEnabled: Boolean,
    val scheduleDays: String,
    val scheduleStartMin: Int,
    val scheduleEndMin: Int
) {
    /**
     * اعمال قالب روی ویومدل ویرایش محدوده (فقط صفحه محدوده تازه؛ فراخواننده
     * مسئول این قید است). نام فقط وقتی نوشته می‌شود که فیلد نام خالی باشد؛
     * شعاع، بازه بی‌صدا و زمان‌بندی همیشه بازنویسی می‌شوند. منطق ذخیره و
     * اعتبارسنجی دست نخورده می‌ماند.
     */
    fun applyTo(vm: ZoneEditViewModel) {
        if (vm.name.isBlank()) vm.name = name
        vm.unit = RadiusUnit.METER
        vm.radiusText = radiusMeters.toString()
        vm.silentEnabled = silentEnabled
        vm.silentStartMin = silentStartMin
        vm.silentEndMin = silentEndMin
        vm.scheduleEnabled = scheduleEnabled
        vm.scheduleDays = scheduleDays
        vm.scheduleStartMin = scheduleStartMin
        vm.scheduleEndMin = scheduleEndMin
    }
}

/** چهار قالب آماده با نام بومی‌سازی‌شده از رشته‌های جاری برنامه. */
fun zoneTemplates(strings: UiStrings): List<ZoneTemplate> = listOf(
    ZoneTemplate(
        name = strings.templateHome,
        icon = Icons.Filled.Home,
        radiusMeters = 200,
        silentEnabled = true,
        silentStartMin = 23 * 60,
        silentEndMin = 7 * 60,
        scheduleEnabled = false,
        scheduleDays = "1,2,3,4,5,6,7",
        scheduleStartMin = 0,
        scheduleEndMin = 1439
    ),
    ZoneTemplate(
        name = strings.templateWork,
        icon = Icons.Filled.Build,
        radiusMeters = 300,
        silentEnabled = false,
        silentStartMin = 1320,
        silentEndMin = 360,
        scheduleEnabled = true,
        scheduleDays = "1,2,3,4,5",
        scheduleStartMin = 8 * 60,
        scheduleEndMin = 17 * 60
    ),
    ZoneTemplate(
        name = strings.templateGym,
        icon = Icons.Filled.Favorite,
        radiusMeters = 200,
        silentEnabled = false,
        silentStartMin = 1320,
        silentEndMin = 360,
        scheduleEnabled = false,
        scheduleDays = "1,2,3,4,5,6,7",
        scheduleStartMin = 0,
        scheduleEndMin = 1439
    ),
    ZoneTemplate(
        name = strings.templateSchool,
        icon = Icons.Filled.Info,
        radiusMeters = 300,
        silentEnabled = false,
        silentStartMin = 1320,
        silentEndMin = 360,
        scheduleEnabled = true,
        scheduleDays = "1,2,3,4,5",
        scheduleStartMin = 7 * 60 + 30,
        scheduleEndMin = 14 * 60
    )
)

/**
 * ردیف قالب‌ها برای بالای تب ۰ صفحه محدوده تازه: کارت هم‌شکل کارت‌های
 * بخش فرم (شکل large) با چیپ‌های FilterChip هم‌شکل چیپ‌های آماده شعاع —
 * هر چیپ آیکن + نام قالب را نشان می‌دهد و با لمس، قالب را اعمال می‌کند.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ZoneTemplateRow(vm: ZoneEditViewModel, strings: UiStrings) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            zoneTemplates(strings).forEach { template ->
                FilterChip(
                    selected = false,
                    onClick = { template.applyTo(vm) },
                    label = { Text(template.name) },
                    leadingIcon = {
                        Icon(
                            template.icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}
