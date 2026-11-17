package com.red4u.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(val version: String, val apkUrl: String, val notes: String)

object AppUpdater {
    private const val RELEASE_API = "https://api.github.com/repos/Redadz19/red4u/releases/latest"

    suspend fun findUpdate(currentVersion: String): ReleaseInfo? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "RED4U-Android")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val tag = json.optString("tag_name").removePrefix("v")
            val apk = json.optJSONArray("assets") ?: return@withContext null
            val asset = (0 until apk.length()).map { apk.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", true) }
                ?: return@withContext null
            if (compareVersions(tag, currentVersion) <= 0) return@withContext null
            ReleaseInfo(tag, asset.getString("browser_download_url"), json.optString("body"))
        } finally {
            connection.disconnect()
        }
    }

    suspend fun downloadAndInstall(context: Context, release: ReleaseInfo): String =
        withContext(Dispatchers.IO) {
            val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty("User-Agent", "RED4U-Android")
            }
            val apk = File(context.cacheDir, "updates/RED4U-update.apk")
            apk.parentFile?.mkdirs()
            try {
                if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
                connection.inputStream.use { input -> apk.outputStream().use { output -> input.copyTo(output) } }
            } finally {
                connection.disconnect()
            }
            withContext(Dispatchers.Main) {
                val uri: Uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", apk
                )
                if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                    val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}"))
                    settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settings)
                    "permission"
                } else {
                    val install = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(install)
                    "started"
                }
            }
        }

    private fun compareVersions(a: String, b: String): Int {
        val left = a.split(".").map { it.toIntOrNull() ?: 0 }
        val right = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = (left.getOrElse(i) { 0 }).compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }
}
