package com.ananas.pinelauncher

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Typography
import com.ananas.pinelauncher.ui.theme.AppTypography
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        }
    }
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(apps) { app ->

            val appName = pm.getApplicationLabel(app).toString()

            val packageInfo = pm.getPackageInfo(app.packageName, 0)
            val versionName = packageInfo.versionName ?: "Неизвестно"
            val versionCode = packageInfo.longVersionCode

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        val intent = pm.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Версия: $versionName ($versionCode)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}