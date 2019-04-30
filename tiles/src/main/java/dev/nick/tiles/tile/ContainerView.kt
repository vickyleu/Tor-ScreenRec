/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nick.tiles.tile

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

import dev.nick.tiles.R

class ContainerView(context: Context, attrs: AttributeSet) : ViewGroup(context, attrs) {

    private val mCellGapX: Float
    private val mCellGapY: Float

    private var mNumRows: Int = 0
    private val mNumColumns: Int

    init {
        val res = context.resources
        mCellGapX = res.getDimension(R.dimen.dashboard_cell_gap_x)
        mCellGapY = res.getDimension(R.dimen.dashboard_cell_gap_y)
        mNumColumns = res.getInteger(R.integer.dashboard_num_columns)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val availableWidth = (width.toFloat() - paddingLeft.toFloat() - paddingRight.toFloat() -
                (mNumColumns - 1) * mCellGapX).toInt()
        val cellWidth = Math.ceil((availableWidth.toFloat() / mNumColumns).toDouble()).toFloat()
        val N = childCount

        var cellHeight = 0
        var cursor = 0

        for (i in 0 until N) {
            val v = getChildAt(i) as TileView
            if (v.visibility == View.GONE) {
                continue
            }

            val lp = v.layoutParams
            val colSpan = v.columnSpan
            lp.width = (colSpan * cellWidth + (colSpan - 1) * mCellGapX).toInt()

            // Measure the child
            val newWidthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, 0, lp.width)
            val newHeightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, 0, lp.height)
            v.measure(newWidthSpec, newHeightSpec)

            // Save the cell height
            if (cellHeight <= 0) {
                cellHeight = v.measuredHeight
            }

            lp.height = cellHeight

            cursor += colSpan
        }

        mNumRows = Math.ceil((cursor.toFloat() / mNumColumns).toDouble()).toInt()
        val newHeight = (mNumRows * cellHeight + (mNumRows - 1) * mCellGapY).toInt() +
                paddingTop + paddingBottom

        setMeasuredDimension(width, newHeight)
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val N = childCount
        val isLayoutRtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        val width = width

        var x = paddingStart
        var y = paddingTop
        var cursor = 0

        for (i in 0 until N) {
            val child = getChildAt(i) as TileView
            val lp = child.layoutParams
            if (child.visibility == View.GONE) {
                continue
            }

            val col = cursor % mNumColumns
            val colSpan = child.columnSpan

            val childWidth = lp.width
            val childHeight = lp.height

            var row = cursor / mNumColumns

            if (row == mNumRows - 1) {
                child.setDividerVisibility(false)
            } else {
                child.setDividerVisibility(true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                child.setDividerVisibility(false)
            }

            // Push the item to the next row if it can't fit on this one
            if (col + colSpan > mNumColumns) {
                x = paddingStart
                y += (childHeight + mCellGapY).toInt()
                row++
            }

            val childLeft = if (isLayoutRtl) width - x - childWidth else x
            val childRight = childLeft + childWidth

            val childTop = y
            val childBottom = childTop + childHeight

            // Layout the container
            child.layout(childLeft, childTop, childRight, childBottom)

            // Offset the position by the cell gap or reset the position and cursor when we
            // reach the end of the row
            cursor += child.columnSpan
            if (cursor < (row + 1) * mNumColumns) {
                x += (childWidth + mCellGapX).toInt()
            } else {
                x = paddingStart
                y += (childHeight + mCellGapY).toInt()
            }
        }
    }
}
