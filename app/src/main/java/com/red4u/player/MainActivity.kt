package com.red4u.player

import androidx.activity.ComponentActivity
import android.app.AlertDialog
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
import android.widget.HorizontalScrollView
import android.widget.TextView
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

class MainActivity : ComponentActivity() {
    private val background = Color.rgb(13, 17, 23)
    private val surface = Color.rgb(25, 31, 40)
    private val accent = Color.rgb(34, 197, 94)
    private val builtInPlaylistUrls = listOf(
        "https://iptv-org.github.io/iptv/countries/dz.m3u",
        "https://iptv-org.github.io/iptv/languages/ara.m3u",
        "https://broadcastatlas.com/official.m3u"
    )
    private lateinit var sourceInput: EditText
    private lateinit var statusText: TextView
    private lateinit var channelList: LinearLayout
    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null
    private var channels: List<Channel> = emptyList()
    private val favorites = mutableSetOf<String>()
    private var activeFilter = "all"
    private var sortMode = 0
    private lateinit var searchInput: EditText
    private lateinit var countText: TextView
    private lateinit var filterRow: LinearLayout
    private lateinit var sortButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildInterface()
        val saved = getPreferences(MODE_PRIVATE).getString("playlist_url", "") ?: ""
        sourceInput.setText(saved)
        if (saved.isNotBlank()) loadPlaylist(saved) else loadBuiltInPlaylist()
        checkForUpdates(false)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8, 12, 19))
            layoutDirection = if (Locale.getDefault().language == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(14)); setBackgroundColor(Color.rgb(19, 27, 39))
        }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(TextView(this).apply {
            text = "RED4U"; textSize = 29f; letterSpacing = .12f
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD); setTextColor(Color.rgb(30, 210, 125))
        }, matchWrap())
        brand.addView(TextView(this).apply {
            text = if (Locale.getDefault().language == "ar") "عالم القنوات بين يديك" else "YOUR WORLD OF LIVE TV"
            textSize = 10f; setTextColor(Color.LTGRAY)
        }, matchWrap())
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply {
            text = "● LIVE"; textSize = 11f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(30, 210, 125)); setPadding(dp(10), dp(7), dp(10), dp(7))
            setBackgroundColor(Color.rgb(16, 57, 48))
        }, LinearLayout.LayoutParams(-2, -2))
        root.addView(header, matchWrap())

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(18))
        }
        scroll.addView(body)
        body.addView(sectionLabel(if (Locale.getDefault().language == "ar") "مصدر القنوات" else "PLAYLIST SOURCE"), matchWrap())
        val sourceCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(10), dp(10), dp(10))
            setBackgroundColor(Color.rgb(19, 27, 39))
        }
        sourceInput = EditText(this).apply {
            hint = getString(R.string.source_hint); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
            textSize = 13f; isSingleLine = true; setPadding(dp(12), 0, dp(12), 0)
            setBackgroundColor(Color.rgb(8, 12, 19))
        }
        sourceCard.addView(sourceInput, LinearLayout.LayoutParams(-1, dp(48)))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val builtInButton = Button(this).apply {
            setText(R.string.built_in); setTextColor(Color.WHITE)
            setOnClickListener {
                sourceInput.setText("")
                getPreferences(MODE_PRIVATE).edit().remove("playlist_url").apply()
                loadBuiltInPlaylist()
            }
        }
        val loadButton = Button(this).apply {
            setText(R.string.load); setTextColor(Color.BLACK)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(30, 210, 125)))
            setOnClickListener { loadPlaylist(sourceInput.text.toString().trim()) }
        }
        actions.addView(builtInButton, LinearLayout.LayoutParams(0, dp(46), 1f))
        actions.addView(loadButton, LinearLayout.LayoutParams(0, dp(46), 1f))
        sourceCard.addView(actions); body.addView(sourceCard, matchWrap())

        statusText = TextView(this).apply {
            textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0, dp(8), 0, dp(8))
        }
        body.addView(statusText, matchWrap())
        val updateButton = Button(this).apply {
            text = if (Locale.getDefault().language == "ar") "التحقق من التحديثات" else "CHECK FOR UPDATES"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setOnClickListener { checkForUpdates(true) }
        }
        body.addView(updateButton, LinearLayout.LayoutParams(-1, dp(44)))
        body.addView(sectionLabel(if (Locale.getDefault().language == "ar") "المشاهدة الآن" else "NOW PLAYING"), matchWrap())
        playerView = PlayerView(this).apply { useController = true; setBackgroundColor(Color.BLACK) }
        body.addView(playerView, LinearLayout.LayoutParams(-1, dp(220)))

        val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(sectionLabel(if (Locale.getDefault().language == "ar") "استكشف القنوات" else "EXPLORE CHANNELS"),
            LinearLayout.LayoutParams(0, -2, 1f))
        countText = TextView(this).apply { textSize = 11f; setTextColor(Color.rgb(30, 210, 125)) }
        titleRow.addView(countText, LinearLayout.LayoutParams(-2, -2)); body.addView(titleRow, matchWrap())

        searchInput = EditText(this).apply {
            hint = if (Locale.getDefault().language == "ar") "ابحث عن قناة..." else "Search channels..."
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 14f; isSingleLine = true
            setPadding(dp(12), 0, dp(12), 0); setBackgroundColor(Color.rgb(19, 27, 39))
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderChannels() }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        body.addView(searchInput, LinearLayout.LayoutParams(-1, dp(48)))
        val filterScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        filterRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(9), 0, dp(5)) }
        filterScroll.addView(filterRow); body.addView(filterScroll, LinearLayout.LayoutParams(-1, dp(48)))
        sortButton = Button(this).apply {
            textSize = 11f; isAllCaps = false
            setOnClickListener { sortMode = (sortMode + 1) % 3; updateSortLabel(); renderChannels() }
        }
        body.addView(sortButton, LinearLayout.LayoutParams(-2, dp(42)))
        channelList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(channelList, matchWrap())
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f)); setContentView(root)
        buildFilters(); updateSortLabel()
    }

    private fun checkForUpdates(showCurrent: Boolean) {
        lifecycleScope.launch {
            statusText.text = if (Locale.getDefault().language == "ar") "جارٍ التحقق من التحديثات..." else "Checking for updates..."
            val release = runCatching {
                val current = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
                AppUpdater.findUpdate(current)
            }.getOrNull()
            if (release == null) {
                if (showCurrent) statusText.text = if (Locale.getDefault().language == "ar") "أنت تستخدم أحدث إصدار متاح." else "You are using the latest available version."
                return@launch
            }
            val arabic = Locale.getDefault().language == "ar"
            AlertDialog.Builder(this@MainActivity)
                .setTitle(if (arabic) "تحديث جديد متاح: ${release.version}" else "Update available: ${release.version}")
                .setMessage(release.notes.ifBlank { if (arabic) "يتوفر إصدار جديد من RED4U." else "A new RED4U version is available." })
                .setNegativeButton(if (arabic) "لاحقاً" else "Later", null)
                .setPositiveButton(if (arabic) "تنزيل وتثبيت" else "Download & install") { _, _ ->
                    lifecycleScope.launch {
                        statusText.text = if (arabic) "جارٍ تنزيل التحديث..." else "Downloading update..."
                        runCatching { AppUpdater.downloadAndInstall(this@MainActivity, release) }
                            .onFailure { statusText.text = if (arabic) "تعذر تنزيل التحديث: ${it.message}" else "Update failed: ${it.message}" }
                    }
                }.show()
        }
    }

    private fun sectionLabel(value: String) = TextView(this).apply {
        text = value; textSize = 11f; letterSpacing = .08f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(Color.LTGRAY); setPadding(0, dp(9), 0, dp(8))
    }

    private fun buildFilters() {
        filterRow.removeAllViews()
        val labels = listOf(
            "all" to if (Locale.getDefault().language == "ar") "الكل" else "ALL",
            "algeria" to if (Locale.getDefault().language == "ar") "الجزائر" else "ALGERIA",
            "arabic" to if (Locale.getDefault().language == "ar") "عربية" else "ARABIC",
            "international" to if (Locale.getDefault().language == "ar") "دولية" else "INTERNATIONAL",
            "favorites" to if (Locale.getDefault().language == "ar") "المفضلة ★" else "FAVORITES ★"
        )
        labels.forEach { pair ->
            val chip = Button(this).apply {
                text = pair.second; textSize = 10f; isAllCaps = false; setTextColor(Color.WHITE)
                setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    if (activeFilter == pair.first) Color.rgb(20, 130, 82) else Color.rgb(35, 47, 63)))
                setOnClickListener { activeFilter = pair.first; buildFilters(); renderChannels() }
            }
            filterRow.addView(chip, LinearLayout.LayoutParams(-2, dp(38)).apply { marginEnd = dp(5) })
        }
    }

    private fun updateSortLabel() {
        sortButton.text = when (sortMode) {
            1 -> if (Locale.getDefault().language == "ar") "الترتيب الأصلي" else "SOURCE ORDER"
            2 -> if (Locale.getDefault().language == "ar") "حسب التصنيف" else "BY CATEGORY"
            else -> if (Locale.getDefault().language == "ar") "ترتيب أبجدي أ–ي" else "SORT A–Z"
        }
    }

    private fun category(channel: Channel): String {
        val value = (channel.name + " " + channel.group.orEmpty()).lowercase(Locale.ROOT)
        return when {
            listOf("alger", "algeria", "entv", "الجزائر", "جزائر").any { value.contains(it) } -> "algeria"
            listOf("arab", "ara", "middle east", "mena", "عربي", "العربية", "السعودية", "مصر", "المغرب", "تونس").any { value.contains(it) } -> "arabic"
            else -> "international"
        }
    }

    private fun loadBuiltInPlaylist() {
        statusText.setText(R.string.loading)
        lifecycleScope.launch {
            try {
                val loadedLists = withContext(Dispatchers.IO) {
                    builtInPlaylistUrls.mapNotNull { address ->
                        runCatching { downloadText(address) }.getOrNull()
                    }
                }
                if (loadedLists.isEmpty()) error("No built-in playlist source is available")
                channels = loadedLists.flatMap { parseM3u(it) }
                    .distinctBy { it.streamUrl }
                renderChannels()
                statusText.text = if (channels.isEmpty()) getString(R.string.empty_list)
                    else getString(R.string.loaded_count, channels.size)
            } catch (error: Exception) {
                statusText.setText(R.string.load_error)
            }
        }
    }

    private fun loadPlaylist(input: String, saveAsLastUsed: Boolean = true) {
        if (input.isBlank()) {
            statusText.setText(R.string.enter_url)
            return
        }
        val url = normalizeGithubUrl(input)
        if (saveAsLastUsed) getPreferences(MODE_PRIVATE).edit().putString("playlist_url", input).apply()
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
        val query = searchInput.text.toString().trim().lowercase(Locale.ROOT)
        var visible = channels.filter { channel ->
            val matchesSearch = query.isBlank() || channel.name.lowercase(Locale.ROOT).contains(query) ||
                channel.group.orEmpty().lowercase(Locale.ROOT).contains(query)
            val type = category(channel)
            val matchesFilter = when (activeFilter) {
                "algeria" -> type == "algeria"
                "arabic" -> type == "arabic"
                "international" -> type == "international"
                "favorites" -> favorites.contains(channel.streamUrl)
                else -> true
            }
            matchesSearch && matchesFilter
        }
        visible = when (sortMode) {
            1 -> visible
            2 -> visible.sortedWith(compareBy<Channel> { category(it) }.thenBy { it.name.lowercase(Locale.ROOT) })
            else -> visible.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
        countText.text = visible.size.toString() + (if (Locale.getDefault().language == "ar") " قناة" else " CHANNELS")
        if (visible.isEmpty()) {
            channelList.addView(TextView(this).apply {
                text = if (Locale.getDefault().language == "ar") "لا توجد قنوات مطابقة" else "No matching channels"
                setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; textSize = 14f
                setPadding(dp(15), dp(25), dp(15), dp(25))
            }, matchWrap())
            return
        }
        visible.forEachIndexed { index, channel ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(if (index % 2 == 0) Color.rgb(19, 27, 39) else Color.rgb(13, 19, 28))
                isFocusable = true; isClickable = true; setOnClickListener { playChannel(channel) }
            }
            row.addView(TextView(this).apply {
                text = channel.name.take(1).uppercase(Locale.getDefault())
                textSize = 19f; typeface = Typeface.DEFAULT_BOLD; gravity = Gravity.CENTER
                setTextColor(Color.rgb(30, 210, 125)); setBackgroundColor(Color.rgb(17, 63, 53))
            }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(10) })
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            details.addView(TextView(this).apply {
                text = channel.name; textSize = 14f; maxLines = 1
                typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE)
            }, matchWrap())
            details.addView(TextView(this).apply {
                text = channel.group?.takeIf { it.isNotBlank() } ?: when (category(channel)) {
                    "algeria" -> if (Locale.getDefault().language == "ar") "قنوات جزائرية" else "ALGERIAN"
                    "arabic" -> if (Locale.getDefault().language == "ar") "قنوات عربية" else "ARABIC"
                    else -> if (Locale.getDefault().language == "ar") "قنوات دولية" else "INTERNATIONAL"
                }
                textSize = 10f; setTextColor(Color.LTGRAY)
            }, matchWrap())
            row.addView(details, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply {
                text = if (favorites.contains(channel.streamUrl)) "★" else "☆"
                textSize = 23f; gravity = Gravity.CENTER; setTextColor(Color.rgb(255, 194, 72))
                isFocusable = true; isClickable = true
                setOnClickListener {
                    if (!favorites.add(channel.streamUrl)) favorites.remove(channel.streamUrl)
                    renderChannels()
                }
            }, LinearLayout.LayoutParams(dp(38), dp(42)))
            channelList.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(5) })
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
