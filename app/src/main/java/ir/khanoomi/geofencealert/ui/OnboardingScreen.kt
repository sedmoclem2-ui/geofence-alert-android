package ir.khanoomi.geofencealert.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val accent: Color,
    val container: Color
)

/** آموزش شروع به کار؛ فقط در اولین اجرای برنامه نمایش داده می‌شود. */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pages = listOf(
        OnboardingPage(
            title = "محدوده‌ات را تعریف کن",
            desc = "نقطه‌ای که برایت مهم است انتخاب کن: خانه، محل کار، مدرسه…",
            icon = Icons.Filled.LocationOn,
            accent = scheme.primary,
            container = scheme.primaryContainer
        ),
        OnboardingPage(
            title = "مجوزها را بده",
            desc = "برای اینکه ورودت را حتی وقتی برنامه باز نیست تشخیص بدهم، به مکان و اعلان نیاز دارم.",
            icon = Icons.Filled.CheckCircle,
            accent = scheme.secondary,
            container = scheme.secondaryContainer
        ),
        OnboardingPage(
            title = "وارد که شدی خبرت می‌کنم",
            desc = "با ورود به محدوده، هشدار صوتی پخش می‌شود؛ تو انتخاب می‌کنی با خروج قطع شود یا دستی.",
            icon = Icons.Filled.Notifications,
            accent = scheme.primary,
            container = scheme.primaryContainer
        )
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { index ->
            OnboardingPageContent(page = pages[index])
        }
        // نشانگر نقطه‌ها
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { i ->
                val width by animateDpAsState(
                    targetValue = if (i == pagerState.currentPage) 28.dp else 8.dp,
                    label = "dot"
                )
                Surface(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape),
                    color = if (i == pagerState.currentPage) scheme.primary
                    else scheme.outline
                ) {}
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) { Text("قبلی") }
            } else {
                Spacer(Modifier.width(64.dp))
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = {
                if (pagerState.currentPage == pages.lastIndex) {
                    onFinish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            }) {
                Text(if (pagerState.currentPage == pages.lastIndex) "شروع" else "بعدی")
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val maxR = size.minDimension / 2f
            listOf(1f, 0.7f, 0.45f).forEach { f ->
                drawCircle(color = page.container, radius = maxR * f)
                drawCircle(
                    color = page.accent.copy(alpha = 0.5f),
                    radius = maxR * f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(color = page.accent.copy(alpha = 0.25f), radius = maxR * 0.22f)
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = CircleShape,
            color = page.container,
            modifier = Modifier.size(72.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    page.icon,
                    contentDescription = null,
                    tint = page.accent,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            page.desc,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
