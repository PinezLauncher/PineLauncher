package com.ananas.pinelauncher

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import com.ananas.pinelauncher.ui.theme.AppTypography
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import com.ananas.pinelauncher.ui.theme.PineLauncherTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        controller.hide(
            WindowInsetsCompat.Type.systemBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE


        setContent {
            MaterialTheme(
                typography = AppTypography
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black
                    ) {}

                    Image(
                        painter = painterResource(id = R.drawable.bg_overlay),
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.TopStart),
                        alpha = 1.0f
                    )

                    MojangAppList()
                }
            }


                var showSettings by remember { mutableStateOf(false) }

                PineLauncherTheme {

                    if (showSettings) {
                        SettingsScreen(
                            onBack = { showSettings = false }
                        )
                    } else {
                        MainScreen(
                            onOpenSettings = { showSettings = true }
                        )
                    }
                }

        }
    }
}
@Composable
fun GlassIconButton(
    indexX: Int,
    indexY: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.07f)
        ),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.2f)
        ),
        modifier = Modifier.size(56.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            SpriteIcon(
                spriteRes = R.drawable.icons,
                indexX = indexX,
                indexY = indexY,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Text(
            text = "Настройки",
            color = Color.White,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            GlassIconButton(
                indexX = 1,
                indexY = 3,
                onClick = onBack
            )
        }
    }
}
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {

            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            ) {
                GlassIconButton(
                    indexX = 2,
                    indexY = 1,
                    onClick = onOpenSettings
                )
            }
        }
    }
}
@Composable
fun SpriteIcon(
    spriteRes: Int,
    indexX: Int,
    indexY: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = BitmapFactory.decodeResource(context.resources, spriteRes)

    val icon = Bitmap.createBitmap(
        bitmap,
        indexX * 9,
        indexY * 9,
        9,
        9
    )

    Image(
        bitmap = icon.asImageBitmap(),
        contentDescription = null,
        modifier = modifier,
        filterQuality = FilterQuality.None
    )
}

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun MojangAppList() {

    val context = LocalContext.current
    val pm = context.packageManager

    val apps = remember {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName.startsWith("com.mojang") }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp)
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.logo1),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 36.dp, bottom = 6.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }

        items(apps) { app ->
            val appName = pm.getApplicationLabel(app).toString()
            val packageInfo = pm.getPackageInfo(app.packageName, 0)
            val versionName = packageInfo.versionName ?: "Неизвестно"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.07f)
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.2f)
                )
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Image(
                            painter = painterResource(R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp)
                        )

                        Text(
                            text = appName,
                            color = Color.White
                        )

                        Text(
                            text = "Версия: $versionName",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    IconButton(
                        onClick = {
                            val intent = pm.getLaunchIntentForPackage(app.packageName)
                            intent?.let { context.startActivity(it) }
                        }
                    ) {
                        SpriteIcon(
                            spriteRes = R.drawable.icons,
                            indexX = 3,
                            indexY = 3,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}