/*
 * Copyright (C) 2015 The Android Open Source Project
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

package dev.tornaco.torscreenrec.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.drawable.Drawable

import java.util.ArrayList

/**
 * Pie menu item
 */
class PieItem(private var mDrawable: Drawable?, val level: Int) {
    var center: Float = 0.toFloat()
        private set
    var start: Float = 0.toFloat()
        private set
    var sweep: Float = 0.toFloat()
        private set
    var animationAngle: Float = 0.toFloat()
    var innerRadius: Int = 0
        private set
    var outerRadius: Int = 0
        private set
    var isSelected: Boolean = false
    private var mEnabled: Boolean = false
    private var mItems: MutableList<PieItem>? = null
    var path: Path? = null
    private var mOnClickListener: OnClickListener? = null
    private var mAlpha: Float = 0.toFloat()
    private var mChangeAlphaWhenDisabled = true

    val items: List<PieItem>?
        get() = mItems

    var isEnabled: Boolean
        get() = mEnabled
        set(enabled) {
            mEnabled = enabled
            if (mChangeAlphaWhenDisabled) {
                if (mEnabled) {
                    setAlpha(ENABLED_ALPHA)
                } else {
                    setAlpha(DISABLED_ALPHA)
                }
            }
        }

    val startAngle: Float
        get() = start + animationAngle

    val intrinsicWidth: Int
        get() = mDrawable!!.intrinsicWidth

    val intrinsicHeight: Int
        get() = mDrawable!!.intrinsicHeight

    interface OnClickListener {
        fun onClick(item: PieItem)
    }

    init {
        setAlpha(1f)
        mEnabled = true
        animationAngle = animationAngle
        start = -1f
        center = -1f
    }

    fun hasItems(): Boolean {
        return mItems != null
    }

    fun addItem(item: PieItem) {
        if (mItems == null) {
            mItems = ArrayList()
        }
        mItems!!.add(item)
    }

    fun setChangeAlphaWhenDisabled(enable: Boolean) {
        mChangeAlphaWhenDisabled = enable
    }

    fun setAlpha(alpha: Float) {
        mAlpha = alpha
        mDrawable!!.alpha = (255 * alpha).toInt()
    }

    fun setGeometry(st: Float, sw: Float, inside: Int, outside: Int) {
        start = st
        sweep = sw
        innerRadius = inside
        outerRadius = outside
    }

    fun setFixedSlice(center: Float, sweep: Float) {
        this.center = center
        this.sweep = sweep
    }

    fun setOnClickListener(listener: OnClickListener) {
        mOnClickListener = listener
    }

    fun performClick() {
        if (mOnClickListener != null) {
            mOnClickListener!!.onClick(this)
        }
    }

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        mDrawable!!.setBounds(left, top, right, bottom)
    }

    fun draw(canvas: Canvas) {
        mDrawable!!.draw(canvas)
    }

    fun setImageResource(context: Context, resId: Int) {
        val d = context.resources.getDrawable(resId).mutate()
        d.bounds = mDrawable!!.bounds
        mDrawable = d
        setAlpha(mAlpha)
    }

    companion object {

        // Gray out the view when disabled
        private val ENABLED_ALPHA = 1f
        private val DISABLED_ALPHA = 0.3.toFloat()
    }

}