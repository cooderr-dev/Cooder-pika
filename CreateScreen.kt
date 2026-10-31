package com.example.wllpr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun CreateScreen() {
    var pattern by rememberSaveable { mutableStateOf(0) }
    var palette by rememberSaveable { mutableStateOf(0) }
    var seed by rememberSaveable { mutableStateOf(Random.nextInt(100000)) }
    var inverted by rememberSaveable { mutableStateOf(false) }

    val spec = Spec(pattern, palette, seed, inverted)
    val aspect = remember { Device.aspect() }
    val previewH = remember { (540 * aspect).roundToInt() }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Text(
            "Create",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        // Live preview with a fake lock-screen clock
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.6f)
                .aspectRatio(1f / aspect)
                .clip(RoundedCornerShape(28.dp))
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(28.dp))
        ) {
            GenImage(spec, 540, previewH, Modifier.matchParentSize())
            ClockOverlay(spec)
        }

        Label("Style")
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PATTERN_LABELS.forEachIndexed { i, label ->
                val selected = i == pattern
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .width(72.dp)
                            .aspectRatio(0.62f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.onBackground else Color.Transparent),
                                RoundedCornerShape(12.dp),
                            )
                            .clickable { pattern = i }
                    ) {
                        GenImage(Spec(i, palette, 42, inverted), 180, 290, Modifier.matchParentSize())
                    }
                    Text(
                        label,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Label("Palette")
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Palettes.all.forEachIndexed { i, pal ->
                val selected = i == palette
                val ramp = pal.colors.map { Color(it) }
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(ramp))
                        .border(
                            BorderStroke(2.dp, if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline),
                            CircleShape,
                        )
                        .clickable { palette = i }
                )
            }
        }

        Label("Mode")
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Pill(listOf("Dark", "Light"), if (inverted) 1 else 0) { inverted = it == 1 }
            OutlinedButton(onClick = { seed = Random.nextInt(100000) }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Shuffle")
            }
        }

        Spacer(Modifier.padding(top = 20.dp))
        WallpaperActions(
            spec = spec,
            fileName = "wllpr-custom-$seed",
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ClockOverlay(spec: Spec) {
    val textColor = if (isLightBackground(spec)) Color(0xBF000000) else Color(0xE6FFFFFF)
    val now = remember { LocalTime.now() }
    val time = remember(now) { "%02d:%02d".format(now.hour, now.minute) }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM")) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(date, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        Text(time, color = textColor, fontSize = 40.sp, fontWeight = FontWeight.Bold)
    }
}
