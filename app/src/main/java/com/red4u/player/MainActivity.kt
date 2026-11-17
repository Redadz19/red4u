package com.red4u.player

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private data class Channel(
    val name: String,
    val streamUrl: String,
    val logo: String? = null,
    val group: String? = null
)

class MainActivity : Activity() {
    private val background = Color.rgb(13, 17, 23)
    private val surface = Color.rgb(25, 31, 40)
    private val accent = Color.rgb(34, 197, 94)
    private lateinit var sourceInput: EditText
    private lateinit var statusText: TextView
    private lateinit var channelList: LinearLayout
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private var channels: List<Channel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
        val saved = getPreferences(MODE_PRIVATE).getString("playlist_url", "") ?: ""
        sourceInput.setText(saved)
        if (saved.isNotBlank()) loadPlaylist(saved)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(16), dp(14), dp(16), dp(12))
            layoutDirection = if (Locale.getDefault().language == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }

        val heading = TextView(this).apply {
            text = "RED4U"
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
        }
        root.addView(heading, matchWrap())

        val subtitle = TextView(this).apply {
            setText(R.string.tagline)
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(subtitle, matchWrap())

        val sourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        sourceInput = EditText(this).apply {
            hint = getString(R.string.source_hint)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 13f
            singleLine = true
            setPadding(dp(10), 0, dp(10), 0)
            setBackgroundColor(surface)
        }
        sourceRow.addView(sourceInput, LinearLayout.LayoutParams(0, dp(50), 1f))
        val loadButton = Button(this).apply {
            setText(R.string.load)
            setTextColor(Color.BLACK)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { loadPlaylist(sourceInput.text.toString().trim()) }
        }
        sourceRow.addView(loadButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(50)))
        root.addView(sourceRow, matchWrap())

        statusText = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(statusText, matchWrap())

        playerView = PlayerView(this).apply {
            useController = true
            setBackgroundColor(Color.BLACK)
        }
        root.addView(playerView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)))

        val channelHeading = TextView(this).apply {
            setText(R.string.channels)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, dp(14), 0, dp(8))
        }
        root.addView(channelHeading, matchWrap())

        val scroll = ScrollView(this)
        channelList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(channelList)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)
    }

    private fun loadPlaylist(input: String) {
        if (input.isBlank()) {
            statusText.setText(R.string.enter_url)
            return
        }
        val url = normalizeGithubUrl(input)
        getPreferences(MODE_PRIVATE).edit().putString("playlist_url", input).apply()
        statusText.setText(R.string.loading)

        lifecycleScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { downloadText(url) }
                channels = parseM3u(text)
                renderChannels()
                statusText.text = if (channels.isEmpty()) getString(R.string.empty_list)
                    else getString(R.string.loaded_count, channels.size)
            } catch (error: Exception) {
                statusText.setText(R.string.load_error)
            }
        }
    }

    private fun normalizeGithubUrl(input: String): String {
        val clean = input.trim()
        val blobPrefix = "https://github.com/"
        if (clean.startsWith(blobPrefix) && clean.contains("/blob/")) {
            val rest = clean.removePrefix(blobPrefix)
            val parts = rest.split("/blob/", limit = 2)
            if (parts.size == 2) return "https://raw.githubusercontent.com/${parts[0]}/${parts[1]}"
        }
        return clean
    }

    private fun downloadText(address: String): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000
            readTimeout = 20000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "RED4U-Android/1.0")
        }
        return try {
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseM3u(content: String): List<Channel> {
        val result = mutableListOf<Channel>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null

        content.lineSequence().map { it.trim() }.forEach { line ->
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    pendingName = line.substringAfterLast(',', "").trim().ifBlank { "Unknown channel" }
                    pendingLogo = Regex("""tvg-logo=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        .find(line)?.groupValues?.get(1)
                    pendingGroup = Regex("""group-title=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                        .find(line)?.groupValues?.get(1)
                }
                line.isNotEmpty() && !line.startsWith("#") && (line.startsWith("http://") || line.startsWith("https://")) -> {
                    result.add(Channel(pendingName ?: line.substringAfterLast('/'), line, pendingLogo, pendingGroup))
                    pendingName = null
                    pendingLogo = null
                    pendingGroup = null
                }
            }
        }
        return result
    }

    private fun renderChannels() {
        channelList.removeAllViews()
        channels.forEachIndexed { index, channel ->
            val item = TextView(this).apply {
                text = "${index + 1}.  ${channel.name}" +
                    (channel.group?.let { "\n$it" } ?: "")
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(dp(14), dp(13), dp(14), dp(13))
                setBackgroundColor(if (index % 2 == 0) surface else background)
                isFocusable = true
                isClickable = true
                setOnClickListener { playChannel(channel) }
            }
            channelList.addView(item, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(4) })
        }
    }

    private fun playChannel(channel: Channel) {
        if (player == null) {
            player = ExoPlayer.Builder(this).build().also { playerView.player = it }
        }
        player?.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player?.prepare()
        player?.play()
        statusText.text = getString(R.string.now_playing, channel.name)
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroy() {
        playerView.player = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
}
