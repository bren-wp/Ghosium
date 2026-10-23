package com.brendigo.ghosium

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.view.View

/** Original native Canvas artwork; no web assets or fake screenshot controls. */
object GhosiumColors {
    const val NIGHT = 0xFF090D16.toInt()
    const val SURFACE = 0xEE141C2A.toInt()
    const val SURFACE2 = 0xF0101929.toInt()
    const val OUTLINE = 0xFF30405B.toInt()
    const val WHITE = 0xFFF6F8FC.toInt()
    const val MUTED = 0xFF9BAAC0.toInt()
    const val MINT = 0xFF58F4CD.toInt()
    const val SKY = 0xFF62B3FF.toInt()
    const val VIOLET = 0xFF9889FF.toInt()
}

/** Midnight gradient, tiny stars, an aurora and mountains / lake inspired by Ghosium mockups. */
class AuroraDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mountain = Path()
    override fun draw(canvas: Canvas) {
        val b = bounds
        val w = b.width().toFloat().coerceAtLeast(1f)
        val h = b.height().toFloat().coerceAtLeast(1f)
        val save = canvas.save()
        canvas.translate(b.left.toFloat(), b.top.toFloat())
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f,0f,0f,h, intArrayOf(0xFF050B16.toInt(),
            0xFF0B1730.toInt(), 0xFF15173A.toInt(), 0xFF041321.toInt()),
            floatArrayOf(0f, .42f, .76f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(0f,0f,w,h,paint)
        paint.shader = null
        val cy = h * .79f
        for (i in 0 until 4) {
            val x = w * (0.14f + i * .24f)
            val rad = w * .6f
            paint.shader = RadialGradient(x,cy - h * .08f,rad,
                if (i%2==0) intArrayOf(0x6644DCCF,0x0044DCCF) else intArrayOf(0x665D42B9,0x005D42B9),
                null,Shader.TileMode.CLAMP)
            canvas.drawOval(x-rad,cy-rad*.54f,x+rad,cy+rad*.54f,paint)
        }
        paint.shader = null
        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 1..75) {
            val x = ((i * 1741 % 997) / 997f) * w
            val y = ((i * 3271 % 991) / 991f) * h * .74f
            star.color = if (i%4==0) 0x9958F4CD.toInt() else 0x77B9C9FF.toInt()
            canvas.drawCircle(x,y,if (i%7==0) 1.65f else .8f,star)
        }
        fun ridge(height: Float, color: Int, phase: Int) {
            mountain.reset()
            mountain.moveTo(0f,h)
            mountain.lineTo(0f,height)
            for (i in 0..16) {
                val x = w*i/16f
                val peak = ((i*13+phase*11)%9).toFloat() / 9f
                mountain.lineTo(x,height-h*.07f*peak + if (i%2==0) h*.015f else 0f)
            }
            mountain.lineTo(w,h)
            mountain.close()
            paint.color = color
            paint.shader = null
            canvas.drawPath(mountain,paint)
        }
        ridge(h*.78f,0xEE101F35.toInt(),1)
        ridge(h*.84f,0xFF081729.toInt(),4)
        // Lake surface + reflections
        paint.shader = LinearGradient(0f,h*.88f,0f,h,
            intArrayOf(0xBB06233A.toInt(),0xBB0B1A2E.toInt(),0xFF07101D.toInt()),
            null,Shader.TileMode.CLAMP)
        canvas.drawRect(0f,h*.88f,w,h,paint)
        paint.shader=null
        val reflected = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 1..20) {
            reflected.color = if (i%3==0) 0x2249E3DB else 0x224A6CEB
            val y = h * (.88f + i*.0054f)
            val x = w * (.5f - (i%5)*.055f)
            canvas.drawRoundRect(x,y,w-x,y+1.6f,1f,1f,reflected)
        }
        canvas.restoreToCount(save)
    }
    override fun setAlpha(alpha: Int) { invalidateSelf() }
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Deprecated("Drawable opacity")
    override fun getOpacity() = PixelFormat.OPAQUE
}

/** Native Ghosium ghost-and-compass mark. Scales to launcher and content sizes. */
class GhosiumMark(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ghost = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val side = width.coerceAtMost(height).toFloat()
        if (side<=0f) return
        val saved=canvas.save()
        canvas.translate((width-side)/2f,(height-side)/2f)
        canvas.scale(side/200f,side/200f)
        p.style=Paint.Style.STROKE
        p.strokeWidth=10f
        p.strokeCap=Paint.Cap.ROUND
        p.shader = SweepGradient(100f,100f,
            intArrayOf(GhosiumColors.MINT,GhosiumColors.SKY,GhosiumColors.VIOLET,GhosiumColors.MINT),null)
        canvas.drawArc(28f,28f,172f,172f,205f,310f,false,p)
        p.shader=null
        p.style=Paint.Style.FILL
        p.color=GhosiumColors.MINT
        fun pointer(x:Float,y:Float,rot:Float) {
            val s=canvas.save()
            canvas.rotate(rot,x,y)
            val path=Path().apply { moveTo(x,y-13f);lineTo(x+6f,y+2f);lineTo(x-6f,y+2f);close() }
            canvas.drawPath(path,p)
            canvas.restoreToCount(s)
        }
        pointer(100f,19f,0f)
        pointer(100f,181f,180f)
        pointer(19f,100f,270f)
        pointer(181f,100f,90f)
        ghost.reset()
        ghost.moveTo(62f,130f)
        ghost.lineTo(62f,88f)
        ghost.cubicTo(62f,36f,138f,36f,138f,88f)
        ghost.lineTo(138f,143f)
        ghost.lineTo(121f,133f)
        ghost.lineTo(106f,143f)
        ghost.lineTo(91f,133f)
        ghost.lineTo(76f,143f)
        ghost.close()
        p.shader=LinearGradient(62f,70f,142f,144f,
            intArrayOf(0xFFFFFFFF.toInt(),0xFF85EAEF.toInt(),0xFFAD97FD.toInt()),
            null,Shader.TileMode.CLAMP)
        canvas.drawPath(ghost,p)
        p.shader=null
        p.color=0xFF0D1829.toInt()
        canvas.drawOval(87f,88f,96f,107f,p)
        canvas.drawOval(114f,88f,123f,107f,p)
        p.color=GhosiumColors.WHITE
        val compass=Path().apply {
            moveTo(110f,130f);lineTo(155f,108f);lineTo(133f,154f);lineTo(125f,140f);close()
        }
        canvas.drawPath(compass,p)
        p.color=GhosiumColors.SKY
        val inset=Path().apply {moveTo(125f,140f);lineTo(155f,108f);lineTo(133f,154f);close()}
        canvas.drawPath(inset,p)
        canvas.restoreToCount(saved)
    }
}
