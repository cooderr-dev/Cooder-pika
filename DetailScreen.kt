package com.example.wllpr

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun DetailScreen(wp: Wallpaper, onBack: () -> Unit) {
    val previewH = remember { (720 * Device.aspect()).roundToInt() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GenImage(wp.spec, 720, previewH, Modifier.fillMaxSize())

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0x66000000)),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 56.dp)
        ) {
            Text(wp.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "${wp.patternName} · ${wp.paletteName} · ${wp.modeLabel}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(16.dp))
            WallpaperActions(
                spec = wp.spec,
                fileName = "wllpr-${wp.id}",
                onScrim = true,
            )
        }
    }
}
