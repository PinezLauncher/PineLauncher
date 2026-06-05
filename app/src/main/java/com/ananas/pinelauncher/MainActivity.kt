package com.ananas.pinelauncher

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ananas.pinelauncher.ui.theme.PineLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
class MainActivity : ComponentActivity() {

    private val importedFileUri = mutableStateOf<Uri?>(null)
    private val isPreparingFile = mutableStateOf(false)
    private val originalFileName = mutableStateOf("")

    private fun copyFileToLocalCache(context: Context, sourceUri: Uri, fileName: String): Uri {
        val sharedDir = java.io.File(context.externalCacheDir ?: context.cacheDir, "shared")
        if (!sharedDir.exists()) {
            sharedDir.mkdirs()
        }
        sharedDir.listFiles()?.forEach { it.delete() }
        val targetFile = java.io.File(sharedDir, fileName)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            java.io.FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        val authority = "${context.packageName}.fileprovider"
        return androidx.core.content.FileProvider.getUriForFile(context, authority, targetFile)
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        originalFileName.value = getFileName(this, data)
        isPreparingFile.value = true
        Thread {
            try {
                val copiedUri = copyFileToLocalCache(this, data, originalFileName.value)
                runOnUiThread {
                    importedFileUri.value = copiedUri
                    isPreparingFile.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    isPreparingFile.value = false
                    android.widget.Toast.makeText(
                        this,
                        "Ошибка подготовки файла: ${e.localizedMessage}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result ?: "file"
    }

    fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable) {
            if (drawable.bitmap != null) {
                return drawable.bitmap
            }
        }
        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

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
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    originalFileName.value = getFileName(context, uri)
                    isPreparingFile.value = true
                    Thread {
                        try {
                            val copiedUri = copyFileToLocalCache(context, uri, originalFileName.value)
                            runOnUiThread {
                                importedFileUri.value = copiedUri
                                isPreparingFile.value = false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread {
                                isPreparingFile.value = false
                                android.widget.Toast.makeText(
                                    context,
                                    "Ошибка подготовки файла: ${e.localizedMessage}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }.start()
                }
            }
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



                    if (isPreparingFile.value) {
                        Dialog(onDismissRequest = {}) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E1E1E)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(24.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF00E997),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Подготовка файла...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White
                                    )
                                    if (originalFileName.value.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = originalFileName.value,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Dialog to choose which Minecraft package should open the imported .mcpack/.mcaddon/.mcworld/.mctemplate
                    if (importedFileUri.value != null) {
                        val fileUri = importedFileUri.value!!
                        val fileName = getFileName(context, fileUri)
                        val context = LocalContext.current
                        val bgBitmap = remember {
                            BitmapFactory.decodeResource(context.resources, R.drawable.bg_file).asImageBitmap()
                        }

                        Dialog(onDismissRequest = { importedFileUri.value = null }) {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1E1E1E)
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Box {
                                    // 1. ФОН (как мы делали ранее)
                                    Image(
                                        bitmap = bgBitmap, // Если bgBitmap не определен здесь, передайте его
                                        contentDescription = null,
                                        modifier = Modifier.matchParentSize(), // Заполняет всю карточку
                                        contentScale = ContentScale.Crop
                                    )

                                    // Затемнение (чтобы белый текст был читабельным)
                                    Box(
                                        modifier = Modifier.matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.7f))
                                    )

                                    Column(
                                        modifier = Modifier
                                            .padding(24.dp)
                                            .fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Открыть файл",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = fileName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF00E997),
                                            modifier = Modifier.padding(bottom = 16.dp),
                                            maxLines = 2
                                        )

                                        val pm = context.packageManager
                                        val mojangApps = remember {
                                            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                                                .filter { it.packageName.startsWith("com.mojang") }
                                        }

                                        if (mojangApps.isEmpty()) {
                                            Text(
                                                text = "Майнкрафт не установлен",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Red,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                        } else {
                                            LazyColumn(
                                                modifier = Modifier
                                                    .heightIn(max = 240.dp)
                                                    .padding(bottom = 16.dp)
                                            ) {
                                                items(mojangApps) { app ->
                                                    val appName =
                                                        pm.getApplicationLabel(app).toString()
                                                    val versionName = try {
                                                        pm.getPackageInfo(
                                                            app.packageName,
                                                            0
                                                        ).versionName ?: "?"
                                                    } catch (e: Exception) {
                                                        "?"
                                                    }
                                                    val appIconBitmap = remember(app.packageName) {
                                                        try {
                                                            val drawable =
                                                                pm.getApplicationIcon(app)
                                                            drawableToBitmap(drawable).asImageBitmap()
                                                        } catch (e: Exception) {
                                                            null
                                                        }
                                                    }

                                                    Card(
                                                        onClick = {
                                                            try {
                                                                context.grantUriPermission(
                                                                    app.packageName,
                                                                    fileUri,
                                                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                                )
                                                                val launchIntent =
                                                                    Intent(Intent.ACTION_VIEW).apply {
                                                                        setDataAndType(
                                                                            fileUri,
                                                                            "application/octet-stream"
                                                                        )
                                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                        setPackage(app.packageName)
                                                                    }
                                                                context.startActivity(launchIntent)
                                                            } catch (e: Exception) {
                                                                e.printStackTrace()
                                                                try {
                                                                    val launchIntent =
                                                                        Intent(Intent.ACTION_VIEW).apply {
                                                                            setDataAndType(
                                                                                fileUri,
                                                                                "*/*"
                                                                            )
                                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                            setPackage(app.packageName)
                                                                        }
                                                                    context.startActivity(
                                                                        launchIntent
                                                                    )
                                                                } catch (e2: Exception) {
                                                                    e2.printStackTrace()
                                                                    try {
                                                                        val launchIntent =
                                                                            Intent(Intent.ACTION_VIEW).apply {
                                                                                setData(fileUri)
                                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                                setPackage(app.packageName)
                                                                            }
                                                                        context.startActivity(
                                                                            launchIntent
                                                                        )
                                                                    } catch (ex: Exception) {
                                                                        ex.printStackTrace()
                                                                        android.widget.Toast.makeText(
                                                                            context,
                                                                            "Не удалось запустить: ${ex.localizedMessage}",
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                }
                                                            }
                                                            importedFileUri.value = null
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        shape = RoundedCornerShape(16.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color.White.copy(alpha = 0.07f)
                                                        ),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            Color.White.copy(alpha = 0.1f)
                                                        )
                                                    ) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(12.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            if (appIconBitmap != null) {
                                                                Image(
                                                                    bitmap = appIconBitmap,
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(36.dp)
                                                                )
                                                            } else {
                                                                Image(
                                                                    painter = painterResource(R.drawable.icon),
                                                                    contentDescription = null,
                                                                    modifier = Modifier.size(36.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Column {
                                                                Text(
                                                                    text = appName,
                                                                    color = Color.White,
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                                Text(
                                                                    text = "Версия: $versionName",
                                                                    color = Color.White.copy(alpha = 0.6f),
                                                                    style = MaterialTheme.typography.bodySmall
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { importedFileUri.value = null },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF232323),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                Color.White.copy(alpha = 0.2f)
                                            )
                                        ) {
                                            Text(text = "Закрыть")
                                        }
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
                            onOpenSettings = { showSettings = true },
                            onSelectFile = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            hasUpdate = showUpdateDialog,
                            apkUrl = apkUrl,
                            onDownloadUpdate = { url ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Не удалось открыть браузер", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
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

            val apkUrl = "https://github.com/PinezLauncher/PineLauncher/releases/latest/"

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
    ) {val context = LocalContext.current
        val bgBitmap = remember {
            BitmapFactory.decodeResource(context.resources, R.drawable.bg_settings).asImageBitmap()
        }

        Box(
            modifier = Modifier.fillMaxSize() // Модификатор для самого контейнера Box
        ) {
            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                // Занимает всю ширину, высота подстраивается автоматически
                modifier = Modifier.fillMaxWidth(),
                // Растягивает по ширине, сохраняя пропорции
                contentScale = ContentScale.FillWidth,
                // Прижимает к верхнему краю
                alignment = Alignment.TopStart
            )
            Box(
                modifier = Modifier
                    .padding(16.dp) // Вот здесь задаются отступы для кнопки
                    .align(Alignment.TopStart) // Прижимаем к углу
            ) {
                GlassIconButton(
                    indexX = 1,
                    indexY = 3,
                    onClick = onBack
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
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
        onOpenSettings: () -> Unit,
        onSelectFile: () -> Unit,
        hasUpdate: Boolean,
        apkUrl: String,
        onDownloadUpdate: (String) -> Unit
    ) {
        val context = LocalContext.current
        val bgBitmap = remember {
            BitmapFactory.decodeResource(context.resources, R.drawable.bg_overlay).asImageBitmap()
        }
        var showFabMenu by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxSize()) {

            Image(
                bitmap = bgBitmap,
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopStart),
                alpha = 1.0f,
                filterQuality = FilterQuality.High
            )

            MojangAppList(
                sortByVersion = sortByVersion,
                hasUpdate = hasUpdate,
                apkUrl = apkUrl,
                onDownloadUpdate = onDownloadUpdate
            )
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
                            text = "/  Выбрать файл",
                            onClick = {
                                onSelectFile()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GlassActionButton(
                            text = "±  Добавить приложение",
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
        sortByVersion: Boolean,
        hasUpdate: Boolean,
        apkUrl: String,
        onDownloadUpdate: (String) -> Unit
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
            if (hasUpdate) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.07f) // Стиль главного меню
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
                            val updateBitmap = remember {
                                BitmapFactory.decodeResource(context.resources, R.drawable.update).asImageBitmap()
                            }
                            Image(
                                bitmap = updateBitmap,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                filterQuality = FilterQuality.None
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Доступно обновление",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { onDownloadUpdate(apkUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E997),
                                    contentColor = Color(0xFF232323)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "@  Скачать",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
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

        val appIconBitmap = remember(packageName) {
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val drawable = pm.getApplicationIcon(appInfo)
                drawableToBitmap(drawable).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

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
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 8.dp)
                        )
                    }
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
