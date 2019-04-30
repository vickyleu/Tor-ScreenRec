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
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

import java.util.ArrayList

class RenderOverlay(context: Context, attrs: AttributeSet) : FrameLayout(context, attrs) {

    private val mRenderView: RenderView
    private val mClients: MutableList<Renderer>?

    // reverse list of touch clients
    private val mTouchClients: MutableList<Renderer>?
    private val mPosition = IntArray(2)

    val pieRenderer: PieRenderer?
        get() {
            for (renderer in mClients!!) {
                if (renderer is PieRenderer) {
                    return renderer
                }
            }
            return null
        }

    val clientSize: Int
        get() = mClients!!.size

    val windowPositionX: Int
        get() = mPosition[0]

    val windowPositionY: Int
        get() = mPosition[1]

    interface Renderer {

        fun handlesTouch(): Boolean

        fun onTouchEvent(evt: MotionEvent): Boolean

        fun setOverlay(overlay: RenderOverlay?)

        fun layout(left: Int, top: Int, right: Int, bottom: Int)

        fun draw(canvas: Canvas)

    }

    init {
        mRenderView = RenderView(context)
        addView(mRenderView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT))
        mClients = ArrayList(10)
        mTouchClients = ArrayList(10)
        setWillNotDraw(false)

        addRenderer(PieRenderer(context))
    }

    fun addRenderer(renderer: Renderer) {
        mClients!!.add(renderer)
        renderer.setOverlay(this)
        if (renderer.handlesTouch()) {
            mTouchClients!!.add(0, renderer)
        }
        renderer.layout(left, top, right, bottom)
    }

    fun addRenderer(pos: Int, renderer: Renderer) {
        mClients!!.add(pos, renderer)
        renderer.setOverlay(this)
        renderer.layout(left, top, right, bottom)
    }

    fun remove(renderer: Renderer) {
        mClients!!.remove(renderer)
        renderer.setOverlay(null)
    }

    override fun dispatchTouchEvent(m: MotionEvent): Boolean {
        return false
    }

    fun directDispatchTouch(m: MotionEvent, target: Renderer): Boolean {
        mRenderView.setTouchTarget(target)
        val res = super.dispatchTouchEvent(m)
        mRenderView.setTouchTarget(null)
        return res
    }

    private fun adjustPosition() {
        getLocationInWindow(mPosition)
    }

    fun update() {
        mRenderView.invalidate()
    }

    private inner class RenderView(context: Context) : View(context) {

        private var mTouchTarget: Renderer? = null

        init {
            setWillNotDraw(false)
        }

        fun setTouchTarget(target: Renderer?) {
            mTouchTarget = target
        }

        override fun onTouchEvent(evt: MotionEvent): Boolean {
            if (mTouchTarget != null) {
                return mTouchTarget!!.onTouchEvent(evt)
            }
            if (mTouchClients != null) {
                var res = false
                for (client in mTouchClients) {
                    res = res or client.onTouchEvent(evt)
                }
                return res
            }
            return false
        }

        public override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            adjustPosition()
            super.onLayout(changed, left, top, right, bottom)
            if (mClients == null) {
                return
            }
            for (renderer in mClients) {
                renderer.layout(left, top, right, bottom)
            }
        }

        override fun draw(canvas: Canvas) {
            super.draw(canvas)
            if (mClients == null) {
                return
            }
            var redraw = false
            for (renderer in mClients) {
                renderer.draw(canvas)
                redraw = redraw || (renderer as OverlayRenderer).isVisible
            }
            if (redraw) {
                invalidate()
            }
        }
    }

}