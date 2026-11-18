package com.exemple.nova

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.tv.TvInputManager
import androidx.core.graphics.drawable.toBitmap
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Un élément de la rangée : application, entrée TV ou bouton Menu
data class Item(
    val label: String,
    val key: String,          // paquet, "tvinput:<id>" ou "menu"
    val icon: Bitmap,
    val accent: Int,
    val kind: Int,            // 0 = appli, 1 = entrée TV, 2 = menu
    val inputId: String? = null
)

// ---------- Cartes 16:9 légères (RGB_565 = moitié moins de mémoire) ----------
object Cards {
    const val W = 256
    const val H = 144

    fun accentOf(bmp: Bitmap): Int {
        val px = Bitmap.createScaledBitmap(bmp, 1, 1, true).getPixel(0, 0)
        val hsv = FloatArray(3)
        Color.colorToHSV(px, hsv)
        if (hsv[1] < 0.15f) return Color.parseColor("#00E5FF")
        hsv[1] = maxOf(hsv[1], 0.75f)
        hsv[2] = 1f
        return Color.HSVToColor(hsv)
    }

    fun fromBanner(d: Drawable): Bitmap {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565)
        val c = Canvas(out)
        c.drawColor(Color.parseColor("#1B1B24"))
        d.setBounds(0, 0, W, H)
        d.draw(c)
        return out
    }

    fun fromIcon(d: Drawable): Bitmap {
        val icon = d.toBitmap(96, 96)
        val hsv = FloatArray(3)
        Color.colorToHSV(accentOf(icon), hsv)
        hsv[1] = minOf(hsv[1], 0.6f)
        hsv[2] = 0.30f
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565)
        val c = Canvas(out)
        c.drawColor(Color.HSVToColor(hsv))
        c.drawBitmap(icon, (W - 96) / 2f, (H - 96) / 2f, null)
        return out
    }

    fun text(title: String, sub: String, c1: Int, c2: Int): Bitmap {
        val out = Bitmap.createBitmap(W, H, Bitmap.Config.RGB_565)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), c1, c2, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null
        p.color = Color.WHITE
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = if (title.length > 8) 30f else 40f
        c.drawText(title, W / 2f, H / 2f + 6f, p)
        p.textSize = 18f
        p.color = Color.parseColor("#9FB4D8")
        c.drawText(sub, W / 2f, H / 2f + 40f, p)
        return out
    }
}

// ---------- Chargement des applis et des entrées (à lancer hors du fil principal) ----------
object Loader {
    fun load(ctx: Context): List<Item> {
        val pm = ctx.packageManager
        val list = ArrayList<Item>()

        list.add(Item("Menu", "menu", Cards.text("MENU", "Réglages rapides", 0xFF2A1F4D.toInt(), 0xFF5B2A86.toInt()), 0xFFB388FF.toInt(), 2))

        try {
            val tm = ctx.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
            for (info in tm.tvInputList) {
                if (info.isPassthroughInput) {
                    val label = (info.loadCustomLabel(ctx) ?: info.loadLabel(ctx)).toString()
                    list.add(
                        Item(
                            label, "tvinput:" + info.id,
                            Cards.text(label, "ENTRÉE TV", 0xFF16213E.toInt(), 0xFF0F3460.toInt()),
                            0xFF4FC3F7.toInt(), 1, info.id
                        )
                    )
                }
            }
        } catch (e: Exception) { }

        val tv = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val normal = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val apps = (tv + normal)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        for (ri in apps) {
            try {
                val banner = ri.activityInfo.loadBanner(pm)
                val bmp = if (banner != null) Cards.fromBanner(banner) else Cards.fromIcon(ri.loadIcon(pm))
                list.add(Item(ri.loadLabel(pm).toString(), ri.activityInfo.packageName, bmp, Cards.accentOf(bmp), 0))
            } catch (e: Exception) { }
        }
        return list
    }
}

// ---------- Réseau ----------
object Net {
    fun text(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "Mozilla/5.0")
        try {
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun weather(lat: Double, lon: Double): String? = try {
        val cur = JSONObject(
            text("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code")
        ).getJSONObject("current")
        val icon = when (cur.getInt("weather_code")) {
            0 -> "☀️"
            1, 2 -> "🌤️"
            3 -> "☁️"
            in 45..48 -> "🌫️"
            in 51..67, in 80..82 -> "🌧️"
            in 95..99 -> "⛈️"
            else -> "🌡️"
        }
        icon + " " + Math.round(cur.getDouble("temperature_2m")) + "°C"
    } catch (e: Exception) {
        null
    }
}

// ---------- Fonds d'écran : 15 images téléchargées une fois, puis lues sur la télé ----------
class Wall(ctx: Context) {
    private val dir = File(ctx.cacheDir, "pack")
    private val queries = listOf(
        "naruto", "one piece", "dragon ball", "demon slayer", "jujutsu kaisen", "bleach",
        "my hero academia", "hunter x hunter", "studio ghibli", "cartoon", "adventure time", "simpsons"
    )

    fun files(): List<File> =
        dir.listFiles { f -> f.name.endsWith(".jpg") }?.sortedBy { it.name } ?: emptyList()

    fun fresh(): Boolean {
        val st = File(dir, "stamp")
        if (!st.exists() || files().size < 8) return false
        val t = st.readText().trim().toLongOrNull() ?: return false
        return System.currentTimeMillis() - t < 24 * 3_600_000L
    }

    fun invalidate() {
        File(dir, "stamp").delete()
    }

    // bloquant : à appeler hors du fil principal
    fun download() {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val paths = ArrayList<String>()
        for (q in queries.shuffled().take(3)) {
            val json = Net.text(
                "https://wallhaven.cc/api/v1/search?q=" + URLEncoder.encode(q, "UTF-8") +
                    "&categories=111&purity=100&sorting=random&atleast=1920x1080&page=1"
            )
            val arr = JSONObject(json).getJSONArray("data")
            val list = ArrayList<String>()
            for (i in 0 until arr.length()) list.add(arr.getJSONObject(i).getString("path"))
            paths.addAll(list.shuffled().take(5))
        }
        var ok = 0
        paths.forEachIndexed { i, p ->
            val f = File(dir, "p%02d.jpg".format(i))
            try {
                val c = URL(p).openConnection() as HttpURLConnection
                c.connectTimeout = 10000
                c.readTimeout = 30000
                c.inputStream.use { input -> f.outputStream().use { out -> input.copyTo(out) } }
                c.disconnect()
                ok++
            } catch (e: Exception) {
                f.delete()
            }
        }
        if (ok > 0) File(dir, "stamp").writeText(System.currentTimeMillis().toString())
    }

    // image réduite à 1280 px de large, en RGB_565 : légère pour la mémoire
    fun decode(f: File, maxW: Int = 1280): Bitmap? {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeFile(f.path, o)
        var s = 1
        while (o.outWidth / (s * 2) >= maxW) s *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = s
        o2.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeFile(f.path, o2)
    }
}
