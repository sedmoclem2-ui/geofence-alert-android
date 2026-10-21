package ir.khanoomi.geofencealert.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.khanoomi.geofencealert.R
import ir.khanoomi.geofencealert.ui.theme.mixColor
import kotlinx.coroutines.launch

/**
 * نخستین اجرای برنامه:
 * ۱) اگر [startWithLanguage] درست باشد، اول «صحنه انتخاب زبان» می‌آید؛ با
 *    لمس هر زبان، زبان برنامه همان لحظه عوض می‌شود و صحنه‌های معرفی با
 *    همان زبان ادامه پیدا می‌کنند.
 * ۲) سپس سه صحنه معرفی با پس‌زمینه گرادیانیِ هم‌رنگ تم و لوگوی واقعی
 *    برنامه نمایش داده می‌شوند.
 */
@Composable
fun OnboardingScreen(
    startWithLanguage: Boolean,
    onLanguageChosen: (String) -> Unit,
    onFinish: () -> Unit
) {
    var languagePicked by remember { mutableStateOf(!startWithLanguage) }
    if (!languagePicked) {
        LanguageScene(onSelect = { key ->
            onLanguageChosen(key)
            languagePicked = true
        })
    } else {
        IntroPager(onFinish = onFinish)
    }
}

/**
 * صحنه انتخاب زبان: گرادیان قطری primary ← secondary، لوگوی شناور برنامه
 * و سه کارت زبان با ورود پلکانی (محو + سرخوردن).
 */
@Composable
private fun LanguageScene(onSelect: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalStrings.current
    val infinite = rememberInfiniteTransition(label = "langFloat")
    val floatY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(
            tween(2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "langFloatY"
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(scheme.primary, scheme.secondary))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Image(
                painter = painterResource(R.drawable.img_app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(124.dp)
                    .offset(y = floatY.dp)
                    .clip(RoundedCornerShape(30.dp))
            )
            Spacer(Modifier.height(28.dp))
            Text(
                strings.langSceneTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Choose your language · اختر لغتك",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(36.dp))
            LanguageOptionCard(
                visible = shown, index = 0,
                title = "فارسی", sample = "سلام! خوش آمدید"
            ) { onSelect("fa") }
            LanguageOptionCard(
                visible = shown, index = 1,
                title = "English", sample = "Hello! Welcome"
            ) { onSelect("en") }
            LanguageOptionCard(
                visible = shown, index = 2,
                title = "العربية", sample = "مرحباً! أهلاً بك"
            ) { onSelect("ar") }
        }
    }
}

/** کارت یک زبان در صحنه انتخاب زبان، با تأخیر ورود پلکانی. */
@Composable
private fun LanguageOptionCard(
    visible: Boolean,
    index: Int,
    title: String,
    sample: String,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(450, delayMillis = index * 140)) +
            slideInVertically(
                tween(450, delayMillis = index * 140)
            ) { it / 2 }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Place,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        sample,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private data class IntroPage(val title: String, val desc: String)

/** سه صحنه معرفی با گرادیان متحرکِ متفاوت برای هر صفحه. */
@Composable
private fun IntroPager(onFinish: () -> Unit) {
    val strings = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    val pages = listOf(
        IntroPage(strings.ob1Title, strings.ob1Desc),
        IntroPage(strings.ob2Title, strings.ob2Desc),
        IntroPage(strings.ob3Title, strings.ob3Desc)
    )
    val gradientPairs = listOf(
        scheme.primary to scheme.secondary,
        scheme.secondary to scheme.tertiary,
        mixColor(scheme.secondary, scheme.primary, 0.45f) to scheme.primary
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val topColor by animateColorAsState(
        targetValue = gradientPairs[pagerState.currentPage].first,
        animationSpec = tween(500),
        label = "obGradTop"
    )
    val bottomColor by animateColorAsState(
        targetValue = gradientPairs[pagerState.currentPage].second,
        animationSpec = tween(500),
        label = "obGradBottom"
    )
    val infinite = rememberInfiniteTransition(label = "obFloat")
    val floatY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "obFloatY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(topColor, bottomColor)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                if (pagerState.currentPage < pages.lastIndex) {
                    TextButton(onClick = onFinish) {
                        Text(strings.skip, color = Color.White)
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { index ->
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    when (index) {
                        0 -> LogoWithRadar()
                        1 -> LogoWithZoneCards(floatY)
                        else -> LogoWithBell()
                    }
                    Spacer(Modifier.height(28.dp))
                    Text(
                        pages[index].title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        pages[index].desc,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            // نشانگر صفحه‌ها: کپسول کشیده برای صفحه فعال
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val width by animateDpAsState(
                        targetValue = if (i == pagerState.currentPage) 28.dp else 8.dp,
                        label = "obDot$i"
                    )
                    Surface(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape),
                        color = if (i == pagerState.currentPage) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    ) {}
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }) { Text(strings.previous, color = Color.White) }
                } else {
                    Spacer(Modifier.width(72.dp))
                }
                Spacer(Modifier.weight(1f))
                GradientButton(onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }) {
                    Text(
                        if (pagerState.currentPage == pages.lastIndex) {
                            strings.getStarted
                        } else {
                            strings.next
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** حلقه‌های راداریِ موجی سفید دور یک نقطه مرکزی (صفحه‌های ۱ و ۳). */
@Composable
private fun RadarRings(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "obRadar")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "obRadarProgress"
    )
    Canvas(modifier) {
        val maxR = size.minDimension / 2f
        for (i in 0 until 3) {
            val f = (progress + i / 3f) % 1f
            drawCircle(
                color = Color.White.copy(alpha = 0.55f * (1f - f)),
                radius = maxR * (0.45f + 0.55f * f),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/** صفحه ۱: لوگوی واقعی برنامه در مرکز حلقه‌های راداری. */
@Composable
private fun LogoWithRadar() {
    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        RadarRings(Modifier.fillMaxSize())
        Image(
            painter = painterResource(R.drawable.img_app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
        )
    }
}

/** صفحه ۲: لوگوی شناور + دو کارت محدوده نمایشیِ شناور. */
@Composable
private fun LogoWithZoneCards(floatY: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(R.drawable.img_app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(84.dp)
                .offset(y = floatY.dp)
                .clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(20.dp))
        MockZoneCard(nameWidth = 92, modifier = Modifier.fillMaxWidth(0.82f))
        Spacer(Modifier.height(10.dp))
        MockZoneCard(
            nameWidth = 64,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .offset(x = 26.dp)
        )
    }
}

/** کارت محدوده نمایشی (اسکلت روشن روی پس‌زمینه رنگی) برای صفحه ۲. */
@Composable
private fun MockZoneCard(nameWidth: Int, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.18f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.25f))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(nameWidth.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color.White.copy(alpha = 0.75f))
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
            // کلید روشنِ شبیه‌سازی‌شده
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(3.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/** صفحه ۳: زنگ اعلان در دایره‌ای روشن میان موج‌ها + نشان لوگو در گوشه. */
@Composable
private fun LogoWithBell() {
    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        RadarRings(Modifier.fillMaxSize())
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(104.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.img_app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .align(Alignment.BottomEnd)
                .clip(RoundedCornerShape(14.dp))
        )
    }
}
