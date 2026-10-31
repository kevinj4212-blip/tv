package com.exemple.ps5launcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.content.Intent
import android.app.role.RoleManager
import android.content.pm.ApplicationInfo
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
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
val LITE_MODE: Boolean get() = AppState.fx == 0   // effets légers (réglage dans Menu > Réglages)
const val BARCA_ID = "133739"             // FC Barcelone sur TheSportsDB (à vérifier, voir message)
const val SPORTSDB_KEY = "123"            // clé publique gratuite de TheSportsDB
val VIDEO_URLS = listOf<String>()         // liens directs .mp4 (optionnel) pour le fond vidéo

data class Theme(val name: String, val color: Color?, val second: Color, val base: Color)

val THEMES = listOf(
    Theme("Auto (couleur de l'appli)", null, Color(0xFFFF2BD6), Color(0xFF05010F)),
    Theme("Néon rose", Color(0xFFFF2BD6), Color(0xFF7C4DFF), Color(0xFF0A0212)),
    Theme("Matrix", Color(0xFF00FF66), Color(0xFF00B3FF), Color(0xFF010A04)),
    Theme("Glace", Color(0xFF7DD3FF), Color(0xFFB388FF), Color(0xFF020A14)),
    Theme("Barça", Color(0xFFA50044), Color(0xFF004D98), Color(0xFF020818)),
    Theme("Naruto orange", Color(0xFFFF8A00), Color(0xFF1565C0), Color(0xFF0A0500)),
    Theme("Coucher de soleil", Color(0xFFFF5E62), Color(0xFFFFB300), Color(0xFF12060A)),
    Theme("Violet gaming", Color(0xFF9C4DFF), Color(0xFF00E5FF), Color(0xFF070212)),
    Theme("Rouge sang", Color(0xFFFF1744), Color(0xFF880E4F), Color(0xFF0D0204)),
    Theme("Or royal", Color(0xFFFFC400), Color(0xFF6D4C41), Color(0xFF0B0802)),
    Theme("Cyan électrique", Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF01080F)),
    Theme("Vert toxique", Color(0xFFB2FF59), Color(0xFF00BFA5), Color(0xFF030A02)),
    Theme("Lave", Color(0xFFFF6D00), Color(0xFFD50000), Color(0xFF0D0300)),
    Theme("Océan", Color(0xFF00B0FF), Color(0xFF304FFE), Color(0xFF010612)),
    Theme("Forêt", Color(0xFF00C853), Color(0xFF76FF03), Color(0xFF020A05)),
    Theme("Sakura", Color(0xFFFF80AB), Color(0xFFE040FB), Color(0xFF12040A)),
    Theme("Galaxie", Color(0xFF7C4DFF), Color(0xFFFF4081), Color(0xFF05020F)),
    Theme("Cyberpunk", Color(0xFFFFEA00), Color(0xFFFF00E5), Color(0xFF08000F)),
    Theme("Rétro 80s", Color(0xFFFF2975), Color(0xFF00F0FF), Color(0xFF0B0224)),
    Theme("Menthe", Color(0xFF64FFDA), Color(0xFF18FFFF), Color(0xFF021210)),
    Theme("Glace polaire", Color(0xFFE0F7FA), Color(0xFF80DEEA), Color(0xFF03101A)),
    Theme("Feu bleu", Color(0xFF2979FF), Color(0xFFFF6D00), Color(0xFF020610)),
    Theme("Blanc pur", Color(0xFFFFFFFF), Color(0xFFB0BEC5), Color(0xFF0A0A0A)),
    Theme("Mandarine", Color(0xFFFFA000), Color(0xFFFF5722), Color(0xFF0F0700)),
    Theme("Raisin", Color(0xFFAB47BC), Color(0xFF5C6BC0), Color(0xFF0B0310)),
    Theme("Émeraude", Color(0xFF00E676), Color(0xFF1DE9B6), Color(0xFF01100A)),
    Theme("Personnalisé", Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF000000)),
)
val BG_MODES = listOf("Images", "Vidéo", "Particules", "Mes photos")
val PARTICLE_STYLES = listOf("Étoiles", "Pluie néon", "Neige", "Bulles", "Lucioles", "Matrix")
val DIM_LEVELS = listOf(0.08f, 0.25f, 0.45f)
val DIM_NAMES = listOf("Lumineux", "Normal", "Sombre")
val SAVER_BGS = listOf("Mes photos", "Animés", "Particules")

val QUOTES = listOf(
    "Chaque jour est une nouvelle chance de devenir plus fort.",
    "Les rêves ne meurent pas, on les abandonne.",
    "Tombe sept fois, relève-toi huit.",
    "Le travail bat le talent quand le talent ne travaille pas.",
    "Ce n'est pas la vitesse qui compte, mais la direction.",
    "Un guerrier ne s'arrête jamais de s'entraîner.",
    "La patience est la clé de tout.",
    "Crois en toi, même quand personne d'autre ne le fait.",
    "Le meilleur moment pour commencer, c'est maintenant.",
    "La vraie force vient de ceux que l'on protège.",
    "Sois la meilleure version de toi-même.",
    "Les petites victoires construisent les grands succès.",
    "Rien n'est impossible à celui qui ne renonce pas.",
    "Le courage, c'est d'avancer malgré la peur.",
    "Après la pluie, le beau temps.",
    "Un pas après l'autre, on va très loin.",
    "Ton seul adversaire, c'est celui que tu étais hier.",
    "La discipline est le pont entre les objectifs et les résultats.",
    "Vise la lune : même si tu rates, tu atterriras parmi les étoiles.",
    "L'important n'est pas la chute, mais la façon de se relever.",
    "Fais-le avec passion ou ne le fais pas.",
    "Més que un club.",
    "Visca el Barça !",
    "Le talent gagne des matchs, le collectif gagne des titres.",
    "Garde la tête haute et le cœur ouvert.",
    "Chaque champion a un jour été un débutant.",
    "Ce que tu fais aujourd'hui construit ce que tu seras demain.",
    "Même la nuit la plus sombre finit par se lever.",
)

data class WallTheme(val name: String, val query: String, val cats: String)

val WALL_THEMES = listOf(
    WallTheme("Mélange animés + dessins animés", "naruto|one piece|dragon ball|demon slayer|attack on titan|jujutsu kaisen|bleach|my hero academia|hunter x hunter|studio ghibli|cartoon|simpsons|adventure time|spongebob", "111"),
    WallTheme("Tous les animés", "", "010"),
    WallTheme("Naruto", "naruto", "010"),
    WallTheme("One Piece", "one piece", "010"),
    WallTheme("Dragon Ball", "dragon ball", "010"),
    WallTheme("Demon Slayer", "demon slayer", "010"),
    WallTheme("Attack on Titan", "attack on titan", "010"),
    WallTheme("Jujutsu Kaisen", "jujutsu kaisen", "010"),
    WallTheme("Bleach", "bleach", "010"),
    WallTheme("My Hero Academia", "my hero academia", "010"),
    WallTheme("Hunter x Hunter", "hunter x hunter", "010"),
    WallTheme("Fullmetal Alchemist", "fullmetal alchemist", "010"),
    WallTheme("Death Note", "death note", "010"),
    WallTheme("Tokyo Ghoul", "tokyo ghoul", "010"),
    WallTheme("One Punch Man", "one punch man", "010"),
    WallTheme("Sword Art Online", "sword art online", "010"),
    WallTheme("Fairy Tail", "fairy tail", "010"),
    WallTheme("Black Clover", "black clover", "010"),
    WallTheme("Chainsaw Man", "chainsaw man", "010"),
    WallTheme("Spy x Family", "spy x family", "010"),
    WallTheme("Haikyuu", "haikyuu", "010"),
    WallTheme("Evangelion", "evangelion", "010"),
    WallTheme("Cowboy Bebop", "cowboy bebop", "010"),
    WallTheme("Studio Ghibli", "studio ghibli", "010"),
    WallTheme("Pokémon", "pokemon", "010"),
    WallTheme("Dessins animés (cartoons)", "cartoon|looney tunes|tom and jerry|simpsons|futurama|adventure time|spongebob|rick and morty|gravity falls|steven universe", "111"),
    WallTheme("Disney / Pixar", "disney|pixar|frozen|lion king", "111"),
    WallTheme("Super-héros (comics)", "spiderman|batman|superman|avengers", "111"),
    WallTheme("Chibi / Kawaii", "chibi|kawaii", "010"),
    WallTheme("Pixel art", "pixel art", "111"),
    WallTheme("Néon", "neon", "111"),
    WallTheme("Cyberpunk (ville)", "cyberpunk", "111"),
)

data class City(val name: String, val lat: Double, val lon: Double)

val CITIES = listOf(
    City("Abidjan (Koumassi)", 5.30, -3.95), City("Bouaké", 7.69, -5.03), City("Yamoussoukro", 6.82, -5.28),
    City("San-Pédro", 4.75, -6.65), City("Korhogo", 9.46, -5.63), City("Dakar", 14.69, -17.44),
    City("Accra", 5.60, -0.19), City("Lagos", 6.52, 3.38), City("Casablanca", 33.57, -7.59),
    City("Paris", 48.85, 2.35), City("Bruxelles", 50.85, 4.35), City("Barcelone", 41.39, 2.17),
    City("Montréal", 45.50, -73.57),
)

val FX_NAMES = listOf("Léger", "Normal", "Max")
val WALL_DELAYS = listOf(10, 5, 20, 30, 60)
val TILE_NAMES = listOf("Moyenne", "Petite", "Grande")
val TILE_SCALES = listOf(1f, 0.85f, 1.1f)
val FOCUS_STYLES = listOf("PS5", "Simple", "Aucune")
val SAVER_MINUTES = listOf(3, 1, 5, 10, 0)
const val CUSTOM_NAME = "Personnalisé"
val TABS = listOf("Tout", "Favoris", "Jeux", "Vidéo", "Musique")

fun glow(c: Color, r: Float): Shadow? = if (LITE_MODE) null else Shadow(c, blurRadius = r)

fun hsvColor(h: Float, sat: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, sat, v)))

fun resolveTheme(i: Int): Theme {
    val t = THEMES[i]
    if (t.name != CUSTOM_NAME) return t
    val h = AppState.hue.toFloat()
    return Theme(CUSTOM_NAME, hsvColor(h, 0.9f, 1f), hsvColor((h + 50f) % 360f, 0.8f, 1f), hsvColor(h, 0.6f, 0.07f))
}

fun timeFormat(): String = when {
    AppState.h12 && AppState.hideSec -> "hh:mm a"
    AppState.h12 -> "hh:mm:ss a"
    AppState.hideSec -> "HH:mm"
    else -> "HH:mm:ss"
}

fun onOff(b: Boolean) = if (b) "oui" else "non"

fun saverLabel(i: Int): String = if (SAVER_MINUTES[i] == 0) "jamais" else SAVER_MINUTES[i].toString() + " min"

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
    var wallIndex by mutableStateOf(0)
    var particleStyle by mutableStateOf(0)
    var dimIndex by mutableStateOf(0)
    var photoTick by mutableStateOf(0)
    var saverBg by mutableStateOf(0)
    var fx by mutableStateOf(0)
    var delayIndex by mutableStateOf(0)
    var tileIndex by mutableStateOf(0)
    var focusStyle by mutableStateOf(0)
    var hue by mutableStateOf(0)
    var h12 by mutableStateOf(false)
    var hideSec by mutableStateOf(false)
    var hideBarca by mutableStateOf(false)
    var hideRates by mutableStateOf(false)
    var mute by mutableStateOf(false)
    var cityIndex by mutableStateOf(0)
    var saverDelay by mutableStateOf(0)

    private fun b(x: Boolean) = if (x) 1 else 0
    private val keys = listOf(
        "theme", "bg", "wall", "pstyle", "dim", "sbg", "fx", "delay", "tile", "focus",
        "hue", "h12", "hidesec", "hidebarca", "hiderates", "mute", "city", "sdelay"
    )

    fun all(): List<Int> = listOf(
        themeIndex, bgMode, wallIndex, particleStyle, dimIndex, saverBg, fx, delayIndex, tileIndex, focusStyle,
        hue, b(h12), b(hideSec), b(hideBarca), b(hideRates), b(mute), cityIndex, saverDelay
    )

    fun save(st: Store) {
        val v = all()
        keys.forEachIndexed { i, k -> st.putInt(k, v[i]) }
    }

    fun load(st: Store) {
        themeIndex = st.int("theme").coerceIn(0, THEMES.lastIndex)
        bgMode = st.int("bg").coerceIn(0, BG_MODES.lastIndex)
        wallIndex = st.int("wall").coerceIn(0, WALL_THEMES.lastIndex)
        particleStyle = st.int("pstyle").coerceIn(0, PARTICLE_STYLES.lastIndex)
        dimIndex = st.int("dim").coerceIn(0, DIM_LEVELS.lastIndex)
        saverBg = st.int("sbg").coerceIn(0, SAVER_BGS.lastIndex)
        fx = st.int("fx").coerceIn(0, FX_NAMES.lastIndex)
        delayIndex = st.int("delay").coerceIn(0, WALL_DELAYS.lastIndex)
        tileIndex = st.int("tile").coerceIn(0, TILE_SCALES.lastIndex)
        focusStyle = st.int("focus").coerceIn(0, FOCUS_STYLES.lastIndex)
        hue = st.int("hue").coerceIn(0, 359)
        h12 = st.int("h12") == 1
        hideSec = st.int("hidesec") == 1
        hideBarca = st.int("hidebarca") == 1
        hideRates = st.int("hiderates") == 1
        mute = st.int("mute") == 1
        cityIndex = st.int("city").coerceIn(0, CITIES.lastIndex)
        saverDelay = st.int("sdelay").coerceIn(0, SAVER_MINUTES.lastIndex)
    }
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
    private val rate = 22050

    private fun build(
        freqs: List<Double>, ms: Int,
        usage: Int = AudioAttributes.USAGE_ASSISTANCE_SONIFICATION, loud: Double = 12000.0
    ): AudioTrack {
        val n = rate * ms / 1000
        val segLen = n / freqs.size
        val data = ShortArray(n) { i ->
            val seg = minOf(i / segLen, freqs.size - 1)
            val j = i - seg * segLen
            val env = exp(-5.0 * j / segLen) * minOf(1.0, j / 120.0)
            (sin(2 * PI * freqs[seg] * i / rate) * env * loud).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
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
    LaunchedEffect(AppState.cityIndex) {
        while (true) {
            text = withContext(Dispatchers.IO) {
                try {
                    val url = URL(
                        "https://api.open-meteo.com/v1/forecast?latitude=${CITIES[AppState.cityIndex].lat}&longitude=${CITIES[AppState.cityIndex].lon}" +
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

@Suppress("DEPRECATION")
fun wifiName(ctx: Context): String = try {
    val wm = ctx.applicationContext.getSystemService(WifiManager::class.java)
    val ssid = wm.connectionInfo?.ssid?.trim('"') ?: ""
    if (ssid.isNotBlank() && ssid != "<unknown ssid>") "Wi-Fi : $ssid" else "Wi-Fi"
} catch (ex: Exception) { "Wi-Fi" }

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
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> wifiName(ctx)
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
    LaunchedEffect(AppState.wallpaperSkip, AppState.wallIndex) {
        var queue = emptyList<String>()
        while (true) {
            if (queue.isEmpty()) {
                queue = withContext(Dispatchers.IO) {
                    try {
                        val url = URL(
                            "https://wallhaven.cc/api/v1/search?q=" + URLEncoder.encode(WALL_THEMES[AppState.wallIndex].query.split("|").random(), "UTF-8") +
                                "&categories=" + WALL_THEMES[AppState.wallIndex].cats + "&purity=100&sorting=random&atleast=1920x1080&page=1"
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
            delay(WALL_DELAYS[AppState.delayIndex] * 1000L)
        }
    }
    return current
}

// ---------- Barça (TheSportsDB, gratuit) ----------
@Volatile private var barcaIdCache: String? = null

// Cherche automatiquement l'identifiant du FC Barcelone (football, Espagne, La Liga)
fun barcaId(): String {
    barcaIdCache?.let { return it }
    try {
        val j = JSONObject(URL("https://www.thesportsdb.com/api/v1/json/$SPORTSDB_KEY/searchteams.php?t=Barcelona").readText())
        val arr = j.optJSONArray("teams")
        if (arr != null) {
            val teams = List(arr.length()) { arr.getJSONObject(it) }
            val liga = teams.filter {
                it.optString("strSport") == "Soccer" && it.optString("strCountry") == "Spain" &&
                    it.optString("strLeague").contains("La Liga", ignoreCase = true)
            }
            val best = liga.firstOrNull { it.optString("strTeam") == "Barcelona" || it.optString("strTeam") == "FC Barcelona" }
                ?: liga.firstOrNull()
            val id = best?.optString("idTeam").orEmpty()
            if (id.isNotEmpty()) { barcaIdCache = id; return id }
        }
    } catch (ex: Exception) {}
    return BARCA_ID
}

fun fetchEvents(path: String, id: String): List<JSONObject> {
    val j = JSONObject(URL("https://www.thesportsdb.com/api/v1/json/$SPORTSDB_KEY/$path?id=$id").readText())
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
                var err = ""
                val id = barcaId()
                try {
                    nextEvent = fetchEvents("eventsnext.php", id).minByOrNull { it.optString("dateEvent") }
                    nextEvent?.let { e ->
                        val d = matchDate(e)?.let { SimpleDateFormat("EEE d MMM HH:mm", Locale.FRANCE).format(it) } ?: e.optString("dateEvent")
                        out.add("Prochain : ${e.optString("strHomeTeam")} - ${e.optString("strAwayTeam")}  •  $d")
                    }
                } catch (ex: Exception) {
                    err = ex.javaClass.simpleName + " " + (ex.message ?: "").take(50)
                }
                try {
                    fetchEvents("eventslast.php", id).maxByOrNull { it.optString("dateEvent") }?.let { e ->
                        out.add("Dernier : ${e.optString("strHomeTeam")} ${e.optString("intHomeScore")}-${e.optString("intAwayScore")} ${e.optString("strAwayTeam")}")
                    }
                } catch (ex: Exception) {
                    if (err.isEmpty()) err = ex.javaClass.simpleName + " " + (ex.message ?: "").take(50)
                }
                val ok = out.isNotEmpty()
                if (!ok) out.add(if (err.isNotEmpty()) "Erreur : $err" else "Aucun match trouvé (équipe $id)")
                Triple(out, nextEvent, ok)
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
            delay(if (result.third) 5 * 60_000L else 60_000L)
        }
    }
    return lines
}

fun wallpaperRequest(ctx: Context, data: Any): ImageRequest =
    ImageRequest.Builder(ctx).data(data)
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
    @Volatile var photoDir: File? = null
    @Volatile var quotesFile: File? = null

    fun addQuote(q: String) {
        val f = quotesFile ?: return
        val t = q.replace("\n", " ").trim().take(200)
        if (t.isNotEmpty()) { f.appendText(t + "\n"); AppState.photoTick++ }
    }

    private const val PAGE = """<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{background:#111;color:#fff;font-family:sans-serif;text-align:center}a,button{display:block;margin:12px auto;padding:16px;width:80%;font-size:20px;border-radius:12px;background:#ff2bd6;color:#fff;border:0;text-decoration:none}input[type=text]{width:75%;padding:14px;font-size:20px}input[type=file]{display:block;margin:10px auto;font-size:16px}</style></head><body><h2>Ma TV</h2><a href="/theme">Couleurs suivantes</a><a href="/bg">Changer de fond</a><a href="/wall">Thème d'images suivant</a><a href="/style">Particules suivantes</a><a href="/next">Image suivante</a><a href="/saver">Écran de veille</a><form action="/msg"><input type="text" name="t" placeholder="Message sur la télé"><button>Envoyer</button></form><form action="/addquote"><input type="text" name="t" placeholder="Ta citation pour la veille"><button>Ajouter la citation</button></form><h3>Envoyer des photos sur la télé</h3><input type="file" id="f" accept="image/*" multiple><button onclick="up()">Envoyer</button><p id="s"></p><script>async function up(){var fs=document.getElementById('f').files,s=document.getElementById('s');for(var i=0;i<fs.length;i++){s.textContent='Envoi '+(i+1)+'/'+fs.length+'...';await fetch('/upload?n='+Date.now()+'_'+i,{method:'POST',body:fs[i]});}s.textContent='Terminé ('+fs.length+' photo(s))';}</script></body></html>"""

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

    private fun readLine(i: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = i.read()
            if (b == -1) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) return sb.toString().trimEnd('\r')
            sb.append(b.toChar())
        }
    }

    private fun handle(c: Socket) {
        try {
            c.use {
                val input = BufferedInputStream(it.getInputStream())
                val line = readLine(input) ?: return
                var len = 0L
                while (true) {
                    val h = readLine(input) ?: break
                    if (h.isEmpty()) break
                    if (h.startsWith("Content-Length:", ignoreCase = true)) {
                        len = h.substringAfter(":").trim().toLongOrNull() ?: 0L
                    }
                }
                val path = line.split(" ").getOrNull(1) ?: "/"
                val cmd = path.substringBefore("?")
                val arg = path.substringAfter("t=", "")
                val out = it.getOutputStream()

                if (cmd == "/upload") {
                    val dir = photoDir
                    var ok = false
                    if (dir != null && len in 1..25_000_000L) {
                        dir.mkdirs()
                        val name = path.substringAfter("n=", "x")
                            .filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(40)
                        File(dir, "p_$name.img").outputStream().use { os ->
                            val buf = ByteArray(8192)
                            var left = len
                            while (left > 0) {
                                val n = input.read(buf, 0, minOf(buf.size.toLong(), left).toInt())
                                if (n < 0) break
                                os.write(buf, 0, n)
                                left -= n
                            }
                        }
                        AppState.photoTick++
                        ok = true
                    }
                    val status = if (ok) "200 OK" else "400 Bad Request"
                    out.write(("HTTP/1.1 $status\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok").toByteArray())
                    out.flush()
                    return
                }

                var redirect = true
                when (cmd) {
                    "/theme" -> AppState.themeIndex = (AppState.themeIndex + 1) % THEMES.size
                    "/bg" -> AppState.bgMode = (AppState.bgMode + 1) % BG_MODES.size
                    "/wall" -> AppState.wallIndex = (AppState.wallIndex + 1) % WALL_THEMES.size
                    "/style" -> AppState.particleStyle = (AppState.particleStyle + 1) % PARTICLE_STYLES.size
                    "/next" -> AppState.wallpaperSkip++
                    "/saver" -> AppState.saver = true
                    "/msg" -> AppState.banner = URLDecoder.decode(arg, "UTF-8").take(120)
                    "/addquote" -> addQuote(URLDecoder.decode(arg, "UTF-8"))
                    else -> redirect = false
                }
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
    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val st = Store(this)
        AppState.load(st)
        Remote.photoDir = File(filesDir, "photos").apply { mkdirs() }
        Remote.quotesFile = File(filesDir, "quotes.txt")
        Remote.start()
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            try { permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) } catch (ignored: Exception) {}
        }
        setContent {
            val dm = resources.displayMetrics
            val base = LocalDensity.current
            val target = minOf(dm.widthPixels, dm.heightPixels) / 720f
            CompositionLocalProvider(LocalDensity provides Density(target, base.fontScale)) {
                HomeScreen()
            }
        }
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
    var sub by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { if (!AppState.mute) sounds.boot(); delay(800); soundOn = true }
    LaunchedEffect(AppState.all()) { AppState.save(store) }
    LaunchedEffect(AppState.resumeTick) { lastKey = System.currentTimeMillis() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            val mins = SAVER_MINUTES[AppState.saverDelay]
            if (mins > 0 && !AppState.saver && System.currentTimeMillis() - lastKey > mins * 60_000L) AppState.saver = true
        }
    }
    LaunchedEffect(AppState.banner) { if (AppState.banner != null) { delay(6000); AppState.banner = null } }

    // ----- Liste des applis -----
    val ordered = remember(hidden, order) { all.filter { it.pkg !in hidden }.sortedWith(
        compareBy<AppItem>(
            { order.indexOf(it.pkg).let { i -> if (i < 0) Int.MAX_VALUE else i } },
            { it.label.lowercase() }
        )
    ) }
    val apps = when (tab) {
        1 -> ordered.filter { it.pkg in favs }
        2 -> ordered.filter { it.cat == ApplicationInfo.CATEGORY_GAME }
        3 -> ordered.filter { it.cat == ApplicationInfo.CATEGORY_VIDEO }
        4 -> ordered.filter { it.cat == ApplicationInfo.CATEGORY_AUDIO }
        else -> ordered
    }
    val recentApps = recents.mapNotNull { p -> ordered.find { it.pkg == p } }.take(8)

    val theme = resolveTheme(AppState.themeIndex)
    val ts = TILE_SCALES[AppState.tileIndex]
    val accent by animateColorAsState(
        theme.color ?: focusedApp?.accent ?: NEON_DEFAULT, if (LITE_MODE) snap() else tween(600), label = "accent"
    )

    val launch: (AppItem) -> Unit = { app ->
        if (!AppState.mute) sounds.select()
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
        val useVideo = AppState.bgMode == 1 && VIDEO_URLS.isNotEmpty()
        if (useVideo) VideoBg(VIDEO_URLS)
        Box(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        if (!useVideo && !AppState.saver) {
            when (AppState.bgMode) {
                0, 1 -> Wallpaper(rememberWallpaper())
                3 -> Wallpaper(rememberPhotos())
            }
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                drawRect(Color.Black.copy(alpha = DIM_LEVELS[AppState.dimIndex]))
                drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), startY = 0f, endY = size.height * 0.35f))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)), startY = size.height * 0.55f, endY = size.height))
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(size.width * 0.3f, size.height * 0.8f),
                        radius = size.width * 0.75f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(theme.second.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.5f
                    )
                )
            }
        )
        }
        if (AppState.bgMode == 2) Particles(accent)

        InfoPanel(accent, Modifier.align(Alignment.TopEnd).padding(40.dp))
        if (!AppState.hideBarca) BarcaCard(accent, Modifier.align(Alignment.TopCenter).padding(top = 110.dp))

        // Grand aperçu de l'appli sélectionnée
        Column(Modifier.align(Alignment.TopStart).padding(start = 80.dp, top = 40.dp)) {
            Crossfade(targetState = focusedApp, animationSpec = tween(if (LITE_MODE) 1 else 400), label = "preview") { a ->
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
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(horizontal = 14.dp, vertical = 4.dp),
                style = TextStyle(
                    color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    shadow = glow(accent, 30f)
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
                    NeonButton("⚡ Menu", accent, Modifier.size(120.dp * ts, 112.dp * ts)) { AppState.menuOpen = true }
                }
                itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app ->
                    AppTile(
                        app, if (index == 0) firstTile else null,
                        onFocus = { focusedApp = app; if (soundOn && !AppState.mute) sounds.move() },
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
                            app, null, 140.dp * ts, 79.dp * ts,
                            onFocus = { focusedApp = app; if (soundOn && !AppState.mute) sounds.move() },
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton("🔉 Vol −", accent, Modifier.weight(1f), requester = first) {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    }
                    NeonButton("🔊 Vol +", accent, Modifier.weight(1f)) {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    }
                }
                NeonButton("🎨 Apparence", accent) { AppState.menuOpen = false; sub = "look" }
                NeonButton("⚙ Réglages", accent) { AppState.menuOpen = false; sub = "settings" }
                NeonButton("📷 Photos et citations", accent) { AppState.menuOpen = false; sub = "photos" }
                NeonButton("📶 Wi-Fi", accent) { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                NeonButton("⚙ Paramètres de la télé", accent) { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                NeonButton("🌙 Écran de veille", accent) { AppState.menuOpen = false; AppState.saver = true }
                NeonButton("👁 Réafficher les applis masquées", accent) {
                    hidden = emptyList(); store.save("hidden", hidden)
                }
            }
        }
    }

    // ----- Apparence -----
    if (sub == "look") {
        Dialog(onDismissRequest = { sub = "" }) {
            Panel("🎨 Apparence", accent) { first ->
                NeonButton("🎨 Couleurs : ${theme.name}", accent, requester = first, dot = accent) { sub = "colors" }
                NeonButton("🖼 Fond : ${BG_MODES[AppState.bgMode]}", accent) {
                    AppState.bgMode = (AppState.bgMode + 1) % BG_MODES.size
                }
                NeonButton("🎌 Univers d'images : ${WALL_THEMES[AppState.wallIndex].name}", accent) { sub = "walls" }
                NeonButton("✨ Particules : ${PARTICLE_STYLES[AppState.particleStyle]}", accent) {
                    AppState.particleStyle = (AppState.particleStyle + 1) % PARTICLE_STYLES.size
                }
                NeonButton("☀ Luminosité : ${DIM_NAMES[AppState.dimIndex]}", accent) {
                    AppState.dimIndex = (AppState.dimIndex + 1) % DIM_LEVELS.size
                }
                NeonButton("💤 Fond de veille : ${SAVER_BGS[AppState.saverBg]}", accent) {
                    AppState.saverBg = (AppState.saverBg + 1) % SAVER_BGS.size
                }
                NeonButton("🌙 Voir l'écran de veille", accent) { sub = ""; AppState.saver = true }
                NeonButton("⏭ Image suivante", accent) { AppState.wallpaperSkip++ }
                NeonButton("Fermer", accent) { sub = "" }
            }
        }
    }

    // ----- Réglages -----
    if (sub == "settings") {
        Dialog(onDismissRequest = { sub = "" }) {
            Panel("⚙ Réglages", accent) { first ->
                NeonButton("⚡ Effets : ${FX_NAMES[AppState.fx]}", accent, requester = first) {
                    AppState.fx = (AppState.fx + 1) % FX_NAMES.size
                }
                NeonButton("🎬 Animation de sélection : ${FOCUS_STYLES[AppState.focusStyle]}", accent) {
                    AppState.focusStyle = (AppState.focusStyle + 1) % FOCUS_STYLES.size
                }
                NeonButton("🔲 Taille des tuiles : ${TILE_NAMES[AppState.tileIndex]}", accent) {
                    AppState.tileIndex = (AppState.tileIndex + 1) % TILE_SCALES.size
                }
                NeonButton("⏱ Changement de fond : ${WALL_DELAYS[AppState.delayIndex]} s", accent) {
                    AppState.delayIndex = (AppState.delayIndex + 1) % WALL_DELAYS.size
                }
                NeonButton("🕒 Format de l'heure : ${if (AppState.h12) "12 h" else "24 h"}", accent) { AppState.h12 = !AppState.h12 }
                NeonButton("⏲ Afficher les secondes : ${onOff(!AppState.hideSec)}", accent) { AppState.hideSec = !AppState.hideSec }
                NeonButton("⚽ Carte Barça : ${onOff(!AppState.hideBarca)}", accent) { AppState.hideBarca = !AppState.hideBarca }
                NeonButton("💱 Taux de change : ${onOff(!AppState.hideRates)}", accent) { AppState.hideRates = !AppState.hideRates }
                NeonButton("🌦 Ville de la météo : ${CITIES[AppState.cityIndex].name}", accent) { sub = "cities" }
                NeonButton("🔊 Sons : ${onOff(!AppState.mute)}", accent) { AppState.mute = !AppState.mute }
                NeonButton("💤 Veille après : ${saverLabel(AppState.saverDelay)}", accent) {
                    AppState.saverDelay = (AppState.saverDelay + 1) % SAVER_MINUTES.size
                }
                NeonButton("Fermer", accent) { sub = "" }
            }
        }
    }

    // ----- Choix : couleurs, univers d'images, villes -----
    if (sub == "colors") {
        PickDialog(
            "🎨 Couleurs", THEMES.map { it.name }, AppState.themeIndex, accent,
            THEMES.indices.map { resolveTheme(it).color ?: NEON_DEFAULT },
            { AppState.themeIndex = it }, { sub = "look" }
        ) {
            if (theme.name == CUSTOM_NAME) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NeonButton("◀ Teinte", accent, Modifier.weight(1f), dot = accent) { AppState.hue = (AppState.hue + 345) % 360 }
                    NeonButton("Teinte ▶", accent, Modifier.weight(1f), dot = accent) { AppState.hue = (AppState.hue + 15) % 360 }
                }
            }
        }
    }
    if (sub == "walls") {
        PickDialog(
            "🎌 Univers d'images", WALL_THEMES.map { it.name }, AppState.wallIndex, accent, null,
            { AppState.wallIndex = it }, { sub = "look" }
        )
    }
    if (sub == "cities") {
        PickDialog(
            "🌦 Ville de la météo", CITIES.map { it.name }, AppState.cityIndex, accent, null,
            { AppState.cityIndex = it }, { sub = "settings" }
        )
    }

    // ----- Photos et citations -----
    if (sub == "photos") {
        Dialog(onDismissRequest = { sub = "" }) {
            Panel("📷 Photos et citations", accent) { first ->
                val count = remember(AppState.photoTick) { photoFiles().size }
                val nq = remember(AppState.photoTick) { customQuotes().size }
                Text("Sur ton téléphone (même Wi-Fi), ouvre :", color = Color.White, fontSize = 16.sp)
                Text("http://${localIp()}:$REMOTE_PORT", color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Tu y envoies tes photos et tes citations.", color = Color.White, fontSize = 16.sp)
                Text("Photos : $count   •   Citations perso : $nq", color = Color.White, fontSize = 18.sp)
                NeonButton("🗑 Supprimer mes photos", accent, requester = first) {
                    photoFiles().forEach { it.delete() }; AppState.photoTick++
                }
                NeonButton("🗑 Supprimer mes citations", accent) {
                    Remote.quotesFile?.delete(); AppState.photoTick++
                }
                NeonButton("Fermer", accent) { sub = "" }
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
    val lines = rememberBarca().ifEmpty { listOf("Barça : données indisponibles") }
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .border(2.dp, Color(0xFFA50044), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            "🔵🔴 FC Barcelone",
            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, shadow = glow(accent, 20f))
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
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(14.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            SimpleDateFormat(timeFormat(), Locale.FRANCE).format(now),
            style = TextStyle(
                color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold,
                shadow = glow(accent, 28f)
            )
        )
        Text(
            SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text("$weather   •   $network", color = Color.White, fontSize = 20.sp)
        if (rates.isNotEmpty() && !AppState.hideRates) Text(rates, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
    }
}

@Composable
fun ScreenSaver(accent: Color) {
    val now = rememberNow()
    val hasPhotos = remember(AppState.photoTick) { photoFiles().isNotEmpty() }
    val mode = when {
        AppState.saverBg == 2 -> 2
        AppState.saverBg == 0 && hasPhotos -> 0
        else -> 1
    }
    val quotes = remember(AppState.photoTick) { allQuotes() }
    var qi by remember { mutableStateOf(Random.nextInt(1000)) }
    LaunchedEffect(Unit) { while (true) { delay(12_000); qi++ } }
    val quote = quotes[qi % quotes.size]

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            when (mode) {
                0 -> Wallpaper(rememberPhotos())
                1 -> Wallpaper(rememberWallpaper())
            }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (mode == 2) 0.85f else 0.5f)))
        }
        if (mode == 2) Particles(accent)
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat(timeFormat(), Locale.FRANCE).format(now),
                style = TextStyle(
                    color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.Bold,
                    shadow = glow(accent, 50f)
                )
            )
            Text(
                SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() },
                color = Color.White.copy(alpha = 0.85f), fontSize = 28.sp
            )
            Spacer(Modifier.height(30.dp))
            Crossfade(targetState = quote, animationSpec = tween(if (LITE_MODE) 400 else 1200), label = "quote") { q ->
                Text(
                    "« " + q + " »",
                    modifier = Modifier.widthIn(max = 900.dp).padding(horizontal = 48.dp),
                    style = TextStyle(
                        color = Color.White, fontSize = 30.sp, fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center, shadow = glow(accent, 16f)
                    )
                )
            }
        }
    }
}

@Composable
fun Particles(color: Color, count: Int = if (LITE_MODE) 20 else 60) {
    val style = AppState.particleStyle
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
        val w = size.width
        val h = size.height
        seeds.forEach { s ->
            val k = s[2]
            val sz = s[3]
            when (style) {
                1 -> { // pluie néon
                    val y = (s[1] + t * k) % 1f
                    drawLine(color.copy(alpha = 0.55f), Offset(s[0] * w, y * h), Offset(s[0] * w, y * h + 14f * sz), strokeWidth = 2.5f)
                }
                2 -> { // neige
                    val y = (s[1] + t) % 1f
                    val x = s[0] + 0.03f * sin((t * 2f * k + s[0]) * 6.2832f)
                    drawCircle(Color.White.copy(alpha = 0.7f), sz * 2.2f, Offset(x * w, y * h))
                }
                3 -> { // bulles
                    val y = ((s[1] - t) % 1f + 1f) % 1f
                    val x = s[0] + 0.02f * sin((t * 2f * k + s[0]) * 6.2832f)
                    drawCircle(color.copy(alpha = 0.6f), sz * 7f, Offset(x * w, y * h), style = Stroke(width = 2.5f))
                }
                4 -> { // lucioles
                    val x = 0.5f + 0.45f * sin((t * k + s[0]) * 6.2832f)
                    val y = 0.5f + 0.4f * sin((t * (k + 1f) + s[1]) * 6.2832f)
                    val a = 0.25f + 0.5f * (0.5f + 0.5f * sin((t * 3f * k + s[0]) * 6.2832f))
                    drawCircle(color.copy(alpha = a), sz * 3.5f, Offset(x * w, y * h))
                }
                5 -> { // matrix
                    val col = (s[0] * 40f).toInt() / 40f
                    val y = (s[1] + t * k) % 1f
                    for (j in 0..3) {
                        drawCircle(color.copy(alpha = 0.7f - j * 0.17f), 3f, Offset(col * w, ((y - j * 0.025f + 1f) % 1f) * h))
                    }
                }
                else -> { // étoiles
                    val y = ((s[1] - t * k) % 1f + 1f) % 1f
                    val x = s[0] + 0.02f * sin((t * 3f * k + s[0]) * 6.2832f)
                    drawCircle(color.copy(alpha = 0.25f + 0.05f * sz), sz * 2.5f, Offset(x * w, y * h))
                }
            }
        }
    }
}

fun customQuotes(): List<String> =
    Remote.quotesFile?.takeIf { it.exists() }?.readLines()?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

fun allQuotes(): List<String> = QUOTES + customQuotes()

fun photoFiles(): List<File> =
    Remote.photoDir?.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()

@Composable
fun rememberPhotos(): String? {
    var current by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(AppState.wallpaperSkip, AppState.photoTick) {
        var idx = Random.nextInt(1000)
        while (true) {
            val files = photoFiles()
            if (files.isEmpty()) { current = null; delay(3000); continue }
            current = files[idx % files.size].absolutePath
            idx++
            delay(WALL_DELAYS[AppState.delayIndex] * 1000L)
        }
    }
    return current
}

@Composable
fun Wallpaper(url: String?) {
    val ctx = LocalContext.current
    Crossfade(targetState = url, animationSpec = tween(if (LITE_MODE) 700 else 1500), label = "wallpaper") { u ->
        if (u != null) {
            AsyncImage(
                model = wallpaperRequest(ctx, if (u.startsWith("http")) u else File(u)),
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
    selected: Boolean = false, requester: FocusRequester? = null, dot: Color? = null, onClick: () -> Unit
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = Color.White, fontSize = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun PickDialog(
    title: String, items: List<String>, selected: Int, accent: Color, dots: List<Color>?,
    onPick: (Int) -> Unit, onClose: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    Dialog(onDismissRequest = onClose) {
        Panel(title, accent) { first ->
            extra()
            items.forEachIndexed { i, name ->
                NeonButton(
                    name, accent, selected = i == selected,
                    requester = if (i == selected) first else null, dot = dots?.getOrNull(i)
                ) { onPick(i) }
            }
            NeonButton("◀ Retour", accent) { onClose() }
        }
    }
}

@Composable
fun Panel(title: String, accent: Color, content: @Composable ColumnScope.(FocusRequester) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); try { first.requestFocus() } catch (ignored: Exception) {} }
    val view = LocalView.current
    SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xF00A0A18))
            .border(2.dp, accent, RoundedCornerShape(20.dp))
            .padding(28.dp)
            .width(400.dp)
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        content(first)
    }
}

@Composable
fun AppTile(
    app: AppItem, requester: FocusRequester?,
    w: Dp = 200.dp * TILE_SCALES[AppState.tileIndex], h: Dp = 112.dp * TILE_SCALES[AppState.tileIndex],
    onFocus: () -> Unit, onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val style = AppState.focusStyle
    val fx = AppState.fx
    val target = when (style) { 0 -> 1.25f; 1 -> 1.15f; else -> 1f }
    val scale by animateFloatAsState(
        if (isFocused) target else 1f,
        if (fx == 0) tween(120) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    // les voisins s'écartent quand la tuile est sélectionnée (comme sur PS5)
    val gap by animateDpAsState(if (isFocused && style == 0) 22.dp else 0.dp, tween(if (fx == 0) 100 else 220), label = "gap")
    // contour blanc qui pulse (effets Normal et Max)
    val pulseA = if (isFocused && fx >= 1 && style == 0) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            0.55f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "pa"
        )
    } else null
    // reflet qui traverse la tuile (effet Max)
    val shineA = if (isFocused && fx >= 2 && style == 0) {
        rememberInfiniteTransition(label = "shine").animateFloat(
            -0.3f, 1.3f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "sh"
        )
    } else null
    val shape = RoundedCornerShape(16.dp)
    Image(
        bitmap = app.image,
        contentDescription = app.label,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .padding(horizontal = gap)
            .size(w, h)
            .scale(scale)
            .shadow(if (isFocused && fx >= 2) 28.dp else 0.dp, shape, ambientColor = app.accent, spotColor = app.accent)
            .clip(shape)
            .drawWithContent {
                drawContent()
                if (isFocused) {
                    val p = pulseA?.value ?: 1f
                    val col = if (style == 0) Color.White else app.accent
                    drawRoundRect(
                        color = col.copy(alpha = p), cornerRadius = CornerRadius(16.dp.toPx()),
                        style = Stroke(width = 6.dp.toPx())
                    )
                    shineA?.let { sh ->
                        val x = size.width * sh.value
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                start = Offset(x - 60f, 0f), end = Offset(x + 60f, size.height * 0.6f)
                            )
                        )
                    }
                }
            }
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus() }
            .clickable { onClick() }
    )
}
