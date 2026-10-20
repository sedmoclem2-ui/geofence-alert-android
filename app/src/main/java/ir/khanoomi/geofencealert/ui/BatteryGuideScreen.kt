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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back
                        )
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
                    strings.bgIntro,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            item {
                GuideCard(
                    title = strings.bg1Title,
                    steps = strings.bg1Steps.split("\n")
                )
            }
            item {
                GuideCard(
                    title = strings.bg2Title,
                    steps = strings.bg2Steps.split("\n")
                )
            }
            item {
                GuideCard(
                    title = strings.bg3Title,
                    steps = strings.bg3Steps.split("\n")
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
