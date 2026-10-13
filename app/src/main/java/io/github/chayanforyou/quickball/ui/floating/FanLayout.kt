package io.github.chayanforyou.quickball.ui.floating

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Geometry of the radial menu, shared by the overlay and the settings preview.
 *
 * [offsets] are the item centers relative to the FAB center for a FAB on the right edge
 * (items fan out to the left), first item on top. All lengths use the unit of the inputs.
 */
class FanLayout(
    val offsets: List<Pair<Float, Float>>,
    val arcRadius: Float,
    val arcEdgeAngle: Float
) {
    companion object {
        // Largest item count that still fits on a single arc
        const val SINGLE_RING_MAX = 5

        // Widest fan, used when a small menu radius needs more room between items
        const val MAX_SWEEP_ANGLE = 160.0

        const val ITEM_GAP_DP = 6f

        // Gap between the connector arc and the nearest items
        const val ARC_GAP_DP = 8f
        const val ARC_MIN_INSET_DP = 4f

        // Gap between the FAB and the start of a straight or dotted connector
        const val CONNECTOR_INSET_DP = 8f

        // Dot diameter and distance between dot centers, relative to the connector width
        const val DOT_SIZE_RATIO = 1.75f
        const val DOT_SPACING_RATIO = 5f

        /**
         * Items are spread evenly around the horizontal axis. With more than
         * [SINGLE_RING_MAX] items they alternate between an outer and an inner arc.
         *
         * [radius] is the radius of the (inner) arc. A small radius widens the fan up to
         * [MAX_SWEEP_ANGLE] to keep items apart; only past that the radius grows.
         *
         * @param density length of one dp in the unit of the other inputs
         * @param maxHalfSpan largest vertical distance from the FAB center to an item center
         */
        fun compute(
            count: Int,
            buttonSize: Float,
            radius: Float,
            halfFab: Float,
            density: Float,
            maxHalfSpan: Float = Float.MAX_VALUE
        ): FanLayout {
            val isTwoRings = count > SINGLE_RING_MAX
            val itemGap = ITEM_GAP_DP * density
            val minChord = buttonSize + itemGap

            // Neighbours on the same arc are two steps apart in the two-arc layout
            val ringStride = if (isTwoRings) 2 else 1
            val ringSpacing = if (isTwoRings) buttonSize else 0f

            // Keep the items clear of the FAB itself
            var innerRadius = max(radius, halfFab + buttonSize / 2f + itemGap)

            // Widen the fan when the items would overlap at this radius
            val preferredStep = Math.toRadians(180.0 / (count + 1))
            val maxStep = Math.toRadians(MAX_SWEEP_ANGLE / (count - 1))
            val fitStep = 2 * asin(min(1.0, minChord / (2.0 * innerRadius))) / ringStride
            val stepRad = max(preferredStep, fitStep).coerceAtMost(maxStep)
            val edgeAngleRad = (count - 1) / 2.0 * stepRad

            // Smallest radius that still keeps the items apart at this step
            val minRadius = (minChord / (2 * sin(stepRad * ringStride / 2))).toFloat()
            val maxRadius = (maxHalfSpan / sin(edgeAngleRad)).toFloat() - ringSpacing

            innerRadius = max(minRadius, min(innerRadius, maxRadius))
            val outerRadius = innerRadius + ringSpacing

            val offsets = List(count) { index ->
                val ringRadius = if (isTwoRings && index % 2 == 0) outerRadius else innerRadius
                val angleRad = edgeAngleRad - index * stepRad

                Pair(
                    (-cos(angleRad) * ringRadius).toFloat(),
                    (-sin(angleRad) * ringRadius).toFloat()
                )
            }

            val arcRadius = max(
                innerRadius - buttonSize / 2f - ARC_GAP_DP * density,
                halfFab + ARC_MIN_INSET_DP * density
            )

            return FanLayout(offsets, arcRadius, Math.toDegrees(edgeAngleRad).toFloat())
        }
    }
}
