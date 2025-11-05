package io.github.chayanforyou.quickball.utils

import android.animation.TimeInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SpringInterpolator(
    private var dampingRatio: Float = 0.95f,
    private var stiffness: Float = 0.6f
) : TimeInterpolator {

    private var omega: Float = 0f
    private var zeta: Float = 0f
    private var sqrtDiscriminant: Float = 0f
    private var a: Float = 0f
    private var b: Float = 0f
    private var initialPosition: Float = -1f
    private var initialVelocity: Float = 1f

    init {
        updateParameters()
    }

    private fun updateParameters() {
        val pow = (2.0 * PI / stiffness.toDouble()).pow(2.0)
        val mass = initialVelocity
        omega = (pow * mass).toFloat()

        zeta = ((dampingRatio * 4.0 * PI * mass) / stiffness).toFloat()

        val discriminant = (mass * 4.0f * omega) - (zeta * zeta)
        sqrtDiscriminant = sqrt(discriminant.toDouble()).toFloat() / (mass * 2.0f)

        a = -(zeta / 2.0f) * mass
        b = (-a * initialPosition) / sqrtDiscriminant
    }

    override fun getInterpolation(input: Float): Float {
        val expTerm = exp((a * input).toDouble())
        val cosTerm = cos((sqrtDiscriminant * input).toDouble())
        val sinTerm = sin((sqrtDiscriminant * input).toDouble())

        return (expTerm * (initialPosition * cosTerm + b * sinTerm) + 1.0).toFloat()
    }

    fun setDampingRatio(damping: Float) {
        this.dampingRatio = damping
        updateParameters()
    }

    fun setStiffness(stiffness: Float) {
        this.stiffness = stiffness
        updateParameters()
    }
}