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

import android.content.Context

import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import dev.nick.tiles.R

open class TileView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), View.OnClickListener {

    var imageView: ImageView? = null
        private set
    var titleTextView: TextView? = null
        private set
    var summaryTextView: TextView? = null
        private set
    private var mDivider: View? = null

    internal var columnSpan = DEFAULT_COL_SPAN

    protected val layoutId: Int
        get() = R.layout.dashboard_tile

    init {
        onCreate(context)
        val view = LayoutInflater.from(context).inflate(layoutId, this)
        onViewInflated(view)
    }

    protected open fun onCreate(context: Context) {}

    protected fun onViewInflated(view: View) {
        imageView = view.findViewById<View>(R.id.icon) as ImageView
        if (useStaticTintColor())
            imageView!!.setColorFilter(ContextCompat.getColor(context, R.color.tile_icon_tint))
        titleTextView = view.findViewById<View>(R.id.title) as TextView
        summaryTextView = view.findViewById<View>(R.id.status) as TextView
        mDivider = view.findViewById(R.id.tile_divider)

        onBindActionView(view.findViewById<View>(R.id.action_area) as RelativeLayout)

        setOnClickListener(this)

        setBackgroundResource(R.drawable.dashboard_tile_background)
        isFocusable = true
    }

    protected open fun onBindActionView(container: RelativeLayout) {}


    fun setDividerVisibility(visible: Boolean) {
        mDivider!!.visibility = if (visible) View.VISIBLE else View.GONE
    }

    protected open fun useStaticTintColor(): Boolean {
        return true
    }

    override fun onClick(v: View) {

    }

    @JvmOverloads
    protected fun generateCenterParams(w: Int = ViewGroup.LayoutParams.WRAP_CONTENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT): RelativeLayout.LayoutParams {
        val params = RelativeLayout.LayoutParams(w, h)
        params.addRule(RelativeLayout.CENTER_HORIZONTAL)
        params.addRule(RelativeLayout.CENTER_VERTICAL)
        return params
    }

    companion object {

        private val DEFAULT_COL_SPAN = 1
    }
}
