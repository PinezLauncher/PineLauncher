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
import com.ananas.pinelauncher.ui.theme.AppTypography
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(0.dp)
    ) {

        item {
            Image(
                painter = painterResource(id = R.drawable.logo),
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clickable {
                        val intent = pm.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.07f)
                ),
                border = BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
            }
        }
    }
}