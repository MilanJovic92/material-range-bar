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

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * Class representing the blue connecting line between the two thumbs.
 */
class ConnectingLine(
    /**
     * The y co-ordinate for the line.
     */
    private val y: Float,
    /**
     * The width of the line.
     */
    connectingLineWeight: Float,
    /**
     * The color of the line.
     */
    connectingLineColors: List<Int>
) {

    private val colors: IntArray

    private val positions: FloatArray

    private val paint by lazy {
        Paint().apply {
            strokeWidth = connectingLineWeight
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
    }

    init {

        //Need two colors
        val adjustedColors = if (connectingLineColors.size == 1) {
            connectingLineColors + connectingLineColors[0]
        } else {
            connectingLineColors
        }
        colors = IntArray(adjustedColors.size)
        positions = FloatArray(adjustedColors.size)

        adjustedColors.forEachIndexed { index, color ->
            colors[index] = color
            positions[index] = index.toFloat() / (adjustedColors.size - 1)
        }
    }

    /**
     * Draw the connecting line between the two thumbs in rangebar.
     *
     * @param canvas     the Canvas to draw to
     * @param leftThumb  the left thumb
     * @param rightThumb the right thumb
     */
    fun draw(canvas: Canvas, leftThumb: PinView, rightThumb: PinView) {
        paint.shader = getLinearGradient(0f, canvas.width.toFloat(), y)
        canvas.drawLine(leftThumb.x, y, rightThumb.x, y, paint)
    }

    /**
     * Draw the connecting line between for single slider.
     *
     * @param canvas     the Canvas to draw to
     * @param rightThumb the right thumb
     * @param leftMargin the left margin
     */
    fun draw(canvas: Canvas, leftMargin: Float, rightThumb: PinView) {
        paint.shader = getLinearGradient(0f, canvas.width.toFloat(), y)
        canvas.drawLine(leftMargin, y, rightThumb.x, y, paint)
    }

    private fun getLinearGradient(startX: Float, endX: Float, height: Float): LinearGradient {
        return LinearGradient(
            startX, height, endX, height,
            colors,
            positions,
            Shader.TileMode.REPEAT
        )
    }
}