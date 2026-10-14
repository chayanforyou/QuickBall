package io.github.chayanforyou.quickball.ui.floating

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import io.github.chayanforyou.quickball.domain.models.MenuConnectorStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws menu connectors in one [MenuConnectorStyle] on an Android [Canvas].
 *
 * The overlay ([FloatPanelView]) and the Compose previews (through the native canvas) both
 * draw with this class, so each style is defined once and looks the same everywhere.
 *
 * Positions are given by the caller: a center point (the FAB), a unit direction toward an
 * item, and distances from the center where the connector starts and ends. The caller owns
 * the geometry and the animation; this class only owns how a style looks.
 *
 * @param color connector color, including its alpha
 * @param strokeWidth line width in pixels; dots, dashes, waves and the rest scale with it
 */
class ConnectorRenderer(
    val style: MenuConnectorStyle,
    color: Int,
    private val strokeWidth: Float
) {

    companion object {
        // Gap between the FAB and the start of a connector
        const val INSET_DP = 8f

        // Dot diameter and distance between dot centers, relative to the line width
        private const val DOT_SIZE_RATIO = 1.75f
        private const val DOT_SPACING_RATIO = 5f

        // Dash length and gap, relative to the line width (round caps add one width)
        private const val DASH_LENGTH_RATIO = 3f
        private const val DASH_GAP_RATIO = 4f

        // How far a curved connector bows out, relative to its length, for the outermost items
        private const val CURVE_BEND = 0.22f

        // Taper: width at the FAB and at the item, relative to the line width
        private const val TAPER_START_RATIO = 3f
        private const val TAPER_END_RATIO = 0.4f

        // Wave: crest height and length of one full wave, relative to the line width.
        // The length follows the height, so changing the height keeps the same wave shape
        private const val WAVE_AMPLITUDE_RATIO = 1f
        private const val WAVE_LENGTH_RATIO = WAVE_AMPLITUDE_RATIO * 8f
        private const val WAVE_SEGMENTS_PER_HALF = 8

        // Zigzag: tooth height and length of one full tooth pair, relative to the line width.
        // Taller and tighter than the wave, with sharp corners, so the two look different
        private const val ZIGZAG_AMPLITUDE_RATIO = 1.3f
        private const val ZIGZAG_LENGTH_RATIO = ZIGZAG_AMPLITUDE_RATIO * 3.4f

        // Double: width of each rail and its distance from the center line
        private const val DOUBLE_RAIL_RATIO = 0.7f
        private const val DOUBLE_OFFSET_RATIO = 1.1f

        // Glow: halo layers drawn under the line, as (width, opacity) relative to the line
        private val GLOW_LAYERS = listOf(8f to 0.08f, 5f to 0.14f, 2.6f to 0.3f)

        // Fade: opacity left at the item end, so the line still reaches it
        private const val FADE_END_ALPHA = 0.2f

        // Bubble: dot size at the FAB and at the item, relative to the regular dot size
        private const val BUBBLE_START_RATIO = 0.5f
        private const val BUBBLE_END_RATIO = 1.6f
    }

    /** Ring styles also draw an arc around the FAB through the start of every connector. */
    val hasRing = style == MenuConnectorStyle.LINE_RING || style == MenuConnectorStyle.DOT_RING

    private val isDotted = style == MenuConnectorStyle.DOTTED || style == MenuConnectorStyle.DOT_RING

    private val baseAlpha = Color.alpha(color)
    private val opaqueColor = color or 0xFF000000.toInt()
    private val fadeEndColor = (color and 0x00FFFFFF) or ((FADE_END_ALPHA * 255).toInt() shl 24)

    private val dotRadius = strokeWidth * DOT_SIZE_RATIO / 2f
    private val dotSpacing = strokeWidth * DOT_SPACING_RATIO

    private val path = Path()
    private val bounds = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        strokeWidth = this@ConnectorRenderer.strokeWidth
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * Arc around a center: the ring of the ring styles, or the arc of the Arc style.
     * Dotted styles draw it as evenly spaced dots, with a dot exactly on each end.
     *
     * @param startAngle start in radians; 0 points right, angles grow clockwise
     * @param sweepAngle length of the arc in radians
     * @param opacity 0 to 1, multiplied with the color's own alpha
     */
    fun drawRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Double,
        sweepAngle: Double,
        opacity: Float
    ) {
        if (opacity <= 0f) return
        setOpacity(opacity)

        if (isDotted) {
            paint.style = Paint.Style.FILL
            val steps = max(1, (radius * sweepAngle / dotSpacing).roundToInt())
            for (step in 0..steps) {
                val angle = startAngle + sweepAngle * step / steps
                canvas.drawCircle(
                    centerX + (cos(angle) * radius).toFloat(),
                    centerY + (sin(angle) * radius).toFloat(),
                    dotRadius,
                    paint
                )
            }
            return
        }

        paint.style = Paint.Style.STROKE
        bounds.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawArc(
            bounds,
            Math.toDegrees(startAngle).toFloat(),
            Math.toDegrees(sweepAngle).toFloat(),
            false,
            paint
        )
    }

    /**
     * One connector, from [start] to [end] along a unit direction from the center.
     *
     * @param opacity 0 to 1, multiplied with the color's own alpha
     * @param bend Curve style only: -1 to 1, how far and to which side the line bows
     */
    fun drawConnector(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        directionX: Float,
        directionY: Float,
        start: Float,
        end: Float,
        opacity: Float,
        bend: Float = 0f
    ) {
        if (end <= start || opacity <= 0f) return
        setOpacity(opacity)

        val line = Line(centerX, centerY, directionX, directionY, start, end)

        when (style) {
            MenuConnectorStyle.NONE -> Unit
            MenuConnectorStyle.DOTTED,
            MenuConnectorStyle.DOT_RING -> drawDots(canvas, line)
            MenuConnectorStyle.DASH -> drawDashes(canvas, line)
            MenuConnectorStyle.FADE -> drawFade(canvas, line)
            MenuConnectorStyle.CURVE -> drawCurve(canvas, line, bend)
            MenuConnectorStyle.TAPER -> drawTaper(canvas, line)
            MenuConnectorStyle.WAVE -> drawPolyline(canvas, wavePoints(line), Paint.Join.ROUND)
            MenuConnectorStyle.ZIGZAG -> drawPolyline(canvas, zigzagPoints(line), Paint.Join.MITER)
            MenuConnectorStyle.DOUBLE -> drawDouble(canvas, line)
            MenuConnectorStyle.GLOW -> drawGlow(canvas, line)
            MenuConnectorStyle.BUBBLE -> drawBubbles(canvas, line)
            MenuConnectorStyle.LINE,
            MenuConnectorStyle.LINE_RING,
            MenuConnectorStyle.ARC -> drawStraight(canvas, line)
        }
    }

    private fun setOpacity(opacity: Float) {
        paint.alpha = (baseAlpha * opacity.coerceIn(0f, 1f)).toInt()
    }

    // ---- Styles -----------------------------------------------------------------------------

    private fun drawStraight(canvas: Canvas, line: Line) {
        paint.style = Paint.Style.STROKE
        canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
    }

    private fun drawDots(canvas: Canvas, line: Line) {
        paint.style = Paint.Style.FILL
        // With a ring, the first dot of each line is on the ring, so start one step out
        var position = if (hasRing) line.start + dotSpacing else line.start
        // Stop before a dot would touch the item
        while (position <= line.end - dotRadius) {
            canvas.drawCircle(line.x(position), line.y(position), dotRadius, paint)
            position += dotSpacing
        }
    }

    private fun drawDashes(canvas: Canvas, line: Line) {
        paint.style = Paint.Style.STROKE
        val dashLength = strokeWidth * DASH_LENGTH_RATIO
        val dashGap = strokeWidth * DASH_GAP_RATIO

        // Round caps reach half a width past each end, so keep them off the item
        val lastEnd = line.end - strokeWidth / 2f
        var position = line.start
        while (position < lastEnd) {
            val dashEnd = min(position + dashLength, lastEnd)
            // Skip a leftover piece too short to read as a dash
            if (dashEnd - position < strokeWidth) break
            canvas.drawLine(line.x(position), line.y(position), line.x(dashEnd), line.y(dashEnd), paint)
            position += dashLength + dashGap
        }
    }

    private fun drawFade(canvas: Canvas, line: Line) {
        // Full color at the FAB, fading toward the item
        paint.style = Paint.Style.STROKE
        paint.shader = LinearGradient(
            line.startX, line.startY, line.endX, line.endY,
            opaqueColor, fadeEndColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
        paint.shader = null
    }

    private fun drawCurve(canvas: Canvas, line: Line, bend: Float) {
        // Bow sideways: control point beside the midpoint
        val offset = (line.end - line.start) * CURVE_BEND * bend
        val middle = (line.start + line.end) / 2f

        paint.style = Paint.Style.STROKE
        path.reset()
        path.moveTo(line.startX, line.startY)
        path.quadTo(line.x(middle, offset), line.y(middle, offset), line.endX, line.endY)
        canvas.drawPath(path, paint)
    }

    private fun drawTaper(canvas: Canvas, line: Line) {
        // Filled wedge: wide at the FAB, narrow at the item
        val startHalf = strokeWidth * TAPER_START_RATIO / 2f
        val endHalf = strokeWidth * TAPER_END_RATIO / 2f

        paint.style = Paint.Style.FILL
        path.reset()
        path.moveTo(line.x(line.start, startHalf), line.y(line.start, startHalf))
        path.lineTo(line.x(line.end, endHalf), line.y(line.end, endHalf))
        path.lineTo(line.x(line.end, -endHalf), line.y(line.end, -endHalf))
        path.lineTo(line.x(line.start, -startHalf), line.y(line.start, -startHalf))
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawPolyline(canvas: Canvas, points: FloatArray, join: Paint.Join) {
        paint.style = Paint.Style.STROKE
        paint.strokeJoin = join
        path.reset()
        path.moveTo(points[0], points[1])
        for (i in 2 until points.size step 2) path.lineTo(points[i], points[i + 1])
        canvas.drawPath(path, paint)
        paint.strokeJoin = Paint.Join.ROUND
    }

    private fun drawDouble(canvas: Canvas, line: Line) {
        // Two thin rails, one on each side of the center line
        val offset = strokeWidth * DOUBLE_OFFSET_RATIO

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth * DOUBLE_RAIL_RATIO
        for (side in floatArrayOf(-offset, offset)) {
            canvas.drawLine(
                line.x(line.start, side), line.y(line.start, side),
                line.x(line.end, side), line.y(line.end, side),
                paint
            )
        }
        paint.strokeWidth = strokeWidth
    }

    private fun drawGlow(canvas: Canvas, line: Line) {
        // Wide faint layers under the line read as a soft glow
        val lineAlpha = paint.alpha

        paint.style = Paint.Style.STROKE
        for ((widthRatio, opacity) in GLOW_LAYERS) {
            paint.strokeWidth = strokeWidth * widthRatio
            paint.alpha = (lineAlpha * opacity).toInt()
            canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
        }
        paint.strokeWidth = strokeWidth
        paint.alpha = lineAlpha
        canvas.drawLine(line.startX, line.startY, line.endX, line.endY, paint)
    }

    private fun drawBubbles(canvas: Canvas, line: Line) {
        // Dots grow from small at the FAB to large at the item
        paint.style = Paint.Style.FILL
        var position = line.start
        while (position < line.end) {
            val fraction = (position - line.start) / (line.end - line.start)
            val scale = BUBBLE_START_RATIO + (BUBBLE_END_RATIO - BUBBLE_START_RATIO) * fraction
            // Stop before a bubble would touch the item
            if (position + dotRadius * scale > line.end) break
            canvas.drawCircle(line.x(position), line.y(position), dotRadius * scale, paint)
            position += dotSpacing * (0.6f + scale * 0.5f)
        }
    }

    // ---- Wave shapes --------------------------------------------------------------------

    /**
     * Sine wave along the line, as alternating x, y. A whole number of half waves, so it
     * meets both ends on the center line.
     */
    private fun wavePoints(line: Line): FloatArray {
        val length = line.end - line.start
        val halfWave = strokeWidth * WAVE_LENGTH_RATIO / 2f
        val halves = max(1, (length / halfWave).roundToInt())
        val amplitude = strokeWidth * WAVE_AMPLITUDE_RATIO
        val steps = halves * WAVE_SEGMENTS_PER_HALF

        return FloatArray((steps + 1) * 2) { i ->
            val fraction = (i / 2).toFloat() / steps
            val along = line.start + length * fraction
            val across = sin(fraction * halves * PI).toFloat() * amplitude
            if (i % 2 == 0) line.x(along, across) else line.y(along, across)
        }
    }

    /**
     * Triangle wave along the line, as alternating x, y corner points. Every slope is
     * equal: peaks sit halfway along each half wave, and both ends sit on the center line.
     */
    private fun zigzagPoints(line: Line): FloatArray {
        val length = line.end - line.start
        val halfWave = strokeWidth * ZIGZAG_LENGTH_RATIO / 2f
        val halves = max(1, (length / halfWave).roundToInt())
        val amplitude = strokeWidth * ZIGZAG_AMPLITUDE_RATIO

        // Start point, one peak per half wave, end point
        val pointCount = halves + 2
        return FloatArray(pointCount * 2) { i ->
            val point = i / 2
            val (fraction, across) = when (point) {
                0 -> 0f to 0f
                pointCount - 1 -> 1f to 0f
                else -> {
                    val peak = point - 1
                    (peak + 0.5f) / halves to if (peak % 2 == 0) amplitude else -amplitude
                }
            }
            val along = line.start + length * fraction
            if (i % 2 == 0) line.x(along, across) else line.y(along, across)
        }
    }

    /**
     * A connector's axis: positions are measured [along] the direction from the center, and
     * [across] it, positive to the clockwise side.
     */
    private class Line(
        val centerX: Float,
        val centerY: Float,
        val directionX: Float,
        val directionY: Float,
        val start: Float,
        val end: Float
    ) {
        val startX get() = x(start)
        val startY get() = y(start)
        val endX get() = x(end)
        val endY get() = y(end)

        fun x(along: Float, across: Float = 0f) = centerX + directionX * along - directionY * across
        fun y(along: Float, across: Float = 0f) = centerY + directionY * along + directionX * across
    }
}
