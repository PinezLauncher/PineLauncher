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
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import android.util.Log.e
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)

        setContent {
            val logs = remember { mutableStateListOf<String>() }
            fun log(msg: String) {
                logs.add(msg)
            }
            val context = LocalContext.current
            var showUpdateDialog by remember { mutableStateOf(false) }
            var apkUrl by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {

                log("Проверка обновлений...")

                val result = withContext(Dispatchers.IO) {
                    checkUpdate { log(it) }
                }

                if (result == null) {
                    log("result = null")
                }

                result?.let { (latestVersion, url) ->

                    val current = getCurrentVersion(context)

                    log("Latest: $latestVersion")
                    log("Current: $current")

                    val latest = latestVersion.removePrefix("v")
                    val currentClean = current.removePrefix("v")

                    val compare = compareVersions(latest, currentClean)

                    log("Compare: $compare")

                    if (compare > 0) {
                        log("ЕСТЬ ОБНОВЛЕНИЕ")
                        showUpdateDialog = true
                        apkUrl = url
                    }
                }
            }
            var showSettings by remember { mutableStateOf(false) }
            var sortByVersion by remember {
                mutableStateOf(prefs.getBoolean("sortByVersion", false))
            }

            var lightTheme by remember {
                mutableStateOf(prefs.getBoolean("lightTheme", false))
            }

            var hideIcons by remember {
                mutableStateOf(prefs.getBoolean("hideIcons", false))
            }


            PineLauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (lightTheme) Color.White else Color.Black
                ) {

                    if (showUpdateDialog) {

                        Dialog(onDismissRequest = { showUpdateDialog = false }) {

                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF232323)
                                )
                            ) {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(20.dp)
                                ) {

                                    val context = LocalContext.current
                                    val updateBitmap = remember {
                                        BitmapFactory.decodeResource(context.resources, R.drawable.update).asImageBitmap()
                                    }

                                    Image(
                                        bitmap = updateBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.size(180.dp),
                                        filterQuality = FilterQuality.None
                                    )


                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "Доступно обновление",
                                        color = Color.White
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))


                                    Button(
                                        onClick = {},
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00E997),
                                            contentColor = Color(0xFF232323)
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text(text = "Скачать")
                                    }

                                }
                            }
                        }
                    }
                    if (showSettings) {
                        SettingsScreen(
                            sortByVersion = sortByVersion,
                            lightTheme = lightTheme,
                            hideIcons = hideIcons,
                            onToggleSort = {
                                sortByVersion = !sortByVersion
                                prefs.edit().putBoolean("sortByVersion", sortByVersion).apply()
                            },
                            onToggleTheme = {
                                lightTheme = !lightTheme
                                prefs.edit().putBoolean("lightTheme", lightTheme).apply()
                            },
                            onToggleHideIcons = {
                                hideIcons = !hideIcons
                                prefs.edit().putBoolean("hideIcons", hideIcons).apply()
                            },
                            onBack = { showSettings = false }
                        )
                    } else {
                        MainScreen(
                            sortByVersion = sortByVersion,
                            onOpenSettings = { showSettings = true }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color(0xAA000000))
                                .padding(8.dp)
                                .width(220.dp)
                        ) {
                            logs.takeLast(8).forEach {
                                Text(
                                    text = it,
                                    color = Color.Green,

                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun downloadAndInstall(context: Context, url: String) {

        val request = DownloadManager.Request(Uri.parse(url))
        request.setNotificationVisibility(
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )

        val fileName = "update.apk"
        request.setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            fileName
        )

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {

                val uri = dm.getUriForDownloadedFile(downloadId)

                val installIntent = Intent(Intent.ACTION_VIEW)
                installIntent.setDataAndType(
                    uri,
                    "application/vnd.android.package-archive"
                )
                installIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                context.startActivity(installIntent)
            }
        }

        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun getCurrentVersion(context: Context): String {
        return context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "0"
    }

    suspend fun checkUpdate(log: (String) -> Unit): Pair<String, String>? {
        return try {
            val url = URL("https://api.github.com/repos/PinezLauncher/PineLauncher/releases/latest")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            log("HTTP code: $responseCode")

            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val input = stream.bufferedReader().use { it.readText() }
            log("JSON: ${input.take(100)}...")

            val obj = JSONObject(input)

            val version = obj.getString("tag_name")

            val apkUrl = "https://github.com/PinezLauncher/PineLauncher/releases/latest/download//app-debug.apk"

            Pair(version, apkUrl)

        } catch (e: Exception) {
            log("ERROR: ${e.javaClass.simpleName}")
            log("MSG: ${e.message}")
            null
        }
    }


    @Composable
    fun SettingItem(
        title: String,
        checked: Boolean,
        onToggle: () -> Unit
    ) {
        Card(
            onClick = onToggle,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.07f)
            ),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = title,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                SpriteIcon(
                    spriteRes = R.drawable.icons,
                    indexX = if (checked) 2 else 1,
                    indexY = 0,
                    modifier = Modifier.size(24.dp)
                )
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
        sortByVersion: Boolean,
        lightTheme: Boolean,
        hideIcons: Boolean,
        onToggleSort: () -> Unit,
        onToggleTheme: () -> Unit,
        onToggleHideIcons: () -> Unit,
        onBack: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
            ) {

                SettingItem(
                    title = "<  Сортировать по версии",
                    checked = sortByVersion,
                    onToggle = onToggleSort
                )

                SettingItem(
                    title = "?  Светлая тема",
                    checked = lightTheme,
                    onToggle = onToggleTheme
                )

                SettingItem(
                    title = "<>±;?@/",
                    checked = hideIcons,
                    onToggle = onToggleHideIcons
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
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
    fun GlassActionButton(
        text: String,
        onClick: () -> Unit
    ) {

        Card(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF161616)
            ),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = 0.2f)
            )
        ) {

            Text(
                text = text,
                color = Color.White,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                )
            )
        }
    }

    @Composable
    fun MainScreen(
        sortByVersion: Boolean,
        onOpenSettings: () -> Unit
    ) {
        var showFabMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                painter = painterResource(id = R.drawable.bg_overlay),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopStart),
                alpha = 1.0f
            )

            MojangAppList(sortByVersion)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {

                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.07f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                ) {

                    Box(
                        modifier = Modifier
                            .padding(vertical = 16.dp, horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PineLauncher",
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                ) {
                    GlassIconButton(
                        indexX = 2,
                        indexY = 1,
                        onClick = onOpenSettings
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(vertical = 120.dp, horizontal = 16.dp)
            ) {

                Column(horizontalAlignment = Alignment.End) {

                    if (showFabMenu) {

                        GlassActionButton(
                            text = "@  Скачать Minecraft",
                            onClick = { }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassActionButton(
                            text = "±  Выбрать приложение",
                            onClick = { }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    GlassIconButton(
                        indexX = 2,
                        indexY = 2,
                        onClick = { showFabMenu = !showFabMenu }
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
    fun MojangAppList(
        sortByVersion: Boolean
    ) {
        val context = LocalContext.current
        val pm = context.packageManager

        val apps = remember {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { it.packageName.startsWith("com.mojang") }
        }

        val sortedApps = if (sortByVersion) {
            apps.sortedWith { app1, app2 ->

                val version1 = try {
                    pm.getPackageInfo(app1.packageName, 0).versionName ?: "0"
                } catch (e: Exception) {
                    "0"
                }

                val version2 = try {
                    pm.getPackageInfo(app2.packageName, 0).versionName ?: "0"
                } catch (e: Exception) {
                    "0"
                }

                compareVersions(version2, version1)
            }
        } else {
            apps
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 110.dp)
        ) {
            items(sortedApps) { app ->

                val appName = pm.getApplicationLabel(app).toString()
                val versionName = try {
                    pm.getPackageInfo(app.packageName, 0).versionName ?: "?"
                } catch (e: Exception) {
                    "?"
                }

                MojangCard(appName, versionName, app.packageName)
            }
        }
    }

    @Composable
    fun MojangCard(
        appName: String,
        versionName: String,
        packageName: String
    ) {
        val context = LocalContext.current
        val pm = context.packageManager

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

                Column(modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(R.drawable.icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp)
                    )
                    Text(text = appName, color = Color.White)
                    Text(
                        text = "Версия: $versionName",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                IconButton(
                    onClick = {
                        val intent = pm.getLaunchIntentForPackage(packageName)
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

    fun versionToList(version: String): List<Int> {
        return version.split(".")
            .map { it.toIntOrNull() ?: 0 }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }

            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }

        return 0
    }
}