package com.exemple.ps5launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// =====================================================================
//  RÉGLAGES FACILES À MODIFIER
// =====================================================================
val NEON_DEFAULT = Color(0xFF00E5FF)
const val LAT = 5.30                      // Abidjan / Koumassi
const val LON = -3.95
const val WALLPAPER_QUERY = "neon"        // "" = tout, ou "cyberpunk", "space", "car"...
const val WALLPAPER_DELAY_MS = 10_000L    // changement de fond toutes les 10 s
const val SCREENSAVER_AFTER_MS = 3 * 60_000L
const val REMOTE_PORT = 8080              // contrôle depuis le téléphone
const val LITE_MODE = true                // télé à 1 Go de RAM : effets allégés (mets false pour tout activer)
const val BARCA_ID = "133739"             // FC Barcelone sur TheSportsDB (à vérifier, voir message)
const val SPORTSDB_KEY = "3"              // clé de test gratuite de TheSportsDB
val VIDEO_URLS = listOf<String>()         // liens directs .mp4 (optionnel) pour le fond vidéo

data class Theme(val name: String, val color: Color?, val second: Color, val base: Color)

val THEMES = listOf(
    Theme("Auto (couleur de l'appli)", null, Color(0xFFFF2BD6), Color(0xFF05010F)),
    Theme("Néon rose", Color(0xFFFF2BD6), Color(0xFF7C4DFF), Color(0xFF0A0212)),
    Theme("Matrix", Color(0xFF00FF66), Color(0xFF00B3FF), Color(0xFF010A04)),
    Theme("Glace", Color(0xFF7DD3FF), Color(0xFFB388FF), Color(0xFF020A14)),
    Theme("Barça", Color(0xFFA50044), Color(0xFF004D98), Color(0xFF020818)),
)
val BG_MODES = listOf("Images", "Vidéo", "Particules")
val TABS = listOf("Tout", "Favoris", "Jeux", "Vidéo", "Musique")

// =====================================================================
//  ÉTAT PARTAGÉ (UI + télécommande téléphone)
// =====================================================================
object AppState {
    var themeIndex by mutableStateOf(0)
    var bgMode by mutableStateOf(0)
    var wallpaperSkip by mutableStateOf(0)
    var banner by mutableStateOf<String?>(null)
    var menuOpen by mutableStateOf(false)
    var saver by mutableStateOf(false)
    var resumeTick by mutableStateOf(0)
}

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    fun list(k: String): List<String> = sp.getString(k, "")!!.split(",").filter { it.isNotEmpty() }
    fun save(k: String, v: List<String>) = sp.edit().putString(k, v.joinToString(",")).apply()
    fun int(k: String): Int = sp.getInt(k, 0)
    fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
}

// =====================================================================
//  APPLIS
// =====================================================================
data class AppItem(
    val label: String, val pkg: String, val image: ImageBitmap, val accent: Color, val cat: Int
)

fun accentFrom(bmp: Bitmap): Color {
    val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(px, hsv)
    if (hsv[1] < 0.15f) return NEON_DEFAULT
    hsv[1] = maxOf(hsv[1], 0.75f)
    hsv[2] = 1f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != ctx.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { ri ->
            val drawable = ri.activityInfo.loadBanner(pm) ?: ri.loadIcon(pm)
            val bmp = drawable.toBitmap(320, 180)
            AppItem(
                ri.loadLabel(pm).toString(), ri.activityInfo.packageName,
                bmp.asImageBitmap(), accentFrom(bmp), ri.activityInfo.applicationInfo.category
            )
        }
}

// =====================================================================
//  SONS SYNTHÉTISÉS (aucun fichier audio nécessaire)
// =====================================================================
class Sounds {
    private fun build(freqs: List<Double>, ms: Int): AudioTrack {
        val rate = 22050
        val n = rate * ms / 1000
        val segLen = n / freqs.size
        val data = ShortArray(n) { i ->
            val seg = minOf(i / segLen, freqs.size - 1)
            val j = i - seg * segLen
            val env = exp(-5.0 * j / segLen) * minOf(1.0, j / 120.0)
            (sin(2 * PI * freqs[seg] * i / rate) * env * 12000).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(n * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(data, 0, n)
        return track
    }

    private val moveT = build(listOf(1175.0), 60)
    private val selectT = build(listOf(660.0, 990.0), 200)
    private val bootT = build(listOf(523.0, 659.0, 784.0, 1047.0), 700)

    private fun play(t: AudioTrack) {
        try { t.stop(); t.reloadStaticData(); t.play() } catch (ignored: Exception) {}
    }

    fun move() = play(moveT)
    fun select() = play(selectT)
    fun boot() = play(bootT)
    fun release() { moveT.release(); selectT.release(); bootT.release() }
}

// =====================================================================
//  DONNÉES EN LIGNE : météo, réseau, taux de change, fonds d'écran
// =====================================================================
@Composable
fun rememberWeather(): String {
    var text by remember { mutableStateOf("--") }
    LaunchedEffect(Unit) {
        while (true) {
            text = withContext(Dispatchers.IO) {
                try {
                    val url = URL(
                        "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
                            "&current=temperature_2m,weather_code"
                    )
                    val cur = JSONObject(url.readText()).getJSONObject("current")
                    val icon = when (cur.getInt("weather_code")) {
                        0 -> "☀️"
                        1, 2 -> "🌤️"
                        3 -> "☁️"
                        in 45..48 -> "🌫️"
                        in 51..67, in 80..82 -> "🌧️"
                        in 95..99 -> "⛈️"
                        else -> "🌡️"
                    }
                    "$icon ${cur.getDouble("temperature_2m").roundToInt()}°C"
                } catch (e: Exception) { "--" }
            }
            delay(if (text == "--") 60_000L else 30 * 60_000L)
        }
    }
    return text
}

@Composable
fun rememberRates(): String {
    var text by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            text = withContext(Dispatchers.IO) {
                try {
                    val r = JSONObject(URL("https://open.er-api.com/v6/latest/USD").readText())
                        .getJSONObject("rates")
                    val xof = r.getDouble("XOF")
                    val eur = r.getDouble("EUR")
                    "1 \$ = ${xof.roundToInt()} FCFA   •   1 € = ${(xof / eur).roundToInt()} FCFA"
                } catch (e: Exception) { "" }
            }
            delay(if (text.isEmpty()) 60_000L else 6 * 3_600_000L)
        }
    }
    return text
}

@Composable
fun rememberNetwork(): String {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        while (true) {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            state = when {
                caps == null -> "Hors ligne"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Connecté"
            }
            delay(5000)
        }
    }
    return state
}

@Composable
fun rememberNow(): Date {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1000) } }
    return now
}

@Composable
fun rememberWallpaper(): String? {
    val ctx = LocalContext.current
    var current by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(AppState.wallpaperSkip) {
        var queue = emptyList<String>()
        while (true) {
            if (queue.isEmpty()) {
                queue = withContext(Dispatchers.IO) {
                    try {
                        val url = URL(
                            "https://wallhaven.cc/api/v1/search?q=$WALLPAPER_QUERY" +
                                "&categories=111&purity=100&sorting=random&atleast=1920x1080&page=1"
                        )
                        val arr = JSONObject(url.readText()).getJSONArray("data")
                        List(arr.length()) { arr.getJSONObject(it).getString("path") }
                    } catch (e: Exception) { emptyList() }
                }
                if (queue.isEmpty()) { delay(15_000); continue }
            }
            current = queue.first()
            queue = queue.drop(1)
            queue.firstOrNull()?.let {
                ctx.imageLoader.enqueue(wallpaperRequest(ctx, it))
            }
            delay(WALLPAPER_DELAY_MS)
        }
    }
    return current
}

// ---------- Barça (TheSportsDB, gratuit) ----------
fun fetchEvents(path: String): List<JSONObject> {
    val j = JSONObject(URL("https://www.thesportsdb.com/api/v1/json/$SPORTSDB_KEY/$path?id=$BARCA_ID").readText())
    val arr: JSONArray = j.optJSONArray("events") ?: j.optJSONArray("results") ?: return emptyList()
    return List(arr.length()) { arr.getJSONObject(it) }
}

fun matchDate(e: JSONObject): Date? = try {
    val t = e.optString("strTime").ifBlank { "00:00:00" }
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        .parse(e.optString("dateEvent") + " " + t)
} catch (ex: Exception) { null }

@Composable
fun rememberBarca(): List<String> {
    var lines by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(Unit) {
        var alerted = ""
        while (true) {
            val result = withContext(Dispatchers.IO) {
                val out = mutableListOf<String>()
                var nextEvent: JSONObject? = null
                try {
                    nextEvent = fetchEvents("eventsnext.php").minByOrNull { it.optString("dateEvent") }
                    nextEvent?.let { e ->
                        val d = matchDate(e)?.let { SimpleDateFormat("EEE d MMM HH:mm", Locale.FRANCE).format(it) } ?: e.optString("dateEvent")
                        out.add("Prochain : ${e.optString("strHomeTeam")} - ${e.optString("strAwayTeam")}  •  $d")
                    }
                } catch (ex: Exception) {}
                try {
                    fetchEvents("eventslast.php").maxByOrNull { it.optString("dateEvent") }?.let { e ->
                        out.add("Dernier : ${e.optString("strHomeTeam")} ${e.optString("intHomeScore")}-${e.optString("intAwayScore")} ${e.optString("strAwayTeam")}")
                    }
                } catch (ex: Exception) {}
                Pair(out, nextEvent)
            }
            lines = result.first
            // alerte 20 min avant le coup d'envoi
            result.second?.let { e ->
                val diff = (matchDate(e)?.time ?: 0L) - System.currentTimeMillis()
                val id = e.optString("idEvent")
                if (diff in 0..20 * 60_000L && id != alerted) {
                    alerted = id
                    AppState.banner = "⚽ ${e.optString("strHomeTeam")} - ${e.optString("strAwayTeam")} dans ${diff / 60_000} min !"
                }
            }
            delay(if (lines.isEmpty()) 60_000L else 5 * 60_000L)
        }
    }
    return lines
}

fun wallpaperRequest(ctx: Context, url: String): ImageRequest =
    ImageRequest.Builder(ctx).data(url)
        .apply { if (LITE_MODE) { size(1280, 720); allowRgb565(true) } }
        .build()

fun localIp(): String = try {
    NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress ?: "?"
} catch (e: Exception) { "?" }

// =====================================================================
//  CONTRÔLE DEPUIS LE TÉLÉPHONE (mini serveur web sur la télé)
// =====================================================================
object Remote {
    private var server: ServerSocket? = null
    private const val PAGE = """<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{background:#111;color:#fff;font-family:sans-serif;text-align:center}a,button{display:block;margin:14px auto;padding:18px;width:80%;font-size:22px;border-radius:12px;background:#ff2bd6;color:#fff;border:0;text-decoration:none}input{width:75%;padding:14px;font-size:20px}</style></head><body><h2>Ma TV</h2><a href="/theme">Changer de thème</a><a href="/bg">Changer de fond</a><a href="/next">Image suivante</a><a href="/saver">Écran de veille</a><form action="/msg"><input name="t" placeholder="Message sur la télé"><button>Envoyer</button></form></body></html>"""

    fun start() {
        if (server != null) return
        thread(isDaemon = true) {
            try {
                val s = ServerSocket(REMOTE_PORT)
                server = s
                while (true) {
                    val c = s.accept()
                    thread(isDaemon = true) { handle(c) }
                }
            } catch (ignored: Exception) { server = null }
        }
    }

    private fun handle(c: Socket) {
        try {
            c.use {
                val line = BufferedReader(InputStreamReader(it.getInputStream())).readLine() ?: return
                val path = line.split(" ").getOrNull(1) ?: "/"
                val cmd = path.substringBefore("?")
                val arg = path.substringAfter("t=", "")
                var redirect = true
                when (cmd) {
                    "/theme" -> AppState.themeIndex = (AppState.themeIndex + 1) % THEMES.size
                    "/bg" -> AppState.bgMode = (AppState.bgMode + 1) % BG_MODES.size
                    "/next" -> AppState.wallpaperSkip++
                    "/saver" -> AppState.saver = true
                    "/msg" -> AppState.banner = URLDecoder.decode(arg, "UTF-8").take(120)
                    else -> redirect = false
                }
                val out = it.getOutputStream()
                if (redirect) {
                    out.write("HTTP/1.1 303 See Other\r\nLocation: /\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                } else {
                    val body = PAGE.toByteArray(Charsets.UTF_8)
                    out.write(
                        ("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\n" +
                            "Content-Length: ${body.size}\r\nConnection: close\r\n\r\n").toByteArray()
                    )
                    out.write(body)
                }
                out.flush()
            }
        } catch (ignored: Exception) {}
    }
}

// =====================================================================
//  ACTIVITÉ
// =====================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val st = Store(this)
        AppState.themeIndex = st.int("theme").coerceIn(0, THEMES.lastIndex)
        AppState.bgMode = st.int("bg").coerceIn(0, BG_MODES.lastIndex)
        Remote.start()
        setContent { HomeScreen() }
    }

    override fun onResume() {
        super.onResume()
        AppState.saver = false
        AppState.resumeTick++
    }
}

// =====================================================================
//  ÉCRAN D'ACCUEIL
// =====================================================================
@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val store = remember { Store(ctx) }
    val all = remember { loadApps(ctx) }
    val sounds = remember { Sounds() }
    val audio = remember { ctx.getSystemService(AudioManager::class.java) }
    DisposableEffect(Unit) { onDispose { sounds.release() } }

    var favs by remember { mutableStateOf(store.list("fav")) }
    var hidden by remember { mutableStateOf(store.list("hidden")) }
    var order by remember { mutableStateOf(store.list("order")) }
    var recents by remember { mutableStateOf(store.list("recent")) }
    var tab by remember { mutableStateOf(0) }
    var focusedApp by remember { mutableStateOf<AppItem?>(null) }
    var optionsFor by remember { mutableStateOf<AppItem?>(null) }
    var soundOn by remember { mutableStateOf(false) }
    var lastKey by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) { sounds.boot(); delay(800); soundOn = true }
    LaunchedEffect(AppState.themeIndex, AppState.bgMode) {
        store.putInt("theme", AppState.themeIndex); store.putInt("bg", AppState.bgMode)
    }
    LaunchedEffect(AppState.resumeTick) { lastKey = System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            if (!AppState.saver && System.currentTimeMillis() - lastKey > SCREENSAVER_AFTER_MS) AppState.saver = true
        }
    }
    LaunchedEffect(AppState.banner) { if (AppState.banner != null) { delay(6000); AppState.banner = null } }

    // ----- Liste des applis -----
    val ordered = all.filter { it.pkg !in hidden }.sortedWith(
        compareBy<AppItem>(
            { order.indexOf(it.pkg).let { i -> if (i < 0) Int.MAX_VALUE else i } },
            { it.label.lowercase() }
        )
    )
    val apps = when (tab) {
        1 -> ordered.filter { it.pkg in favs }
        2 -> ordered.filter { it.cat == ApplicationInfo.CATEGORY_GAME }
        3 -> ordered.filter { it.cat == ApplicationInfo.CATEGORY_VIDEO }
        4 -> ordered.filter { it.cat == ApplicationInfo.CATEGORY_AUDIO }
        else -> ordered
    }
    val recentApps = recents.mapNotNull { p -> ordered.find { it.pkg == p } }.take(8)

    val theme = THEMES[AppState.themeIndex]
    val accent by animateColorAsState(
        theme.color ?: focusedApp?.accent ?: NEON_DEFAULT, tween(600), label = "accent"
    )

    val launch: (AppItem) -> Unit = { app ->
        sounds.select()
        recents = (listOf(app.pkg) + recents.filter { it != app.pkg }).take(10)
        store.save("recent", recents)
        val i = ctx.packageManager.getLeanbackLaunchIntentForPackage(app.pkg)
            ?: ctx.packageManager.getLaunchIntentForPackage(app.pkg)
        i?.let { ctx.startActivity(it) }
    }

    val firstTile = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(300); try { firstTile.requestFocus() } catch (ignored: Exception) {} }
    BackHandler { }

    Box(
        Modifier
            .fillMaxSize()
            .background(theme.base)
            .onPreviewKeyEvent { e ->
                lastKey = System.currentTimeMillis()
                when {
                    AppState.saver -> { if (e.type == KeyEventType.KeyDown) AppState.saver = false; true }
                    e.type == KeyEventType.KeyDown && e.key == Key.Menu -> {
                        focusedApp?.let { optionsFor = it }; true
                    }
                    else -> false
                }
            }
    ) {
        // Fond : images / vidéo / particules
        when {
            AppState.bgMode == 1 && VIDEO_URLS.isNotEmpty() -> VideoBg(VIDEO_URLS)
            AppState.bgMode != 2 -> Wallpaper(rememberWallpaper())
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Color.Black.copy(alpha = 0.5f))
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width * 0.3f, size.height * 0.8f),
                        radius = size.width * 0.75f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(theme.second.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.5f
                    )
                )
            }
        )
        if (AppState.bgMode == 2) Particles(accent)

        InfoPanel(accent, Modifier.align(Alignment.TopEnd).padding(40.dp))
        BarcaCard(accent, Modifier.align(Alignment.TopCenter).padding(top = 110.dp))

        // Grand aperçu de l'appli sélectionnée
        Column(Modifier.align(Alignment.TopStart).padding(start = 80.dp, top = 40.dp)) {
            Crossfade(targetState = focusedApp, animationSpec = tween(400), label = "preview") { a ->
                if (a != null) {
                    Image(
                        bitmap = a.image, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(300.dp, 169.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(2.dp, accent, RoundedCornerShape(18.dp))
                    )
                } else Spacer(Modifier.size(300.dp, 169.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                (if (focusedApp?.pkg in favs) "★ " else "") + (focusedApp?.label ?: ""),
                style = TextStyle(
                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    shadow = Shadow(accent, blurRadius = 30f)
                )
            )
        }

        // Onglets + rangée d'applis + récents
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = 20.dp)) {
            Row(
                Modifier.padding(start = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TABS.forEachIndexed { i, name ->
                    NeonButton(name, accent, selected = tab == i) { tab = i }
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 80.dp, vertical = 30.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    NeonButton("⚡ Menu", accent, Modifier.size(120.dp, 112.dp)) { AppState.menuOpen = true }
                }
                itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    AppTile(
                        app, if (index == 0) firstTile else null,
                        onFocus = { focusedApp = app; if (soundOn) sounds.move() },
                        onClick = { launch(app) }
                    )
                }
            }
            if (recentApps.isNotEmpty()) {
                Text("Récents", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp,
                    modifier = Modifier.padding(start = 80.dp, bottom = 6.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 80.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(recentApps, key = { _, a -> "r" + a.pkg }) { _, app ->
                        AppTile(
                            app, null, 140.dp, 79.dp,
                            onFocus = { focusedApp = app; if (soundOn) sounds.move() },
                            onClick = { launch(app) }
                        )
                    }
                }
            }
        }

        // Notification animée (envoyée depuis le téléphone)
        AnimatedVisibility(
            visible = AppState.banner != null,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            Text(
                "🔔  ${AppState.banner ?: ""}", color = Color.White, fontSize = 24.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0xE6101020))
                    .border(2.dp, accent, RoundedCornerShape(30.dp))
                    .padding(horizontal = 32.dp, vertical = 14.dp)
            )
        }

        if (AppState.saver) ScreenSaver(accent)
    }

    // ----- Menu rapide -----
    if (AppState.menuOpen) {
        Dialog(onDismissRequest = { AppState.menuOpen = false }) {
            Panel("Menu rapide", accent) { first ->
                NeonButton("🔉 Volume −", accent, requester = first) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                }
                NeonButton("🔊 Volume +", accent) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                }
                NeonButton("📶 Wi-Fi", accent) { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                NeonButton("⚙ Paramètres", accent) { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                NeonButton("🎨 Thème : ${theme.name}", accent) {
                    AppState.themeIndex = (AppState.themeIndex + 1) % THEMES.size
                }
                NeonButton("🖼 Fond : ${BG_MODES[AppState.bgMode]}", accent) {
                    AppState.bgMode = (AppState.bgMode + 1) % BG_MODES.size
                }
                NeonButton("⏭ Image suivante", accent) { AppState.wallpaperSkip++ }
                NeonButton("🌙 Veille (horloge)", accent) { AppState.menuOpen = false; AppState.saver = true }
                NeonButton("👁 Réafficher les applis masquées", accent) {
                    hidden = emptyList(); store.save("hidden", hidden)
                }
                Text("📱 Téléphone : http://${localIp()}:$REMOTE_PORT", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }
    }

    // ----- Options d'une appli (touche Menu de la télécommande) -----
    optionsFor?.let { app ->
        Dialog(onDismissRequest = { optionsFor = null }) {
            Panel(app.label, accent) { first ->
                NeonButton(if (app.pkg in favs) "★ Retirer des favoris" else "☆ Ajouter aux favoris", accent, requester = first) {
                    favs = if (app.pkg in favs) favs - app.pkg else favs + app.pkg
                    store.save("fav", favs); optionsFor = null
                }
                fun move(dir: Int) {
                    val nb = apps.getOrNull(apps.indexOf(app) + dir) ?: return
                    val cur = ordered.map { it.pkg }.toMutableList()
                    val a = cur.indexOf(app.pkg); val b = cur.indexOf(nb.pkg)
                    cur[a] = nb.pkg; cur[b] = app.pkg
                    order = cur; store.save("order", cur)
                }
                NeonButton("◀ Déplacer à gauche", accent) { move(-1) }
                NeonButton("▶ Déplacer à droite", accent) { move(1) }
                NeonButton("🙈 Masquer", accent) {
                    hidden = hidden + app.pkg; store.save("hidden", hidden); optionsFor = null
                }
                NeonButton("Fermer", accent) { optionsFor = null }
            }
        }
    }
}

// =====================================================================
//  COMPOSANTS
// =====================================================================
@Composable
fun BarcaCard(accent: Color, modifier: Modifier) {
    val lines = rememberBarca()
    if (lines.isEmpty()) return
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(2.dp, Color(0xFFA50044), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            "🔵🔴 FC Barcelone",
            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, shadow = Shadow(accent, blurRadius = 20f))
        )
        lines.forEach { Text(it, color = Color.White, fontSize = 16.sp) }
    }
}

@Composable
fun InfoPanel(accent: Color, modifier: Modifier) {
    val now = rememberNow()
    val weather = rememberWeather()
    val network = rememberNetwork()
    val rates = rememberRates()
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(
            SimpleDateFormat("HH:mm", Locale.FRANCE).format(now),
            style = TextStyle(
                color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold,
                shadow = Shadow(accent, blurRadius = 28f)
            )
        )
        Text(
            SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text("$weather   •   $network", color = Color.White, fontSize = 20.sp)
        if (rates.isNotEmpty()) Text(rates, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
    }
}

@Composable
fun ScreenSaver(accent: Color) {
    val now = rememberNow()
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Particles(accent)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("HH:mm", Locale.FRANCE).format(now),
                style = TextStyle(
                    color = Color.White, fontSize = 160.sp, fontWeight = FontWeight.Bold,
                    shadow = Shadow(accent, blurRadius = 60f)
                )
            )
            Text(
                SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.8f), fontSize = 32.sp
            )
        }
    }
}

@Composable
fun Particles(color: Color, count: Int = if (LITE_MODE) 12 else 60) {
    val t by rememberInfiniteTransition(label = "p").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "t"
    )
    val seeds = remember {
        List(count) {
            floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextInt(1, 4).toFloat(), 1f + Random.nextFloat() * 3f)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        seeds.forEach { s ->
            val y = ((s[1] - t * s[2]) % 1f + 1f) % 1f
            val x = s[0] + 0.02f * sin((t * 3f * s[2] + s[0]) * 6.2832f)
            drawCircle(color.copy(alpha = 0.25f + 0.05f * s[3]), s[3] * 2.5f, Offset(x * size.width, y * size.height))
        }
    }
}

@Composable
fun Wallpaper(url: String?) {
    val ctx = LocalContext.current
    Crossfade(targetState = url, animationSpec = tween(if (LITE_MODE) 700 else 1500), label = "wallpaper") { u ->
        if (u != null) {
            AsyncImage(
                model = wallpaperRequest(ctx, u),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun VideoBg(urls: List<String>) {
    var i by remember { mutableStateOf(0) }
    AndroidView(
        factory = { c ->
            VideoView(c).apply {
                setOnPreparedListener { it.setVolume(0f, 0f) }
                setOnCompletionListener { i = (i + 1) % urls.size }
            }
        },
        update = { v ->
            val u = urls[i]
            if (v.tag != u) { v.tag = u; v.setVideoURI(Uri.parse(u)); v.start() }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun NeonButton(
    text: String, accent: Color, modifier: Modifier = Modifier,
    selected: Boolean = false, requester: FocusRequester? = null, onClick: () -> Unit
) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { f = it.isFocused }
            .clip(shape)
            .background(if (f) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = if (selected) 0.2f else 0.08f))
            .border(if (f) 2.dp else 0.dp, accent, shape)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun Panel(title: String, accent: Color, content: @Composable ColumnScope.(FocusRequester) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); try { first.requestFocus() } catch (ignored: Exception) {} }
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF00A0A18))
            .border(2.dp, accent, RoundedCornerShape(20.dp))
            .padding(28.dp)
            .width(400.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        content(first)
    }
}

@Composable
fun AppTile(
    app: AppItem, requester: FocusRequester?,
    w: Dp = 200.dp, h: Dp = 112.dp,
    onFocus: () -> Unit, onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isFocused) 1.25f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val shape = RoundedCornerShape(16.dp)
    Image(
        bitmap = app.image,
        contentDescription = app.label,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(w, h)
            .scale(scale)
            .shadow(if (isFocused && !LITE_MODE) 28.dp else 0.dp, shape, ambientColor = app.accent, spotColor = app.accent)
            .clip(shape)
            .border(if (isFocused) 3.dp else 0.dp, app.accent, shape)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus() }
            .clickable { onClick() }
    )
}
