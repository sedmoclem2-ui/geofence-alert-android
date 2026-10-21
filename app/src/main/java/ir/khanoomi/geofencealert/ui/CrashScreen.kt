package ir.khanoomi.geofencealert.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * صفحه گزارش خطا: وقتی اجرای قبلی برنامه با خطا بسته شده باشد
 * (CrashGuard گزارشش را ذخیره کرده)، به‌جای رابط اصلی فقط همین صفحه
 * ساده نشان داده می‌شود تا کاربر گزارش را کپی کند و بفرستد.
 *
 * عمداً کاملاً مستقل و کم‌وابستگی است: هیچ رشته‌ای از UiStrings/LocalStrings
 * نمی‌خواند (خودِ لایه رشته‌ها مظنون خطاست)، انیمیشن و Flow ندارد و فقط
 * متن‌های فارسی ثابت و رنگ‌های MaterialTheme را به کار می‌برد.
 */
@Composable
fun CrashScreen(trace: String, onEnterApp: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    // متن‌های این صفحه فارسی‌اند؛ جهت چیدمان را مستقل از زبان برنامه
    // راست‌به‌چپ می‌کنیم تا همیشه درست دیده شود.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "برنامه دفعه قبل با خطا بسته شد",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "برای این‌که دقیقاً بفهمم مشکل کجاست، گزارش خطا را با دکمه زیر کپی کن و برای خانومی بفرست؛ یا از همین صفحه اسکرین‌شات بگیر.",
                style = MaterialTheme.typography.bodyMedium
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // خودِ پشته خطا ذاتاً چپ‌به‌راست است.
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Ltr
                ) {
                    SelectionContainer {
                        Text(
                            text = trace,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
            Button(
                onClick = {
                    clipboard.setText(AnnotatedString(trace))
                    copied = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (copied) "کپی شد" else "کپی گزارش خطا")
            }
            OutlinedButton(
                onClick = onEnterApp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حذف گزارش و ورود به برنامه")
            }
        }
    }
}
