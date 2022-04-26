/*
 * Copyright 2013, Edmodo, Inc.
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
import android.util.TypedValue

/**
 * This class represents the underlying gray bar in the RangeBar (without the
 * thumbs).
 */
class Bar @JvmOverloads constructor(
    /**
     * The context.
     */
    ctx: Context,
    /**
     * The start x co-ordinate.
     */
    x: Float,
    /**
     * The start y co-ordinate.
     */
    private val y: Float,
    /**
     * The length of the bar in px.
     */
    length: Float,
    /**
     * The number of ticks on the bar.
     */
    tickCount: Int,
    /**
     * The height of each tick.
     */
    private val tickHeight: Float,
    /**
     * The weight of the bar.
     */
    barWeight: Float,
    /**
     * The color of the bar.
     */
    barColor: Int,
    /**
     * If the bar has rounded edges or not
     */
    isBarRounded: Boolean,
    /**
     * The color of all ticks.
     */
    private val tickDefaultColor: Int = 0,
    /**
     * The color of each tick's label.
     */
    private val tickLabelColor: Int = 0,
    /**
     * The color of selected tick's label.
     */
    private val tickLabelSelectedColor: Int = 0,
    /**
     * The text color of each tick's label.
     */
    private val tickColors: List<Int> = emptyList(),
    /**
     * The top label of each tick.
     */
    private val tickTopLabels: List<CharSequence> = emptyList(),
    /**
     * The bottom label of each tick.
     */
    private val tickBottomLabels: List<CharSequence> = emptyList(),
    /**
     * The default label for tick.
     */
    private val tickDefaultLabel: String = "",
    /**
     * The label text size.
     */
    tickLabelSize: Float = 0f
) {

    private val barPaint: Paint by lazy {
        Paint().apply {
            color = barColor
            strokeWidth = barWeight
            isAntiAlias = true
            if (isBarRounded) {
                strokeCap = Paint.Cap.ROUND
            }
        }
    }

    private val tickPaint: Paint by lazy {
        Paint().apply {
            strokeWidth = barWeight
            isAntiAlias = true
        }
    }

    private val labelPaint: Paint by lazy {
        val calculatedSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            tickLabelSize,
            ctx.resources.displayMetrics
        ).toInt()

        Paint().apply {
            color = tickLabelColor
            isAntiAlias = true
            textSize = calculatedSize.toFloat()
        }
    }

    /**
     * Get the x-coordinate of the left edge of the bar.
     *
     * @return x-coordinate of the left edge of the bar
     */
    val leftX: Float = x

    /**
     * Get the x-coordinate of the right edge of the bar.
     *
     * @return x-coordinate of the right edge of the bar
     */
    val rightX: Float = x + length

    private var numSegments: Int = tickCount - 1

    private var tickDistance: Float = length / numSegments

    /**
     * Draws the bar on the given Canvas.
     *
     * @param canvas Canvas to draw on; should be the Canvas passed into {#link
     * View#onDraw()}
     */
    fun draw(canvas: Canvas) {
        canvas.drawLine(leftX, y, rightX, y, barPaint)
    }

    /**
     * Gets the x-coordinate of the nearest tick to the given x-coordinate.
     *
     * @param thumb the thumb to find the nearest tick for
     * @return the x-coordinate of the nearest tick
     */
    fun getNearestTickCoordinate(thumb: PinView): Float {
        val nearestTickIndex = getNearestTickIndex(thumb)
        return leftX + nearestTickIndex * tickDistance
    }

    /**
     * Gets the zero-based index of the nearest tick to the given thumb.
     *
     * @param thumb the Thumb to find the nearest tick for
     * @return the zero-based index of the nearest tick
     */
    fun getNearestTickIndex(thumb: PinView): Int {
        var tickIndex = ((thumb.x - leftX + tickDistance / 2f) / tickDistance).toInt()
        if (tickIndex > numSegments) {
            tickIndex = numSegments
        } else if (tickIndex < 0) {
            tickIndex = 0
        }
        return tickIndex
    }

    fun getTickX(tickIndex: Int): Float {
        return leftX + (rightX - leftX) / numSegments * tickIndex
    }

    /**
     * Set the number of ticks that will appear in the RangeBar.
     *
     * @param tickCount the number of ticks
     */
    fun setTickCount(tickCount: Int) {
        val barLength = rightX - leftX
        numSegments = tickCount - 1
        tickDistance = barLength / numSegments
    }

    private fun getTickLabel(index: Int, labels: List<CharSequence>): String {
        return when {
            labels.isEmpty() -> ""
            else -> labels.getOrNull(index)?.toString() ?: tickDefaultLabel

        }
    }

    /**
     * Draws the tick marks on the bar.
     *
     * @param canvas Canvas to draw on; should be the Canvas passed into {#link
     * View#onDraw()}
     */
    @JvmOverloads
    fun drawTicks(canvas: Canvas, pinRadius: Float, rightThumb: PinView, leftThumb: PinView? = null) {
        // Loop through and draw each tick (except final tick).
        for (i in 0 until numSegments) {
            val x = i * tickDistance + leftX
            canvas.drawCircle(x, y, tickHeight, getTick(i))
            if (tickTopLabels.isNotEmpty()) {
                drawTickLabel(
                    canvas = canvas,
                    label = getTickLabel(i, tickTopLabels),
                    x = x,
                    pinRadius = pinRadius,
                    first = i == 0,
                    last = false,
                    isTop = true,
                    rightThumb = rightThumb,
                    leftThumb = leftThumb
                )
            }
            if (tickBottomLabels.isNotEmpty()) {
                drawTickLabel(
                    canvas = canvas,
                    label = getTickLabel(i, tickBottomLabels),
                    x = x,
                    pinRadius = pinRadius,
                    first = i == 0,
                    last = false,
                    isTop = false,
                    rightThumb = rightThumb,
                    leftThumb = leftThumb
                )
            }
        }
        // Draw final tick. We draw the final tick outside the loop to avoid any
        // rounding discrepancies.
        canvas.drawCircle(rightX, y, tickHeight, getTick(numSegments))

        // Draw final tick's label outside the loop
        if (tickTopLabels.isNotEmpty()) {
            drawTickLabel(
                canvas = canvas,
                label = getTickLabel(numSegments, tickTopLabels),
                x = rightX,
                pinRadius = pinRadius,
                first = false,
                last = true,
                isTop = true,
                rightThumb = rightThumb,
                leftThumb = leftThumb
            )
        }
        if (tickBottomLabels.isNotEmpty()) {
            drawTickLabel(
                canvas = canvas,
                label = getTickLabel(numSegments, tickBottomLabels),
                x = rightX,
                pinRadius = pinRadius,
                first = false,
                last = true,
                isTop = false,
                rightThumb = rightThumb,
                leftThumb = leftThumb
            )
        }
    }

    private fun drawTickLabel(
        canvas: Canvas, label: String, x: Float, pinRadius: Float,
        first: Boolean, last: Boolean, isTop: Boolean, rightThumb: PinView, leftThumb: PinView?
    ) {
        if (label.isBlank()) return

        val labelBounds = Rect()
        labelPaint.getTextBounds(label, 0, label.length, labelBounds)
        var xPos = x - labelBounds.width() / 2
        if (first) {
            xPos += tickHeight
        } else if (last) {
            xPos -= tickHeight
        }
        var isSelected = rightThumb.x == x
        if (!isSelected && leftThumb != null) {
            isSelected = leftThumb.x == x
        }
        if (isSelected) {
            labelPaint.color = tickLabelSelectedColor
        } else {
            labelPaint.color = tickLabelColor
        }
        val yPos: Float = if (isTop) {
            y - labelBounds.height() - pinRadius
        } else {
            y + labelBounds.height() + pinRadius
        }
        canvas.drawText(label, xPos, yPos, labelPaint)
    }

    private fun getTick(index: Int): Paint {
        tickPaint.color = tickColors.getOrNull(index) ?: tickDefaultColor
        return tickPaint
    }
}