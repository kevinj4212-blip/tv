package com.exemple.nova

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

// Une tuile dessinée à la main : très légère, coins arrondis, contour blanc quand elle est sélectionnée
class TileView(ctx: Context, private val unit: Float) : View(ctx) {
    private var bmp: Bitmap? = null
    private var shader: BitmapShader? = null
    var onFocusGain: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        isFocusable = true
        isClickable = true
        border.style = Paint.Style.STROKE
        border.color = Color.WHITE
    }

    fun setBitmap(b: Bitmap) {
        bmp = b
        shader = BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        updateShader()
        invalidate()
    }

    private fun updateShader() {
        val b = bmp ?: return
        val sh = shader ?: return
        if (width == 0 || height == 0) return
        val m = Matrix()
        m.setScale(width / b.width.toFloat(), height / b.height.toFloat())
        sh.setLocalMatrix(m)
        paint.shader = sh
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    override fun onDraw(canvas: Canvas) {
        if (bmp == null) return
        val r = 16f * unit
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(rect, r, r, paint)
        if (isFocused) {
            val sw = 4f * unit
            border.strokeWidth = sw
            rect.inset(sw / 2f, sw / 2f)
            canvas.drawRoundRect(rect, r, r, border)
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val s = if (gainFocus) 1.2f else 1f
        animate().scaleX(s).scaleY(s).translationZ(if (gainFocus) 20f else 0f).setDuration(130).start()
        if (gainFocus) onFocusGain?.invoke()
        invalidate()
    }
}

class TileAdapter(
    private val unit: Float,
    private val onClick: (Item) -> Unit,
    private val onFocused: (Item, Int) -> Unit
) : RecyclerView.Adapter<TileAdapter.VH>() {

    var items: List<Item> = emptyList()

    class VH(val tile: TileView) : RecyclerView.ViewHolder(tile)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val t = TileView(parent.context, unit)
        val lp = RecyclerView.LayoutParams((210 * unit).toInt(), (118 * unit).toInt())
        val m = (14 * unit).toInt()
        val v = (30 * unit).toInt()
        lp.setMargins(m, v, m, v)
        t.layoutParams = lp
        return VH(t)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val e = items[position]
        h.tile.setBitmap(e.icon)
        h.tile.setOnClickListener { onClick(e) }
        h.tile.onFocusGain = { onFocused(e, position) }
    }

    override fun getItemCount(): Int = items.size
}
