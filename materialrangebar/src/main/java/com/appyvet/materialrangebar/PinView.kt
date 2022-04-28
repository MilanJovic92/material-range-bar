/*
 * Copyright 2014, Appyvet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.appyvet.materialrangebar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.abs
import kotlin.math.max

/**
 * Represents a thumb in the RangeBar slider. This is the handle for the slider
 * that is pressed and slid.
 */
class PinView(context: Context) : View(context) {

    companion object {
        // The radius (in dp) of the touchable area around the thumb. We are basing
        // this value off of the recommended 48dp Rhythm. See:
        // http://developer.android.com/design/style/metrics-grids.html#48dp-rhythm
        private const val MINIMUM_TARGET_RADIUS_DP = 24f

        // Sets the default values for radius, normal, pressed if circle is to be
        // drawn but no value is given.
        private const val DEFAULT_THUMB_RADIUS_DP = 14f
    }

    /**
     * Radius (in pixels) of the touch area of the thumb.
     */
    private var targetRadiusPx = 0f

    /**
     * Paint to draw the thumbs if attributes are selected
     */
    private val textPaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }
    private val pinDrawable: Drawable by lazy {
        ContextCompat.getDrawable(context, R.drawable.rotate)
            ?: throw IllegalStateException("Pin drawable not found")
    }

    var formatter: RangeBarFormatter? = null

    /**
     * The value of the pin.
     */
    var xValue: String = ""

    // Radius of the new thumb if selected
    private var pinRadiusPx = 0
    private var pinPadding = 0f
    private var textYPadding = 0f
    private val bounds = Rect()
    private val density by lazy {
        context.resources.displayMetrics.density
            .coerceAtLeast(1F)
    }
    private val circlePaint: Paint by lazy {
        Paint().apply {
            isAntiAlias = true
        }
    }
    private var circleBoundaryPaint: Paint? = null
    private var circleRadiusPx = 0f
    private var circleBoundaryRadiusPx = 0f
    private var minPinFont = RangeBar.DEFAULT_MIN_PIN_FONT_SP
    private var maxPinFont = RangeBar.DEFAULT_MAX_PIN_FONT_SP
    private var pinsAreTemporary = false
    private var hasBeenPressed = false
    private var pinColor = 0

    /**
     * The view is created empty with a default constructor. Use init to set all the initial
     * variables for the pin
     *
     * @param ctx                 Context
     * @param y                   The y coordinate to raw the pin (i.e. the bar location)
     * @param pinRadiusDP         the initial size of the pin
     * @param pinColor            the color of the pin
     * @param textColor           the color of the value text in the pin
     * @param circleRadius        the radius of the selector circle
     * @param circleColor         the color of the selector circle
     * @param circleBoundaryColor The color of the selector circle boundary
     * @param circleBoundarySize  The size of the selector circle boundary line
     * @param minFont             the minimum font size for the pin text
     * @param maxFont             the maximum font size for the pin text
     * @param pinsAreTemporary    whether to show the pin initially or just the circle
     */
    fun init(
        ctx: Context,
        y: Float,
        pinRadiusDP: Float,
        pinColor: Int,
        textColor: Int,
        circleRadius: Float,
        circleColor: Int,
        circleBoundaryColor: Int,
        circleBoundarySize: Float,
        minFont: Float,
        maxFont: Float,
        pinsAreTemporary: Boolean
    ) {
        val displayMetrics = ctx.resources.displayMetrics
        minPinFont = minFont / density
        maxPinFont = maxFont / density
        this.pinsAreTemporary = pinsAreTemporary
        pinPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, displayMetrics)
        circleRadiusPx = circleRadius
        textYPadding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.5f, displayMetrics)
        // If one of the attributes are set, but the others aren't, set the
        // attributes to default
        pinRadiusPx = if (pinRadiusDP == -1f) {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_THUMB_RADIUS_DP, displayMetrics)
        } else {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pinRadiusDP, displayMetrics)
        }.toInt()
        //Set text size in px from dp
        val textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 15f, displayMetrics)

        textPaint.color = textColor
        textPaint.textSize = textSize
        // Creates the paint and sets the Paint values
        circlePaint.color = circleColor
        if (circleBoundarySize != 0f) {
            circleBoundaryPaint = Paint().apply {
                style = Paint.Style.STROKE
                color = circleBoundaryColor
                strokeWidth = circleBoundarySize
                isAntiAlias = true
                circleBoundaryRadiusPx = circleRadiusPx - strokeWidth / 2
            }
        }
        this.pinColor = pinColor

        // Sets the minimum touchable area, but allows it to expand based on
        // image size
        val targetRadius = max(MINIMUM_TARGET_RADIUS_DP, pinRadiusPx.toFloat())
        targetRadiusPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, targetRadius, displayMetrics)
        this.y = y
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        hasBeenPressed = hasBeenPressed || pressed
    }

    /**
     * Set size of the pin and padding for use when animating pin enlargement on press
     *
     * @param size    the size of the pin radius
     * @param padding the size of the padding
     */
    fun setSize(size: Float, padding: Float) {
        pinPadding = padding
        pinRadiusPx = size.toInt()
        invalidate()
    }

    /**
     * Determines if the input coordinate is close enough to this thumb to
     * consider it a press.
     *
     * @param targetX the x-coordinate of the user touch
     * @param targetY the y-coordinate of the user touch
     * @return true if the coordinates are within this thumb's target area;
     * false otherwise
     */
    fun isInTargetZone(targetX: Float, targetY: Float): Boolean {
        return abs(targetX - x) <= targetRadiusPx && abs(targetY - y + pinPadding) <= targetRadiusPx
    }

    //Draw the circle regardless of pressed state. If pin size is >0 then also draw the pin and text
    override fun draw(canvas: Canvas) {
        canvas.drawCircle(x, y, circleRadiusPx, circlePaint)

        //Draw the circle boundary only if mCircleBoundaryPaint was initialized
        circleBoundaryPaint?.let { paint ->
            canvas.drawCircle(x, y, circleBoundaryRadiusPx, paint)
        }

        //Draw pin if pressed
        if (pinRadiusPx > 0 && (hasBeenPressed || !pinsAreTemporary)) {
            bounds.set(
                x.toInt() - pinRadiusPx,
                y.toInt() - pinRadiusPx * 2 - pinPadding.toInt(),
                x.toInt() + pinRadiusPx,
                y.toInt() - pinPadding.toInt()
            )
            pinDrawable.bounds = bounds
            val text = formatter?.format(xValue) ?: xValue
            calibrateTextSize(textPaint, text, bounds.width().toFloat())
            textPaint.getTextBounds(text, 0, text.length, bounds)
            textPaint.textAlign = Paint.Align.CENTER
            DrawableCompat.setTint(pinDrawable, pinColor)
            pinDrawable.draw(canvas)
            canvas.drawText(text, x, y - pinRadiusPx - pinPadding + textYPadding, textPaint)
        }
        super.draw(canvas)
    }

    /**
     * Set text size based on available pin width.
     */
    private fun calibrateTextSize(paint: Paint, text: String?, boxWidth: Float) {
        paint.textSize = 10f
        val textSize = paint.measureText(text)
        val estimatedFontSize = (boxWidth * 8 / textSize / density)
            .coerceIn(minPinFont, maxPinFont)
        paint.textSize = estimatedFontSize * density
    }
}