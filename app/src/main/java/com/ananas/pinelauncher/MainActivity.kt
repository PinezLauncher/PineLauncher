package com.ananas.pinelauncher

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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


val PineFont = FontFamily(
    Font(R.font.pine_font, FontWeight.Normal)
)
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = PineFont
    ),
    titleLarge = TextStyle(
        fontFamily = PineFont
    )
)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                typography = AppTypography
            ) {
                MojangAppList()
            }
            Box(modifier = Modifier.fillMaxSize()) {

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {}

                Image(
                    painter = painterResource(id = R.drawable.bg_overlay),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.TopStart),
                    alpha = 1.0f
                )

                MojangAppList()
            }
        }
    }
}

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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clickable {
                        val intent = pm.getLaunchIntentForPackage(app.packageName)
                        intent?.let { context.startActivity(it) }
                    }
            ) {
                Text(
                    text = appName,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}