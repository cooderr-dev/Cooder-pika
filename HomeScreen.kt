package com.example.wllpr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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

@Composable
fun HomeScreen(
    dark: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "WLLPR",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Pill(listOf("Dark", "Light"), if (dark) 0 else 1) { onThemeChange(it == 0) }
            }
        }
        item { Hero(Catalog.featured, onOpen) }
        items(Catalog.sections, key = { it.pattern }) { section ->
            SectionRow(section, onOpen)
        }
    }
}

@Composable
private fun Hero(wp: Wallpaper, onOpen: (String) -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .aspectRatio(1.25f)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpen(wp.id) }
    ) {
        GenImage(wp.spec, 900, 720, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                "FEATURED",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
            Text(wp.name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(
                "${wp.patternName} · ${wp.paletteName} · ${wp.modeLabel}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SectionRow(section: Section, onOpen: (String) -> Unit) {
    val items = remember(section) { Catalog.all.filter { it.section == section.pattern } }
    Column(Modifier.padding(top = 28.dp)) {
        Text(
            section.title,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            section.subtitle,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(
            modifier = Modifier.padding(top = 12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { wp -> WallpaperCard(wp, onOpen) }
        }
    }
}

@Composable
private fun WallpaperCard(wp: Wallpaper, onOpen: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .width(132.dp)
            .aspectRatio(9f / 19.5f)
            .clip(shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), shape)
            .clickable { onOpen(wp.id) }
    ) {
        GenImage(wp.spec, 270, 585, Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.65f to Color.Transparent, 1f to Color(0xB3000000)))
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(wp.paletteName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(wp.modeLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}
