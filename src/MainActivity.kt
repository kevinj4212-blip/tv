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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlin.math.cos
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
val BG_MODES = listOf("Images", "Vidéo", "Animation", "Mes photos")
val PARTICLE_STYLES = listOf(
    "Étoiles", "Étoiles filantes", "Pluie néon", "Neige", "Bulles", "Lucioles",
    "Matrix", "Aurore boréale", "Grille synthwave", "Vagues néon", "Tunnel d'étoiles", "Pétales"
)
val DIM_LEVELS = listOf(0.08f, 0.25f, 0.45f)
val DIM_NAMES = listOf("Lumineux", "Normal", "Sombre")
val SAVER_BGS = listOf("Mes photos", "Animés", "Animation")

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
val ANIM_COUNT_NAMES = listOf("Moyenne", "Faible", "Forte")
val ANIM_COUNTS = listOf(30, 12, 60)
val ANIM_SPEED_NAMES = listOf("Normale", "Lente", "Rapide")
val ANIM_SPEEDS = listOf(30000, 60000, 15000)
val SHAPES = listOf("Arrondie", "Carrée", "Très arrondie", "Pilule", "Cercle", "Hexagone", "Coins coupés")
val GAP_NAMES = listOf("Normal", "Serré", "Large")
val GAP_DP = listOf(28, 14, 46)
val ZOOM_NAMES = listOf("Normal", "Léger", "Fort")
val ZOOMS = listOf(1.25f, 1.12f, 1.4f)
val BORDER_NAMES = listOf("Blanc", "Couleur de l'appli", "Couleur du thème")
val LAYOUT_NAMES = listOf("Rangée", "Double rangée")
val CLOCK_NAMES = listOf("Moyenne", "Petite", "Grande")
val CLOCK_SIZES = listOf(48, 36, 64)
val QUOTE_DELAYS = listOf(12, 6, 20, 30)

val HexShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.25f, 0f); lineTo(w * 0.75f, 0f); lineTo(w, h * 0.5f)
    lineTo(w * 0.75f, h); lineTo(w * 0.25f, h); lineTo(0f, h * 0.5f); close()
}

fun tileShape(i: Int): Shape = when (i) {
    1 -> RoundedCornerShape(4.dp)
    2 -> RoundedCornerShape(30.dp)
    3 -> RoundedCornerShape(50)
    4 -> CircleShape
    5 -> HexShape
    6 -> CutCornerShape(14.dp)
    else -> RoundedCornerShape(16.dp)
}

fun tileDims(): Pair<Dp, Dp> {
    val ts = TILE_SCALES[AppState.tileIndex]
    return if (AppState.tileShape == 4 || AppState.tileShape == 5) Pair(130.dp * ts, 130.dp * ts)
    else Pair(200.dp * ts, 112.dp * ts)
}

fun cyc(cur: Int, size: Int, d: Int): Int = ((cur + d) % size + size) % size

fun bumpPin(pos: Int, d: Int) {
    val digits = "%04d".format(AppState.pinCode).map { it - '0' }.toMutableList()
    digits[pos] = (digits[pos] + d + 10) % 10
    AppState.pinCode = digits.joinToString("").toInt()
    Remote.clearSessions()
}

data class QuickItem(val icon: String, val label: String, val action: () -> Unit)
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
    var studioOpen by mutableStateOf(false)
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
    var overlayOn by mutableStateOf(false)
    var animCount by mutableStateOf(0)
    var animSpeed by mutableStateOf(0)
    var tileShape by mutableStateOf(0)
    var tileGap by mutableStateOf(0)
    var zoomIndex by mutableStateOf(0)
    var borderMode by mutableStateOf(0)
    var showLabels by mutableStateOf(false)
    var hideRecents by mutableStateOf(false)
    var layoutMode by mutableStateOf(0)
    var hideWeather by mutableStateOf(false)
    var hideWifi by mutableStateOf(false)
    var clockSize by mutableStateOf(0)
    var hideQuotes by mutableStateOf(false)
    var quoteDelay by mutableStateOf(0)
    var pinCode by mutableStateOf(0)
    var pinOff by mutableStateOf(false)

    private fun b(x: Boolean) = if (x) 1 else 0

    private val keys = listOf(
        "theme", "bg", "wall", "pstyle", "dim", "sbg", "fx", "delay", "tile", "focus",
        "hue", "h12", "hidesec", "hidebarca", "hiderates", "mute", "city", "sdelay",
        "overlay", "acount", "aspeed", "shape", "gap", "zoom", "border", "labels", "hiderecents",
        "layout", "hideweather", "hidewifi", "clock", "hidequotes", "qdelay", "pin1", "pinoff"
    )

    fun all(): List<Int> = listOf(
        themeIndex, bgMode, wallIndex, particleStyle, dimIndex, saverBg, fx, delayIndex, tileIndex, focusStyle,
        hue, b(h12), b(hideSec), b(hideBarca), b(hideRates), b(mute), cityIndex, saverDelay,
        b(overlayOn), animCount, animSpeed, tileShape, tileGap, zoomIndex, borderMode, b(showLabels), b(hideRecents),
        layoutMode, b(hideWeather), b(hideWifi), clockSize, b(hideQuotes), quoteDelay, pinCode + 1, b(pinOff)
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
        overlayOn = st.int("overlay") == 1
        animCount = st.int("acount").coerceIn(0, ANIM_COUNTS.lastIndex)
        animSpeed = st.int("aspeed").coerceIn(0, ANIM_SPEEDS.lastIndex)
        tileShape = st.int("shape").coerceIn(0, SHAPES.lastIndex)
        tileGap = st.int("gap").coerceIn(0, GAP_DP.lastIndex)
        zoomIndex = st.int("zoom").coerceIn(0, ZOOMS.lastIndex)
        borderMode = st.int("border").coerceIn(0, BORDER_NAMES.lastIndex)
        showLabels = st.int("labels") == 1
        hideRecents = st.int("hiderecents") == 1
        layoutMode = st.int("layout").coerceIn(0, LAYOUT_NAMES.lastIndex)
        hideWeather = st.int("hideweather") == 1
        hideWifi = st.int("hidewifi") == 1
        clockSize = st.int("clock").coerceIn(0, CLOCK_SIZES.lastIndex)
        hideQuotes = st.int("hidequotes") == 1
        quoteDelay = st.int("qdelay").coerceIn(0, QUOTE_DELAYS.lastIndex)
        val p = st.int("pin1")
        pinCode = if (p == 0) Random.nextInt(10000) else (p - 1).coerceIn(0, 9999)
        pinOff = st.int("pinoff") == 1
    }

    fun reset(st: Store) {
        keys.forEach { st.putInt(it, 0) }
        load(st)
        wallpaperSkip++
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

    private const val LOGIN = """<html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{background:#111;color:#fff;font-family:sans-serif;text-align:center;padding-top:60px}input{width:60%;padding:16px;font-size:28px;text-align:center;letter-spacing:8px;border-radius:12px;border:0}button{display:block;margin:20px auto;padding:16px;width:60%;font-size:22px;border-radius:12px;background:#ff2bd6;color:#fff;border:0}p.e{color:#ff8a80}</style></head><body><h2>Ma TV</h2><p>Entre le code PIN affiché sur la télé</p><form method="POST" action="/login"><input name="pin" type="password" inputmode="numeric" maxlength="8" autofocus><button>Entrer</button></form>%MSG%</body></html>"""

    private val sessions = java.util.Collections.synchronizedSet(HashSet<String>())
    @Volatile private var fails = 0
    @Volatile private var lockUntil = 0L

    fun clearSessions() { sessions.clear() }

    private fun loginPage(msg: String): String =
        LOGIN.replace("%MSG%", if (msg.isEmpty()) "" else "<p class=\"e\">$msg</p>")

    private fun send(out: java.io.OutputStream, status: String, body: String, extra: String = "") {
        val b = body.toByteArray(Charsets.UTF_8)
        out.write(
            ("HTTP/1.1 $status\r\nContent-Type: text/html; charset=utf-8\r\n" +
                "Content-Length: ${b.size}\r\nConnection: close\r\n$extra\r\n").toByteArray()
        )
        out.write(b)
        out.flush()
    }

    private fun handle(c: Socket) {
        try {
            c.use {
                val input = BufferedInputStream(it.getInputStream())
                val line = readLine(input) ?: return
                var len = 0L
                var cookie = ""
                while (true) {
                    val h = readLine(input) ?: break
                    if (h.isEmpty()) break
                    if (h.startsWith("Content-Length:", ignoreCase = true)) {
                        len = h.substringAfter(":").trim().toLongOrNull() ?: 0L
                    }
                    if (h.startsWith("Cookie:", ignoreCase = true)) cookie = h.substringAfter(":")
                }
                val method = line.substringBefore(" ")
                val path = line.split(" ").getOrNull(1) ?: "/"
                val cmd = path.substringBefore("?")
                val arg = path.substringAfter("t=", "")
                val out = it.getOutputStream()
                val token = cookie.split(";").map { x -> x.trim() }.firstOrNull { x -> x.startsWith("s=") }?.substring(2) ?: ""
                val authed = AppState.pinOff || (token.isNotEmpty() && sessions.contains(token))

                // ----- connexion par code PIN -----
                if (method == "POST" && cmd == "/login") {
                    val buf = ByteArray(minOf(len, 256L).toInt())
                    var got = 0
                    while (got < buf.size) {
                        val n = input.read(buf, got, buf.size - got)
                        if (n < 0) break
                        got += n
                    }
                    val form = String(buf, 0, got, Charsets.UTF_8)
                    val pin = URLDecoder.decode(form.substringAfter("pin=", "").substringBefore("&"), "UTF-8")
                    val now = System.currentTimeMillis()
                    if (now < lockUntil) {
                        send(out, "429 Too Many Requests", loginPage("Trop d'essais. Réessaie dans une minute."))
                        return
                    }
                    if (pin == "%04d".format(AppState.pinCode)) {
                        fails = 0
                        val t = java.util.UUID.randomUUID().toString().replace("-", "")
                        if (sessions.size > 20) sessions.clear()
                        sessions.add(t)
                        out.write(
                            ("HTTP/1.1 303 See Other\r\nLocation: /\r\n" +
                                "Set-Cookie: s=$t; Path=/; HttpOnly; SameSite=Strict; Max-Age=2592000\r\n" +
                                "Content-Length: 0\r\nConnection: close\r\n\r\n").toByteArray()
                        )
                        out.flush()
                    } else {
                        fails++
                        if (fails >= 5) { lockUntil = now + 60_000L; fails = 0 }
                        send(out, "401 Unauthorized", loginPage("Code incorrect."))
                    }
                    return
                }

                // ----- tout le reste demande le PIN -----
                if (!authed) {
                    send(out, if (cmd == "/upload") "401 Unauthorized" else "200 OK", loginPage(""))
                    return
                }

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
                    out.flush()
                } else {
                    send(out, "200 OK", PAGE)
                }
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
    val (tw, th) = tileDims()
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
        if (AppState.bgMode == 2 || AppState.overlayOn) Particles(accent, theme.second)

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
            val gapDp = GAP_DP[AppState.tileGap].dp
            val tileItem: @Composable (AppItem, Int) -> Unit = { app, index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AppTile(
                        app, if (index == 0) firstTile else null,
                        onFocus = { focusedApp = app; if (soundOn && !AppState.mute) sounds.move() },
                        onClick = { launch(app) }
                    )
                    if (AppState.showLabels) {
                        Text(
                            app.label, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                            modifier = Modifier.width(tw).padding(top = 6.dp)
                        )
                    }
                }
            }
            if (AppState.layoutMode == 0) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 80.dp, vertical = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(gapDp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item { NeonButton("⚡ Menu", accent, Modifier.size(120.dp * ts, th)) { AppState.menuOpen = true } }
                    itemsIndexed(apps, key = { _, a -> a.pkg }) { index, app -> tileItem(app, index) }
                }
            } else {
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(2),
                    modifier = Modifier.height(th * 2 + gapDp / 2 + 60.dp + (if (AppState.showLabels) 52.dp else 0.dp)),
                    contentPadding = PaddingValues(horizontal = 80.dp, vertical = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(gapDp),
                    verticalArrangement = Arrangement.spacedBy(gapDp / 2)
                ) {
                    item { NeonButton("⚡ Menu", accent, Modifier.size(120.dp * ts, th)) { AppState.menuOpen = true } }
                    gridItemsIndexed(apps, key = { _, a -> a.pkg }) { index, app -> tileItem(app, index) }
                }
            }
            if (AppState.layoutMode == 0 && !AppState.hideRecents && recentApps.isNotEmpty()) {
                Text("Récents", color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp,
                    modifier = Modifier.padding(start = 80.dp, bottom = 6.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 80.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(recentApps, key = { _, a -> "r" + a.pkg }) { _, app ->
                        AppTile(
                            app, null, tw * 0.7f, th * 0.7f,
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

    // ----- Menu rapide (grille de cartes) -----
    if (AppState.menuOpen) {
        Dialog(onDismissRequest = { AppState.menuOpen = false }) {
            Panel("⚡ Menu rapide", accent, width = 540.dp) { first ->
                val items = listOf(
                    QuickItem("🔉", "Volume −") {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    },
                    QuickItem("🔊", "Volume +") {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    },
                    QuickItem("🔇", "Muet") {
                        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                    },
                    QuickItem("✨", "Studio") { AppState.menuOpen = false; AppState.studioOpen = true },
                    QuickItem("📷", "Photos et citations") { AppState.menuOpen = false; sub = "photos" },
                    QuickItem("📶", "Wi-Fi") { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    QuickItem("⚙", "Paramètres de la télé") { ctx.startActivity(Intent(Settings.ACTION_SETTINGS)) },
                    QuickItem("🌙", "Écran de veille") { AppState.menuOpen = false; AppState.saver = true },
                    QuickItem("👁", "Réafficher les applis") { hidden = emptyList(); store.save("hidden", hidden) },
                )
                items.chunked(3).forEachIndexed { r, rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEachIndexed { c, q ->
                            MenuCard(q.icon, q.label, accent, Modifier.weight(1f), if (r == 0 && c == 0) first else null) { q.action() }
                        }
                    }
                }
            }
        }
    }

    // ----- Studio de personnalisation -----
    if (AppState.studioOpen) {
        Dialog(
            onDismissRequest = { AppState.studioOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Studio(
                accent, theme,
                onClose = { AppState.studioOpen = false },
                onPhotos = { AppState.studioOpen = false; sub = "photos" },
                onUnhide = { hidden = emptyList(); store.save("hidden", hidden) },
                onReset = { AppState.reset(store) }
            )
        }
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
                Text(
                    if (AppState.pinOff) "Page sans code PIN" else "Code PIN : " + "%04d".format(AppState.pinCode),
                    color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold
                )
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
                color = Color.White, fontSize = CLOCK_SIZES[AppState.clockSize].sp, fontWeight = FontWeight.Bold,
                shadow = glow(accent, 28f)
            )
        )
        Text(
            SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(now).replaceFirstChar { it.uppercase() },
            color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        val net = if (AppState.hideWifi) network.substringBefore(" :") else network
        Text(if (AppState.hideWeather) net else "$weather   •   $net", color = Color.White, fontSize = 20.sp)
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
    LaunchedEffect(Unit) { while (true) { delay(QUOTE_DELAYS[AppState.quoteDelay] * 1000L); qi++ } }
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
            if (!AppState.hideQuotes) Crossfade(targetState = quote, animationSpec = tween(if (LITE_MODE) 400 else 1200), label = "quote") { q ->
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
fun Particles(color: Color, second: Color = color, style: Int = AppState.particleStyle) {
    val dur = ANIM_SPEEDS[AppState.animSpeed]
    val base = ANIM_COUNTS[AppState.animCount]
    val count = if (LITE_MODE) minOf(base, 24) else base
    val t by rememberInfiniteTransition(label = "p").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(dur, easing = LinearEasing)), label = "t"
    )
    val seeds = remember(count) {
        List(count) {
            floatArrayOf(Random.nextFloat(), Random.nextFloat(), Random.nextInt(1, 4).toFloat(), 1f + Random.nextFloat() * 3f)
        }
    }
    Canvas(Modifier.fillMaxSize()) { drawAnim(style, t, color, second, seeds) }
}

fun DrawScope.drawAnim(style: Int, t: Float, c: Color, c2: Color, seeds: List<FloatArray>) {
    val w = size.width
    val h = size.height
    val tau = 6.2832f
    when (style) {
        1 -> { // étoiles filantes
            seeds.forEachIndexed { idx, s ->
                if (idx % 3 == 0) {
                    val p = (t * s[2] * 3f + s[1]) % 1f
                    if (p < 0.3f) {
                        val q = p / 0.3f
                        val hx = (s[0] * 0.7f + q * 0.35f) * w
                        val hy = (s[1] * 0.4f + q * 0.4f) * h
                        val tl = 140f * (0.6f + s[3] / 4f)
                        val fade = sin(q * 3.1416f)
                        drawLine(c.copy(alpha = 0.8f * fade), Offset(hx - tl * 0.85f, hy - tl * 0.5f), Offset(hx, hy), 3f)
                        drawCircle(Color.White.copy(alpha = fade), 4f, Offset(hx, hy))
                    }
                } else {
                    val tw = 0.3f + 0.5f * (0.5f + 0.5f * sin((t * 3f * s[2] + s[0]) * tau))
                    drawCircle(Color.White.copy(alpha = tw * 0.6f), s[3] * 0.9f, Offset(s[0] * w, s[1] * h))
                }
            }
        }
        2 -> seeds.forEach { s -> // pluie néon
            val y = (s[1] + t * s[2]) % 1f
            drawLine(c.copy(alpha = 0.55f), Offset(s[0] * w, y * h), Offset(s[0] * w, y * h + 14f * s[3]), 2.5f)
        }
        3 -> seeds.forEach { s -> // neige
            val y = (s[1] + t) % 1f
            val x = s[0] + 0.03f * sin((t * 2f * s[2] + s[0]) * tau)
            drawCircle(Color.White.copy(alpha = 0.7f), s[3] * 2.2f, Offset(x * w, y * h))
        }
        4 -> seeds.forEach { s -> // bulles
            val y = ((s[1] - t) % 1f + 1f) % 1f
            val x = s[0] + 0.02f * sin((t * 2f * s[2] + s[0]) * tau)
            drawCircle(c.copy(alpha = 0.6f), s[3] * 7f, Offset(x * w, y * h), style = Stroke(width = 2.5f))
        }
        5 -> seeds.forEach { s -> // lucioles
            val x = 0.5f + 0.45f * sin((t * s[2] + s[0]) * tau)
            val y = 0.5f + 0.4f * sin((t * (s[2] + 1f) + s[1]) * tau)
            val a = 0.25f + 0.5f * (0.5f + 0.5f * sin((t * 3f * s[2] + s[0]) * tau))
            drawCircle(c.copy(alpha = a), s[3] * 3.5f, Offset(x * w, y * h))
        }
        6 -> seeds.forEach { s -> // matrix
            val col = (s[0] * 40f).toInt() / 40f
            val y = (s[1] + t * s[2]) % 1f
            for (j in 0..3) {
                drawCircle(c.copy(alpha = 0.7f - j * 0.17f), 3f, Offset(col * w, ((y - j * 0.025f + 1f) % 1f) * h))
            }
        }
        7 -> { // aurore boréale
            val cols = listOf(c, c2, c.copy(alpha = 0.8f))
            for (b in 0..2) {
                val path = Path()
                val baseY = h * (0.22f + 0.13f * b)
                val steps = 24
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = baseY + h * 0.07f * sin(i / steps.toFloat() * (2f + b) * tau + t * tau * (b + 1))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, cols[b].copy(alpha = 0.22f), style = Stroke(width = h * 0.14f))
            }
        }
        8 -> { // grille synthwave
            val horizon = h * 0.6f
            drawCircle(c2.copy(alpha = 0.25f), h * 0.16f, Offset(w / 2f, horizon - h * 0.04f))
            drawLine(c2.copy(alpha = 0.8f), Offset(0f, horizon), Offset(w, horizon), 3f)
            for (i in 0 until 10) {
                val z = ((i / 10f) + t * 5f) % 1f
                val y = horizon + (h - horizon) * z * z
                drawLine(c.copy(alpha = 0.15f + 0.6f * z), Offset(0f, y), Offset(w, y), 1.5f + 2f * z)
            }
            for (j in -8..8) {
                drawLine(c.copy(alpha = 0.35f), Offset(w / 2f + j * w * 0.012f, horizon), Offset(w / 2f + j * w * 0.14f, h), 1.5f)
            }
        }
        9 -> { // vagues néon
            for (b in 0..2) {
                val path = Path()
                val steps = 40
                val baseY = h * (0.55f + 0.12f * b)
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = baseY + h * 0.05f * sin(i / steps.toFloat() * (3f + b) * tau + t * tau * (b + 1))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, (if (b == 1) c2 else c).copy(alpha = 0.7f), style = Stroke(width = 4f))
            }
        }
        10 -> { // tunnel d'étoiles
            val cx = w / 2f
            val cy = h / 2f
            val maxR = maxOf(w, h) * 0.6f
            seeds.forEach { s ->
                val ang = s[0] * tau
                val p = (s[1] + t * s[2] * 2f) % 1f
                val r = p * p * maxR
                val r0 = r * 0.82f
                val dx = cos(ang)
                val dy = sin(ang)
                drawLine(c.copy(alpha = 0.15f + 0.7f * p), Offset(cx + dx * r0, cy + dy * r0), Offset(cx + dx * r, cy + dy * r), 1f + 3f * p)
            }
        }
        11 -> seeds.forEach { s -> // pétales
            val y = (s[1] + t) % 1f
            val x = s[0] + 0.05f * sin((t * 2f * s[2] + s[0]) * tau)
            val deg = t * 360f * s[2] + s[0] * 360f
            rotate(deg, Offset(x * w, y * h)) {
                drawOval(Color(0xFFFF9EC4).copy(alpha = 0.8f), Offset(x * w - s[3] * 3f, y * h - s[3] * 1.8f), Size(s[3] * 6f, s[3] * 3.6f))
            }
        }
        else -> seeds.forEach { s -> // étoiles
            val y = ((s[1] - t * s[2]) % 1f + 1f) % 1f
            val x = s[0] + 0.02f * sin((t * 3f * s[2] + s[0]) * tau)
            drawCircle(c.copy(alpha = 0.25f + 0.05f * s[3]), s[3] * 2.5f, Offset(x * w, y * h))
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
            .background(if (f) accent.copy(alpha = 0.4f) else if (selected) accent.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.5f))
            .border(if (f) 2.dp else 1.dp, if (f) accent else accent.copy(alpha = 0.35f), shape)
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
fun Panel(title: String, accent: Color, width: Dp = 400.dp, content: @Composable ColumnScope.(FocusRequester) -> Unit) {
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
            .width(width)
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
    w: Dp = tileDims().first, h: Dp = tileDims().second,
    onFocus: () -> Unit, onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val style = AppState.focusStyle
    val fx = AppState.fx
    val zoom = ZOOMS[AppState.zoomIndex]
    val target = when (style) { 0 -> zoom; 1 -> 1f + (zoom - 1f) * 0.6f; else -> 1f }
    val scale by animateFloatAsState(
        if (isFocused) target else 1f,
        if (fx == 0) tween(120) else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    val gap by animateDpAsState(if (isFocused && style == 0) 22.dp else 0.dp, tween(if (fx == 0) 100 else 220), label = "gap")
    val pulseA = if (isFocused && fx >= 1 && style == 0) {
        rememberInfiniteTransition(label = "pulse").animateFloat(
            0.55f, 1f, infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse), label = "pa"
        )
    } else null
    val shineA = if (isFocused && fx >= 2 && style == 0) {
        rememberInfiniteTransition(label = "shine").animateFloat(
            -0.3f, 1.3f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "sh"
        )
    } else null
    val shape = tileShape(AppState.tileShape)
    val themeCol = resolveTheme(AppState.themeIndex).color ?: app.accent
    val borderCol = when (AppState.borderMode) { 1 -> app.accent; 2 -> themeCol; else -> Color.White }
    val pulse = pulseA?.value ?: 1f
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
            .then(
                if (isFocused) Modifier.border(BorderStroke(if (style == 2) 3.dp else 5.dp, borderCol.copy(alpha = pulse)), shape)
                else Modifier
            )
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { isFocused = it.isFocused; if (it.isFocused) onFocus() }
            .clickable { onClick() }
    )
}

// =====================================================================
//  STUDIO DE PERSONNALISATION
// =====================================================================
@Composable
fun TabButton(text: String, selected: Boolean, accent: Color, requester: FocusRequester?, onFocus: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { f = it.isFocused; if (it.isFocused) onFocus() }
            .clip(shape)
            .background(if (f) accent.copy(alpha = 0.4f) else if (selected) accent.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.4f))
            .border(if (f) 2.dp else 1.dp, if (f || selected) accent else Color.White.copy(alpha = 0.15f), shape)
            .clickable { onFocus() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun OptionRow(
    label: String, value: String, accent: Color, dot: Color? = null,
    requester: FocusRequester? = null, onChange: (Int) -> Unit
) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { f = it.isFocused }
            .onKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) {
                    when (e.key) {
                        Key.DirectionLeft -> { onChange(-1); true }
                        Key.DirectionRight -> { onChange(1); true }
                        else -> false
                    }
                } else false
            }
            .clip(shape)
            .background(if (f) accent.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.4f))
            .border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(alpha = 0.12f), shape)
            .clickable { onChange(1) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), color = Color.White, fontSize = 18.sp)
        Text("◀", color = if (f) accent else Color.White.copy(alpha = 0.25f), fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        if (dot != null) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
        }
        Text(value, modifier = Modifier.widthIn(min = 150.dp), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.width(10.dp))
        Text("▶", color = if (f) accent else Color.White.copy(alpha = 0.25f), fontSize = 18.sp)
    }
}

@Composable
fun PreviewPanel(accent: Color, theme: Theme, modifier: Modifier) {
    val shape = tileShape(AppState.tileShape)
    val sq = AppState.tileShape == 4 || AppState.tileShape == 5
    val w = if (sq) 64.dp else 96.dp
    val h = if (sq) 64.dp else 54.dp
    val zoom = ZOOMS[AppState.zoomIndex]
    val gap = (GAP_DP[AppState.tileGap] / 2.5f).dp
    val borderCol = when (AppState.borderMode) { 1 -> theme.second; 2 -> theme.color ?: accent; else -> Color.White }
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(theme.base, accent.copy(alpha = 0.35f), theme.second.copy(alpha = 0.3f))))
            .border(2.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Text("Aperçu en direct", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.5f))) {
            Particles(accent, theme.second)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("▶", "🎮", "🎵").forEachIndexed { i, e ->
                val sel = i == 1
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(w, h)
                            .scale(if (sel) zoom else 1f)
                            .clip(shape)
                            .background(accent.copy(alpha = if (sel) 0.85f else 0.45f))
                            .then(if (sel) Modifier.border(4.dp, borderCol, shape) else Modifier),
                        contentAlignment = Alignment.Center
                    ) { Text(e, fontSize = 26.sp) }
                    if (AppState.showLabels) Text("Appli", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Thème : ${theme.name}", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
        Text("Tuiles : ${SHAPES[AppState.tileShape]} • ${LAYOUT_NAMES[AppState.layoutMode]}", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
        Text("Animation : ${PARTICLE_STYLES[AppState.particleStyle]}", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
    }
}

@Composable
fun Studio(
    accent: Color, theme: Theme,
    onClose: () -> Unit, onPhotos: () -> Unit, onUnhide: () -> Unit, onReset: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("🎨 Couleurs", "🖼 Fond", "✨ Animations", "📱 Applis", "🕒 Infos", "🌙 Veille", "⚡ Système")
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(200); try { first.requestFocus() } catch (ignored: Exception) {} }
    val view = LocalView.current
    SideEffect { (view.parent as? DialogWindowProvider)?.window?.setDimAmount(0f) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xF2070510), Color(0xEB0B0820))))) {
        Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "✦ Studio de personnalisation", Modifier.weight(1f),
                    style = TextStyle(color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, shadow = glow(accent, 30f))
                )
                NeonButton("✕ Fermer", accent) { onClose() }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { i, t -> TabButton(t, tab == i, accent, if (i == 0) first else null) { tab = i } }
            }
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    Modifier.weight(1.5f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (tab) {
                        0 -> {
                            OptionRow("Thème de couleurs", theme.name, accent, dot = accent) { d ->
                                AppState.themeIndex = cyc(AppState.themeIndex, THEMES.size, d)
                            }
                            if (theme.name == CUSTOM_NAME) {
                                OptionRow("Teinte (couleur au choix)", "${AppState.hue}°", accent, dot = accent) { d ->
                                    AppState.hue = (AppState.hue + d * 15 + 360) % 360
                                }
                            }
                            OptionRow("Luminosité du fond", DIM_NAMES[AppState.dimIndex], accent) { d ->
                                AppState.dimIndex = cyc(AppState.dimIndex, DIM_LEVELS.size, d)
                            }
                            OptionRow("Couleur du contour des tuiles", BORDER_NAMES[AppState.borderMode], accent) { d ->
                                AppState.borderMode = cyc(AppState.borderMode, BORDER_NAMES.size, d)
                            }
                        }
                        1 -> {
                            OptionRow("Type de fond", BG_MODES[AppState.bgMode], accent) { d ->
                                AppState.bgMode = cyc(AppState.bgMode, BG_MODES.size, d)
                            }
                            OptionRow("Univers d'images", WALL_THEMES[AppState.wallIndex].name, accent) { d ->
                                AppState.wallIndex = cyc(AppState.wallIndex, WALL_THEMES.size, d)
                            }
                            OptionRow("Changement d'image", "${WALL_DELAYS[AppState.delayIndex]} s", accent) { d ->
                                AppState.delayIndex = cyc(AppState.delayIndex, WALL_DELAYS.size, d)
                            }
                            NeonButton("⏭ Image suivante", accent, Modifier.fillMaxWidth()) { AppState.wallpaperSkip++ }
                        }
                        2 -> {
                            OptionRow("Style d'animation", PARTICLE_STYLES[AppState.particleStyle], accent) { d ->
                                AppState.particleStyle = cyc(AppState.particleStyle, PARTICLE_STYLES.size, d)
                            }
                            OptionRow("Animation par-dessus le fond", onOff(AppState.overlayOn), accent) { AppState.overlayOn = !AppState.overlayOn }
                            OptionRow("Quantité", ANIM_COUNT_NAMES[AppState.animCount], accent) { d ->
                                AppState.animCount = cyc(AppState.animCount, ANIM_COUNTS.size, d)
                            }
                            OptionRow("Vitesse", ANIM_SPEED_NAMES[AppState.animSpeed], accent) { d ->
                                AppState.animSpeed = cyc(AppState.animSpeed, ANIM_SPEEDS.size, d)
                            }
                        }
                        3 -> {
                            OptionRow("Disposition", LAYOUT_NAMES[AppState.layoutMode], accent) { d ->
                                AppState.layoutMode = cyc(AppState.layoutMode, LAYOUT_NAMES.size, d)
                            }
                            OptionRow("Forme des tuiles", SHAPES[AppState.tileShape], accent) { d ->
                                AppState.tileShape = cyc(AppState.tileShape, SHAPES.size, d)
                            }
                            OptionRow("Taille des tuiles", TILE_NAMES[AppState.tileIndex], accent) { d ->
                                AppState.tileIndex = cyc(AppState.tileIndex, TILE_SCALES.size, d)
                            }
                            OptionRow("Espacement", GAP_NAMES[AppState.tileGap], accent) { d ->
                                AppState.tileGap = cyc(AppState.tileGap, GAP_DP.size, d)
                            }
                            OptionRow("Animation de sélection", FOCUS_STYLES[AppState.focusStyle], accent) { d ->
                                AppState.focusStyle = cyc(AppState.focusStyle, FOCUS_STYLES.size, d)
                            }
                            OptionRow("Zoom de sélection", ZOOM_NAMES[AppState.zoomIndex], accent) { d ->
                                AppState.zoomIndex = cyc(AppState.zoomIndex, ZOOMS.size, d)
                            }
                            OptionRow("Noms sous les tuiles", onOff(AppState.showLabels), accent) { AppState.showLabels = !AppState.showLabels }
                            OptionRow("Afficher les récents", onOff(!AppState.hideRecents), accent) { AppState.hideRecents = !AppState.hideRecents }
                        }
                        4 -> {
                            OptionRow("Format de l'heure", if (AppState.h12) "12 h" else "24 h", accent) { AppState.h12 = !AppState.h12 }
                            OptionRow("Afficher les secondes", onOff(!AppState.hideSec), accent) { AppState.hideSec = !AppState.hideSec }
                            OptionRow("Taille de l'horloge", CLOCK_NAMES[AppState.clockSize], accent) { d ->
                                AppState.clockSize = cyc(AppState.clockSize, CLOCK_SIZES.size, d)
                            }
                            OptionRow("Météo", onOff(!AppState.hideWeather), accent) { AppState.hideWeather = !AppState.hideWeather }
                            OptionRow("Ville de la météo", CITIES[AppState.cityIndex].name, accent) { d ->
                                AppState.cityIndex = cyc(AppState.cityIndex, CITIES.size, d)
                            }
                            OptionRow("Nom du Wi-Fi", onOff(!AppState.hideWifi), accent) { AppState.hideWifi = !AppState.hideWifi }
                            OptionRow("Carte du Barça", onOff(!AppState.hideBarca), accent) { AppState.hideBarca = !AppState.hideBarca }
                            OptionRow("Taux de change", onOff(!AppState.hideRates), accent) { AppState.hideRates = !AppState.hideRates }
                        }
                        5 -> {
                            OptionRow("Veille après", saverLabel(AppState.saverDelay), accent) { d ->
                                AppState.saverDelay = cyc(AppState.saverDelay, SAVER_MINUTES.size, d)
                            }
                            OptionRow("Fond de la veille", SAVER_BGS[AppState.saverBg], accent) { d ->
                                AppState.saverBg = cyc(AppState.saverBg, SAVER_BGS.size, d)
                            }
                            OptionRow("Citations", onOff(!AppState.hideQuotes), accent) { AppState.hideQuotes = !AppState.hideQuotes }
                            OptionRow("Changement de citation", "${QUOTE_DELAYS[AppState.quoteDelay]} s", accent) { d ->
                                AppState.quoteDelay = cyc(AppState.quoteDelay, QUOTE_DELAYS.size, d)
                            }
                            NeonButton("🌙 Voir l'écran de veille maintenant", accent, Modifier.fillMaxWidth()) {
                                onClose(); AppState.saver = true
                            }
                            NeonButton("📷 Mes photos et mes citations", accent, Modifier.fillMaxWidth()) { onPhotos() }
                        }
                        else -> {
                            OptionRow("Effets (performance)", FX_NAMES[AppState.fx], accent) { d ->
                                AppState.fx = cyc(AppState.fx, FX_NAMES.size, d)
                            }
                            OptionRow("Sons", onOff(!AppState.mute), accent) { AppState.mute = !AppState.mute }
                            OptionRow("Page web protégée par PIN", onOff(!AppState.pinOff), accent) { AppState.pinOff = !AppState.pinOff }
                            OptionRow("PIN : chiffre 1", "%04d".format(AppState.pinCode).substring(0, 1), accent) { d -> bumpPin(0, d) }
                            OptionRow("PIN : chiffre 2", "%04d".format(AppState.pinCode).substring(1, 2), accent) { d -> bumpPin(1, d) }
                            OptionRow("PIN : chiffre 3", "%04d".format(AppState.pinCode).substring(2, 3), accent) { d -> bumpPin(2, d) }
                            OptionRow("PIN : chiffre 4", "%04d".format(AppState.pinCode).substring(3, 4), accent) { d -> bumpPin(3, d) }
                            NeonButton("🔑 PIN au hasard (actuel : ${"%04d".format(AppState.pinCode)})", accent, Modifier.fillMaxWidth()) {
                                AppState.pinCode = Random.nextInt(10000); Remote.clearSessions()
                            }
                            NeonButton("👁 Réafficher les applis masquées", accent, Modifier.fillMaxWidth()) { onUnhide() }
                            NeonButton("♻ Réinitialiser tous les réglages", accent, Modifier.fillMaxWidth()) { onReset() }
                        }
                    }
                }
                PreviewPanel(accent, theme, Modifier.weight(1f).fillMaxHeight())
            }
            Text(
                "◀ ▶ changer la valeur   •   OK : suivant   •   Retour : fermer",
                color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun MenuCard(icon: String, label: String, accent: Color, modifier: Modifier, requester: FocusRequester?, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (f) 1.06f else 1f, tween(100), label = "mc")
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .height(104.dp)
            .scale(sc)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { f = it.isFocused }
            .clip(shape)
            .background(if (f) accent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.07f))
            .border(if (f) 2.dp else 1.dp, if (f) accent else Color.White.copy(alpha = 0.18f), shape)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
    }
}
