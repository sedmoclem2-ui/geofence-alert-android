package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryGuideScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("راهنمای مصرف باتری") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "برای این‌که هشدار ورود به محدوده حتی وقتی برنامه باز نیست کار کند، " +
                        "اندروید نباید برنامه را به‌خاطر مصرف باتری متوقف کند. " +
                        "مراحل زیر برای گوشی‌های سامسونگ است؛ در برندهای دیگر مسیر مشابهی دارد.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                GuideCard(
                    title = "۱. برداشتن محدودیت باتری",
                    steps = listOf(
                        "به «تنظیمات» گوشی بروید.",
                        "«برنامه‌ها» را باز کنید و «محدوده هشدار» را پیدا کنید.",
                        "وارد بخش «باتری» شوید.",
                        "گزینه «بدون محدودیت» (Unrestricted) را انتخاب کنید."
                    )
                )
            }
            item {
                GuideCard(
                    title = "۲. خارج کردن از برنامه‌های در خواب",
                    steps = listOf(
                        "در تنظیمات، به «مراقبت از باتری و دستگاه» بروید.",
                        "«باتری» و سپس «محدودیت‌های استفاده پس‌زمینه» را باز کنید.",
                        "«برنامه‌های در خواب عمیق» (Deep sleeping apps) را بررسی کنید.",
                        "اگر «محدوده هشدار» آن‌جاست، آن را حذف کنید تا در خواب عمیق نرود."
                    )
                )
            }
            item {
                GuideCard(
                    title = "۳. نکات مهم",
                    steps = listOf(
                        "از «Force Stop» (توقف اجباری) برنامه از تنظیمات خودداری کنید؛ " +
                            "پس از آن ژئوفنس‌ها تا باز شدن دوباره برنامه کار نمی‌کنند.",
                        "دسترسی مکان را روی «همیشه» (Allow all the time) بگذارید.",
                        "حالت «صرفه‌جویی در مصرف باتری» ممکن است رویدادها را با تأخیر برساند."
                    )
                )
            }
        }
    }
}

@Composable
private fun GuideCard(title: String, steps: List<String>) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            steps.forEach { step ->
                Text("• $step", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
