package com.example.wllpr

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WllprTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            background = Color(0xFF000000),
            onBackground = Color(0xFFF0F0F0),
            surface = Color(0xFF151515),
            onSurface = Color(0xFFF0F0F0),
            surfaceVariant = Color(0xFF1E1E1E),
            onSurfaceVariant = Color(0xFF888888),
            primary = Color(0xFFF0F0F0),
            onPrimary = Color(0xFF111111),
            outline = Color(0xFF2A2A2A),
        )
    } else {
        lightColorScheme(
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF111111),
            surface = Color(0xFFF2F2F2),
            onSurface = Color(0xFF111111),
            surfaceVariant = Color(0xFFE4E4E4),
            onSurfaceVariant = Color(0xFF666666),
            primary = Color(0xFF111111),
            onPrimary = Color(0xFFFFFFFF),
            outline = Color(0xFFDDDDDD),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

/**
 * Draws a generated wallpaper. Renders off the main thread, caches the result,
 * and keeps showing the previous image while a new one is being rendered.
 */
@Composable
fun GenImage(spec: Spec, w: Int, h: Int, modifier: Modifier = Modifier) {
    val key = spec.key(w, h)
    var shown by remember { mutableStateOf<Bitmap?>(BitmapCache.get(key)) }

    LaunchedEffect(key) {
        val cached = BitmapCache.get(key)
        if (cached != null) {
            shown = cached
        } else {
            val bmp = withContext(Dispatchers.Default) { Engine.render(w, h, spec) }
            BitmapCache.put(key, bmp)
            shown = bmp
        }
    }

    val current = shown
    if (current != null) {
        val image = remember(current) { current.asImageBitmap() }
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surface))
    }
}

/** Small segmented control, e.g. Dark | Light. */
@Composable
fun Pill(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            Box(
                Modifier
                    .clip(shape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Set wallpaper" + "Save" buttons with the home / lock / both chooser. */
@Composable
fun WallpaperActions(
    spec: Spec,
    fileName: String,
    modifier: Modifier = Modifier,
    onScrim: Boolean = false,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val primaryColors = if (onScrim) {
        ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
    } else {
        ButtonDefaults.buttonColors()
    }
    val outlineColors = if (onScrim) {
        ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    } else {
        ButtonDefaults.outlinedButtonColors()
    }
    val outlineBorder = if (onScrim) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }

    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { choosing = true },
            enabled = !busy,
            colors = primaryColors,
            modifier = Modifier.weight(1f),
        ) { Text(if (busy) "Working…" else "Set wallpaper") }

        OutlinedButton(
            onClick = {
                scope.launch {
                    busy = true
                    val ok = Actions.saveToGallery(context, spec, fileName)
                    busy = false
                    toast(if (ok) "Saved to Pictures/WLLPR" else "Couldn't save the image")
                }
            },
            enabled = !busy,
            colors = outlineColors,
            border = outlineBorder,
            modifier = Modifier.weight(1f),
        ) { Text("Save") }
    }

    if (choosing) {
        val options = listOf(
            "Home screen" to Actions.TARGET_HOME,
            "Lock screen" to Actions.TARGET_LOCK,
            "Home and lock screen" to Actions.TARGET_BOTH,
        )
        AlertDialog(
            onDismissRequest = { choosing = false },
            title = { Text("Set wallpaper") },
            text = {
                Column {
                    options.forEach { (label, target) ->
                        TextButton(
                            onClick = {
                                choosing = false
                                scope.launch {
                                    busy = true
                                    val ok = Actions.setWallpaper(context, spec, target)
                                    busy = false
                                    toast(if (ok) "Wallpaper set" else "Couldn't set the wallpaper")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(label) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosing = false }) { Text("Cancel") } },
        )
    }
}
