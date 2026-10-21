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
    val strings = LocalStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.batteryGuideTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
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
                    strings.batteryGuideIntro,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                GuideCard(
                    title = strings.guide1Title,
                    steps = listOf(
                        strings.guide1Step1, strings.guide1Step2,
                        strings.guide1Step3, strings.guide1Step4
                    )
                )
            }
            item {
                GuideCard(
                    title = strings.guide2Title,
                    steps = listOf(
                        strings.guide2Step1, strings.guide2Step2,
                        strings.guide2Step3, strings.guide2Step4
                    )
                )
            }
            item {
                GuideCard(
                    title = strings.guide3Title,
                    steps = listOf(
                        strings.guide3Step1, strings.guide3Step2, strings.guide3Step3
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
