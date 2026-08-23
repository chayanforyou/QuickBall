package io.github.chayanforyou.quickball.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class BrightnessUtilsTest {

    @Test
    fun testMinAndMaxBrightnessConversion() {
        // Test 0% maps to min (1)
        val minLinear = BrightnessUtils.percentToLinear(0, 1, 255)
        assertEquals(1, minLinear)
        assertEquals(0, BrightnessUtils.linearToPercent(minLinear, 1, 255))

        // Test 100% maps to max (255)
        val maxLinear = BrightnessUtils.percentToLinear(100, 1, 255)
        assertEquals(255, maxLinear)
        assertEquals(100, BrightnessUtils.linearToPercent(maxLinear, 1, 255))
    }

    @Test
    fun testMidpointConversion() {
        // In AOSP HLG, 50% slider corresponds to ~1/12th of total physical luminance (~22 on 1..255)
        val midLinear = BrightnessUtils.percentToLinear(50, 1, 255)
        val midPercent = BrightnessUtils.linearToPercent(midLinear, 1, 255)
        assertTrue(abs(50 - midPercent) <= 1)
    }

    @Test
    fun testRoundTripConversionAcrossAllPercentages() {
        // Due to 8-bit quantization (255 steps), the lowest discrete steps (0-5%) quantize to integers 1-3.
        // Above 10%, roundtrip is accurate within 1-2%.
        for (p in 10..100) {
            val linear = BrightnessUtils.percentToLinear(p, 1, 255)
            val convertedBack = BrightnessUtils.linearToPercent(linear, 1, 255)
            assertTrue("Expected $p to roundtrip within 2% but got $convertedBack (linear: $linear)", abs(p - convertedBack) <= 2)
        }
    }
}
