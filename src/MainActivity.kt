package com.exemple.nova

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.tv.TvContract
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private var unit = 1f
    private val io = Executors.newFixedThreadPool(2)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var bgA: ImageView
    private lateinit var bgB: ImageView
    private var frontIsA = true
    private lateinit var rv: RecyclerView
    private lateinit var adapter: TileAdapter
    private lateinit var titleTv: TextView
    private lateinit var subTv: TextView
    private lateinit var weatherTv: TextView
    private lateinit var wifiTv: TextView
    private lateinit var wall: Wall
    private var wallIdx = -1
    private var weatherText = ""

    private fun u(v: Float): Int = (v * unit).toInt()

    private fun label(text: String, sizeU: Float, color: Int, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = text
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeU * unit)
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        t.setShadowLayer(8f, 0f, 2f, Color.BLACK)
        return t
    }

    private fun clock(format: String, sizeU: Float, bold: Boolean): TextClock {
        val c = TextClock(this)
        c.format24Hour = format
        c.format12Hour = format
        c.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizeU * unit)
        c.setTextColor(Color.WHITE)
        if (bold) c.typeface = Typeface.DEFAULT_BOLD
        c.setShadowLayer(8f, 0f, 2f, Color.BLACK)
        return c
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dm = resources.displayMetrics
        unit = minOf(dm.widthPixels, dm.heightPixels) / 720f
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        wall = Wall(this)

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.BLACK)
        root.clipChildren = false

        bgA = ImageView(this)
        bgA.scaleType = ImageView.ScaleType.CENTER_CROP
        bgB = ImageView(this)
        bgB.scaleType = ImageView.ScaleType.CENTER_CROP
        bgB.alpha = 0f
        root.addView(bgA, FrameLayout.LayoutParams(-1, -1))
        root.addView(bgB, FrameLayout.LayoutParams(-1, -1))

        val scrim = View(this)
        scrim.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x99000000.toInt(), 0x22000000, 0x33000000, 0xE6000000.toInt())
        )
        root.addView(scrim, FrameLayout.LayoutParams(-1, -1))

        // heure et date en haut à gauche
        val left = LinearLayout(this)
        left.orientation = LinearLayout.VERTICAL
        left.addView(clock("HH:mm:ss", 54f, true))
        left.addView(clock("EEEE d MMMM", 20f, false))
        val lpLeft = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START)
        lpLeft.leftMargin = u(60f)
        lpLeft.topMargin = u(36f)
        root.addView(left, lpLeft)

        // météo et Wi-Fi en haut à droite
        val right = LinearLayout(this)
        right.orientation = LinearLayout.VERTICAL
        right.gravity = Gravity.END
        weatherTv = label("", 24f, Color.WHITE)
        wifiTv = label("", 16f, 0xCCFFFFFF.toInt())
        right.addView(weatherTv)
        right.addView(wifiTv)
        val lpRight = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END)
        lpRight.rightMargin = u(60f)
        lpRight.topMargin = u(40f)
        root.addView(right, lpRight)

        // nom de l'appli sélectionnée
        val mid = LinearLayout(this)
        mid.orientation = LinearLayout.VERTICAL
        titleTv = label("", 44f, Color.WHITE, true)
        subTv = label("", 18f, 0xFFB0BEC5.toInt())
        mid.addView(titleTv)
        mid.addView(subTv)
        val lpMid = FrameLayout.LayoutParams(-2, -2, Gravity.START or Gravity.CENTER_VERTICAL)
        lpMid.leftMargin = u(60f)
        lpMid.topMargin = u(40f)
        root.addView(mid, lpMid)

        // rangée d'applis
        adapter = TileAdapter(unit, { open(it) }, { item, _ ->
            titleTv.text = item.label
            subTv.text = when (item.kind) {
                0 -> "Application"
                1 -> "Entrée TV"
                else -> "Réglages rapides"
            }
        })
        rv = RecyclerView(this)
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.clipChildren = false
        rv.clipToPadding = false
        rv.setPadding(u(60f), 0, u(60f), 0)
        rv.itemAnimator = null
        rv.isFocusable = false
        rv.adapter = adapter
        val lpRv = FrameLayout.LayoutParams(-1, u(180f), Gravity.BOTTOM)
        lpRv.bottomMargin = u(30f)
        root.addView(rv, lpRv)

        setContentView(root)

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        io.execute {
            val list = Loader.load(this)
            runOnUiThread {
                adapter.items = list
                adapter.notifyDataSetChanged()
                val pos = if (list.size > 1) 1 else 0
                rv.scrollToPosition(pos)
                rv.post { rv.findViewHolderForAdapterPosition(pos)?.itemView?.requestFocus() }
            }
        }
    }

    // ---------- fonds d'écran ----------
    private val rotate = object : Runnable {
        override fun run() {
            nextWall()
            handler.postDelayed(this, 30_000L)
        }
    }

    private fun nextWall() {
        io.execute {
            val fs = wall.files()
            if (fs.isEmpty()) return@execute
            wallIdx = (wallIdx + 1) % fs.size
            val bmp = wall.decode(fs[wallIdx]) ?: return@execute
            runOnUiThread { crossfade(bmp) }
        }
    }

    private fun crossfade(bmp: Bitmap) {
        val front = if (frontIsA) bgA else bgB
        val back = if (frontIsA) bgB else bgA
        back.setImageBitmap(bmp)
        back.alpha = 0f
        back.animate().alpha(1f).setDuration(900).start()
        front.animate().alpha(0f).setDuration(900).start()
        frontIsA = !frontIsA
    }

    private fun ensurePack() {
        io.execute {
            if (!wall.fresh()) {
                try { wall.download() } catch (e: Exception) { }
            }
            runOnUiThread {
                if (wallIdx < 0) nextWall()
            }
        }
    }

    // ---------- infos : météo et Wi-Fi ----------
    private val infoTick = object : Runnable {
        override fun run() {
            io.execute {
                val w = Net.weather(5.30, -3.95)
                if (w != null) weatherText = w
                runOnUiThread { weatherTv.text = weatherText }
            }
            wifiTv.text = wifiName()
            handler.postDelayed(this, 10 * 60_000L)
        }
    }

    @Suppress("DEPRECATION")
    private fun wifiName(): String = try {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wm.connectionInfo?.ssid?.trim('"') ?: ""
        if (ssid.isNotBlank() && ssid != "<unknown ssid>") "Wi-Fi : $ssid" else "Wi-Fi"
    } catch (e: Exception) {
        "Wi-Fi"
    }

    // ---------- lancement ----------
    private fun open(e: Item) {
        when (e.kind) {
            2 -> showMenu()
            1 -> try {
                startActivity(Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(e.inputId!!)))
            } catch (ex: Exception) {
                Toast.makeText(this, "Entrée indisponible : utilise la touche Source", Toast.LENGTH_LONG).show()
            }
            else -> {
                val i = packageManager.getLeanbackLaunchIntentForPackage(e.key)
                    ?: packageManager.getLaunchIntentForPackage(e.key)
                if (i != null) startActivity(i)
            }
        }
    }

    // ---------- menu rapide ----------
    private fun showMenu() {
        val d = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)
        val panel = LinearLayout(this)
        panel.orientation = LinearLayout.VERTICAL
        panel.setPadding(u(28f), u(24f), u(28f), u(24f))
        val bgPanel = GradientDrawable()
        bgPanel.setColor(0xF0101018.toInt())
        bgPanel.cornerRadius = 24f * unit
        bgPanel.setStroke(u(2f), 0xFF7C4DFF.toInt())
        panel.background = bgPanel
        panel.addView(label("⚡ Menu rapide", 26f, Color.WHITE, true))

        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        fun btn(text: String, close: Boolean, action: () -> Unit): TextView {
            val b = TextView(this)
            b.text = text
            b.setTextSize(TypedValue.COMPLEX_UNIT_PX, 17f * unit)
            b.setTextColor(Color.WHITE)
            b.gravity = Gravity.CENTER
            b.isFocusable = true
            b.isClickable = true
            val bg = GradientDrawable()
            bg.setColor(0x33FFFFFF)
            bg.cornerRadius = 16f * unit
            b.background = bg
            b.setOnFocusChangeListener { v, f ->
                bg.setColor(if (f) 0xFF7C4DFF.toInt() else 0x33FFFFFF)
                val s = if (f) 1.06f else 1f
                v.animate().scaleX(s).scaleY(s).setDuration(100).start()
            }
            b.setOnClickListener {
                if (close) d.dismiss()
                action()
            }
            return b
        }

        fun row(vararg bs: TextView): LinearLayout {
            val r = LinearLayout(this)
            r.orientation = LinearLayout.HORIZONTAL
            for (b in bs) {
                val lp = LinearLayout.LayoutParams(0, u(72f), 1f)
                lp.setMargins(u(6f), u(6f), u(6f), u(6f))
                r.addView(b, lp)
            }
            return r
        }

        val first = btn("🔉  Volume −", false) {
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        panel.addView(
            row(
                first,
                btn("🔊  Volume +", false) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                },
                btn("🔇  Muet", false) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
                }
            )
        )
        panel.addView(
            row(
                btn("📶  Wi-Fi", true) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                btn("🛠  Paramètres", true) { startActivity(Intent(Settings.ACTION_SETTINGS)) },
                btn("⏭  Image suivante", false) { nextWall() }
            )
        )
        panel.addView(
            row(
                btn("📦  Nouvelles images", true) {
                    wall.invalidate()
                    ensurePack()
                },
                btn("✕  Fermer", true) { }
            )
        )
        d.setContentView(panel, ViewGroup.LayoutParams(u(780f), ViewGroup.LayoutParams.WRAP_CONTENT))
        d.show()
        first.requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            showMenu()
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        ensurePack()
        handler.removeCallbacks(rotate)
        handler.postDelayed(rotate, 1500L)
        handler.removeCallbacks(infoTick)
        handler.post(infoTick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(rotate)
        handler.removeCallbacks(infoTick)
    }
}
