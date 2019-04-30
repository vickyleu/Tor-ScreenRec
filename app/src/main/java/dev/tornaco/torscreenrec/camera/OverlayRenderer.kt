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
import android.view.MotionEvent

abstract class OverlayRenderer : RenderOverlay.Renderer {
    var mOverlay: RenderOverlay? = null

    protected var mLeft: Int = 0
    protected var mTop: Int = 0
    protected var mRight: Int = 0
    protected var mBottom: Int = 0

    protected var mVisible: Boolean = false

    var isVisible: Boolean
        get() = mVisible
        set(vis) {
            mVisible = vis
            update()
        }

    protected val context: Context?
        get() = if (mOverlay != null) {
            mOverlay!!.context
        } else {
            null
        }

    val width: Int
        get() = mRight - mLeft

    val height: Int
        get() = mBottom - mTop

    // default does not handle touch
    override fun handlesTouch(): Boolean {
        return false
    }

    override fun onTouchEvent(evt: MotionEvent): Boolean {
        return false
    }

    abstract fun onDraw(canvas: Canvas)

    override fun draw(canvas: Canvas) {
        if (mVisible) {
            onDraw(canvas)
        }
    }

    override fun setOverlay(overlay: RenderOverlay?) {
        mOverlay = overlay
    }

    override fun layout(left: Int, top: Int, right: Int, bottom: Int) {
        mLeft = left
        mRight = right
        mTop = top
        mBottom = bottom
    }

    protected fun update() {
        if (mOverlay != null) {
            mOverlay!!.update()
        }
    }

    companion object {

        private val TAG = "CAM OverlayRenderer"
    }

}