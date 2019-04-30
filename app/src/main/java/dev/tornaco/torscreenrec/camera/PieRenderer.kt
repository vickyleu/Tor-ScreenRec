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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.os.Handler
import android.os.Message
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.LinearInterpolator
import android.view.animation.Transformation

import java.util.ArrayList

import dev.tornaco.torscreenrec.R

class PieRenderer(context: Context) : OverlayRenderer(), FocusIndicator {
    // Sometimes continuous autofocus starts and stops several times quickly.
    // These states are used to make sure the animation is run for at least some
    // time.
    @Volatile
    private var mState: Int = 0
    private val mAnimation = ScaleAnimation()
    private val mDisappear = Disappear()
    private val mEndAction = EndAction()
    // geometry
    private var mCenter: Point? = null
    private var mRadius: Int = 0
    private var mRadiusInc: Int = 0

    // the detection if touch is inside a slice is offset
    // inbounds by this amount to allow the selection to show before the
    // finger covers it
    private var mTouchOffset: Int = 0

    private var mItems: MutableList<PieItem>? = null

    private var mOpenItem: PieItem? = null

    private var mSelectedPaint: Paint? = null
    private var mSubPaint: Paint? = null

    // touch handling
    private var mCurrentItem: PieItem? = null

    private var mFocusPaint: Paint? = null
    private var mSuccessColor: Int = 0
    private var mFailColor: Int = 0
    private var mCircleSize: Int = 0
    private var mFocusX: Int = 0
    private var mFocusY: Int = 0
    private var mCenterX: Int = 0
    private var mCenterY: Int = 0

    private var mDialAngle: Int = 0
    private var mCircle: RectF? = null
    private var mDial: RectF? = null
    private var mPoint1: Point? = null
    private var mPoint2: Point? = null
    private var mStartAnimationAngle: Int = 0
    private var mFocused: Boolean = false
    private var mInnerOffset: Int = 0
    private var mOuterStroke: Int = 0
    private var mInnerStroke: Int = 0
    private var mTapMode: Boolean = false
    private var mBlockFocus: Boolean = false
    private var mTouchSlopSquared: Int = 0
    private var mDown: Point? = null
    private var mOpening: Boolean = false
    private var mXFade: LinearAnimation? = null
    private var mFadeIn: LinearAnimation? = null
    @Volatile
    private var mFocusCancelled: Boolean = false
    private var mListener: PieListener? = null
    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_OPEN -> if (mListener != null) {
                    mListener!!.onPieOpened(mCenter!!.x, mCenter!!.y)
                }
                MSG_CLOSE -> if (mListener != null) {
                    mListener!!.onPieClosed()
                }
            }
        }
    }

    val size: Int
        get() = 2 * mCircleSize

    private val randomRange: Int
        get() = (-60 + 120 * Math.random()).toInt()

    init {
        init(context)
    }

    fun setPieListener(pl: PieListener) {
        mListener = pl
    }

    private fun init(ctx: Context) {
        isVisible = false
        mItems = ArrayList()
        val res = ctx.resources
        mRadius = res.getDimensionPixelSize(R.dimen.pie_radius_start)
        mCircleSize = mRadius - res.getDimensionPixelSize(R.dimen.focus_radius_offset)
        mRadiusInc = res.getDimensionPixelSize(R.dimen.pie_radius_increment)
        mTouchOffset = res.getDimensionPixelSize(R.dimen.pie_touch_offset)
        mCenter = Point(0, 0)
        mSelectedPaint = Paint()
        mSelectedPaint!!.color = Color.argb(255, 51, 181, 229)
        mSelectedPaint!!.isAntiAlias = true
        mSubPaint = Paint()
        mSubPaint!!.isAntiAlias = true
        mSubPaint!!.color = Color.argb(200, 250, 230, 128)
        mFocusPaint = Paint()
        mFocusPaint!!.isAntiAlias = true
        mFocusPaint!!.color = Color.WHITE
        mFocusPaint!!.style = Paint.Style.STROKE
        mSuccessColor = Color.GREEN
        mFailColor = Color.RED
        mCircle = RectF()
        mDial = RectF()
        mPoint1 = Point()
        mPoint2 = Point()
        mInnerOffset = res.getDimensionPixelSize(R.dimen.focus_inner_offset)
        mOuterStroke = res.getDimensionPixelSize(R.dimen.focus_outer_stroke)
        mInnerStroke = res.getDimensionPixelSize(R.dimen.focus_inner_stroke)
        mState = STATE_IDLE
        mBlockFocus = false
        mTouchSlopSquared = ViewConfiguration.get(ctx).scaledTouchSlop
        mTouchSlopSquared = mTouchSlopSquared * mTouchSlopSquared
        mDown = Point()
    }

    fun showsItems(): Boolean {
        return mTapMode
    }

    fun addItem(item: PieItem) {
        // add the item to the pie itself
        mItems!!.add(item)
    }

    fun removeItem(item: PieItem) {
        mItems!!.remove(item)
    }

    fun clearItems() {
        mItems!!.clear()
    }

    fun showInCenter() {
        if (mState == STATE_PIE && isVisible) {
            mTapMode = false
            show(false)
        } else {
            if (mState != STATE_IDLE) {
                cancelFocus()
            }
            mState = STATE_PIE
            setCenter(mCenterX, mCenterY)
            mTapMode = true
            show(true)
        }
    }

    fun hide() {
        show(false)
    }

    /**
     * guaranteed has center set
     *
     * @param show
     */
    private fun show(show: Boolean) {
        if (show) {
            mState = STATE_PIE
            // ensure clean state
            mCurrentItem = null
            mOpenItem = null
            for (item in mItems!!) {
                item.isSelected = false
            }
            layoutPie()
            fadeIn()
        } else {
            mState = STATE_IDLE
            mTapMode = false
            if (mXFade != null) {
                mXFade!!.cancel()
            }
        }
        isVisible = show
        mHandler.sendEmptyMessage(if (show) MSG_OPEN else MSG_CLOSE)
    }

    private fun fadeIn() {
        mFadeIn = LinearAnimation(0f, 1f)
        mFadeIn!!.duration = PIE_FADE_IN_DURATION
        mFadeIn!!.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                mFadeIn = null
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        mFadeIn!!.startNow()
        mOverlay!!.startAnimation(mFadeIn)
    }

    fun setCenter(x: Int, y: Int) {
        mCenter!!.x = x
        mCenter!!.y = y
        // when using the pie menu, align the focus ring
        alignFocus(x, y)
    }

    private fun layoutPie() {
        val rgap = 2
        val inner = mRadius + rgap
        val outer = mRadius + mRadiusInc - rgap
        val gap = 1
        layoutItems(mItems!!, (Math.PI / 2).toFloat(), inner, outer, gap)
    }

    private fun layoutItems(items: List<PieItem>, centerAngle: Float, inner: Int,
                            outer: Int, gap: Int) {
        val emptyangle = PIE_SWEEP / 16
        var sweep = (PIE_SWEEP - 2 * emptyangle) / items.size
        var angle = centerAngle - PIE_SWEEP / 2 + emptyangle + sweep / 2
        // check if we have custom geometry
        // first item we find triggers custom sweep for all
        // this allows us to re-use the path
        for (item in items) {
            if (item.center >= 0) {
                sweep = item.sweep
                break
            }
        }
        val path = makeSlice(getDegrees(0.0) - gap, getDegrees(sweep.toDouble()) + gap,
                outer, inner, mCenter!!)
        for (item in items) {
            // shared between items
            item.path = path
            if (item.center >= 0) {
                angle = item.center
            }
            val w = item.intrinsicWidth
            val h = item.intrinsicHeight
            // move views to outer border
            val r = inner + (outer - inner) * 2 / 3
            var x = (r * Math.cos(angle.toDouble())).toInt()
            val y = mCenter!!.y - (r * Math.sin(angle.toDouble())).toInt() - h / 2
            x = mCenter!!.x + x - w / 2
            item.setBounds(x, y, x + w, y + h)
            val itemstart = angle - sweep / 2
            item.setGeometry(itemstart, sweep, inner, outer)
            if (item.hasItems()) {
                layoutItems(item.items!!, angle, inner,
                        outer + mRadiusInc / 2, gap)
            }
            angle += sweep
        }
    }

    private fun makeSlice(start: Float, end: Float, outer: Int, inner: Int, center: Point): Path {
        val bb = RectF((center.x - outer).toFloat(), (center.y - outer).toFloat(), (center.x + outer).toFloat(),
                (center.y + outer).toFloat())
        val bbi = RectF((center.x - inner).toFloat(), (center.y - inner).toFloat(), (center.x + inner).toFloat(),
                (center.y + inner).toFloat())
        val path = Path()
        path.arcTo(bb, start, end - start, true)
        path.arcTo(bbi, end, start - end)
        path.close()
        return path
    }

    /**
     * converts a
     *
     * @param angle from 0..PI to Android degrees (clockwise starting at 3 o'clock)
     * @return skia angle
     */
    private fun getDegrees(angle: Double): Float {
        return (360 - 180 * angle / Math.PI).toFloat()
    }

    private fun startFadeOut() {
        mOverlay!!.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                deselect()
                show(false)
                mOverlay!!.alpha = 1f
                super.onAnimationEnd(animation)
            }
        }).duration = PIE_SELECT_FADE_DURATION
    }

    override fun onDraw(canvas: Canvas) {
        var alpha = 1f
        if (mXFade != null) {
            alpha = mXFade!!.value
        } else if (mFadeIn != null) {
            alpha = mFadeIn!!.value
        }
        val state = canvas.save()
        if (mFadeIn != null) {
            val sf = 0.9f + alpha * 0.1f
            canvas.scale(sf, sf, mCenter!!.x.toFloat(), mCenter!!.y.toFloat())
        }
        drawFocus(canvas)
        if (mState == STATE_FINISHING) {
            canvas.restoreToCount(state)
            return
        }
        if (mOpenItem == null || mXFade != null) {
            // draw base menu
            for (item in mItems!!) {
                drawItem(canvas, item, alpha)
            }
        }
        if (mOpenItem != null) {
            for (inner in mOpenItem!!.items!!) {
                drawItem(canvas, inner, if (mXFade != null) 1 - 0.5f * alpha else 1f)
            }
        }
        canvas.restoreToCount(state)
    }

    private fun drawItem(canvas: Canvas, item: PieItem, alpha: Float) {
        var alpha = alpha
        if (mState == STATE_PIE) {
            if (item.path != null) {
                if (item.isSelected) {
                    val p = mSelectedPaint
                    val state = canvas.save()
                    val r = getDegrees(item.startAngle.toDouble())
                    canvas.rotate(r, mCenter!!.x.toFloat(), mCenter!!.y.toFloat())
                    canvas.drawPath(item.path!!, p!!)
                    canvas.restoreToCount(state)
                }
                alpha *= (if (item.isEnabled) 1f else 0.3f)
                // draw the item view
                item.setAlpha(alpha)
                item.draw(canvas)
            }
        }
    }

    override fun onTouchEvent(evt: MotionEvent): Boolean {
        val x = evt.x
        val y = evt.y
        val action = evt.actionMasked
        val polar = getPolar(x, y, !mTapMode)
        if (MotionEvent.ACTION_DOWN == action) {
            mDown!!.x = evt.x.toInt()
            mDown!!.y = evt.y.toInt()
            mOpening = false
            if (mTapMode) {
                val item = findItem(polar)
                if (item != null && mCurrentItem !== item) {
                    mState = STATE_PIE
                    onEnter(item)
                }
            } else {
                setCenter(x.toInt(), y.toInt())
                show(true)
            }
            return true
        } else if (MotionEvent.ACTION_UP == action) {
            if (isVisible) {
                var item = mCurrentItem
                if (mTapMode) {
                    item = findItem(polar)
                    if (item != null && mOpening) {
                        mOpening = false
                        return true
                    }
                }
                if (item == null) {
                    mTapMode = false
                    show(false)
                } else if (!mOpening && !item.hasItems()) {
                    item.performClick()
                    startFadeOut()
                    mTapMode = false
                }
                return true
            }
        } else if (MotionEvent.ACTION_CANCEL == action) {
            if (isVisible || mTapMode) {
                show(false)
            }
            deselect()
            return false
        } else if (MotionEvent.ACTION_MOVE == action) {
            if (polar.y < mRadius) {
                if (mOpenItem != null) {
                    mOpenItem = null
                } else {
                    deselect()
                }
                return false
            }
            val item = findItem(polar)
            val moved = hasMoved(evt)
            if (item != null && mCurrentItem !== item && (!mOpening || moved)) {
                // only select if we didn'data just open or have moved past slop
                mOpening = false
                if (moved) {
                    // switch back to swipe mode
                    mTapMode = false
                }
                onEnter(item)
            }
        }
        return false
    }

    private fun hasMoved(e: MotionEvent): Boolean {
        return mTouchSlopSquared < (e.x - mDown!!.x) * (e.x - mDown!!.x) + (e.y - mDown!!.y) * (e.y - mDown!!.y)
    }

    /**
     * enter a slice for a view
     * updates model only
     *
     * @param item
     */
    private fun onEnter(item: PieItem?) {
        if (mCurrentItem != null) {
            mCurrentItem!!.isSelected = false
        }
        if (item != null && item.isEnabled) {
            item.isSelected = true
            mCurrentItem = item
            if (mCurrentItem !== mOpenItem && mCurrentItem!!.hasItems()) {
                openCurrentItem()
            }
        } else {
            mCurrentItem = null
        }
    }

    private fun deselect() {
        if (mCurrentItem != null) {
            mCurrentItem!!.isSelected = false
        }
        if (mOpenItem != null) {
            mOpenItem = null
        }
        mCurrentItem = null
    }

    private fun openCurrentItem() {
        if (mCurrentItem != null && mCurrentItem!!.hasItems()) {
            mCurrentItem!!.isSelected = false
            mOpenItem = mCurrentItem
            mOpening = true
            mXFade = LinearAnimation(1f, 0f)
            mXFade!!.duration = PIE_XFADE_DURATION
            mXFade!!.setAnimationListener(object : AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    mXFade = null
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })
            mXFade!!.startNow()
            mOverlay!!.startAnimation(mXFade)
        }
    }

    private fun getPolar(x: Float, y: Float, useOffset: Boolean): PointF {
        var x = x
        var y = y
        val res = PointF()
        // get angle and radius from x/y
        res.x = Math.PI.toFloat() / 2
        x = x - mCenter!!.x
        y = mCenter!!.y - y
        res.y = Math.sqrt((x * x + y * y).toDouble()).toFloat()
        if (x != 0f) {
            res.x = Math.atan2(y.toDouble(), x.toDouble()).toFloat()
            if (res.x < 0) {
                res.x = (2 * Math.PI + res.x).toFloat()
            }
        }
        res.y = res.y + if (useOffset) mTouchOffset else 0
        return res
    }

    /**
     * @param polar x: angle, y: dist
     * @return the item at angle/dist or null
     */
    private fun findItem(polar: PointF): PieItem? {
        // find the matching item:
        val items = if (mOpenItem != null) mOpenItem!!.items else mItems
        for (item in items!!) {
            if (inside(polar, item)) {
                return item
            }
        }
        return null
    }

    private fun inside(polar: PointF, item: PieItem): Boolean {
        return (item.innerRadius < polar.y
                && item.startAngle < polar.x
                && item.startAngle + item.sweep > polar.x
                && (!mTapMode || item.outerRadius > polar.y))
    }

    override fun handlesTouch(): Boolean {
        return true
    }

    // focus specific code

    fun setBlockFocus(blocked: Boolean) {
        mBlockFocus = blocked
        if (blocked) {
            clear()
        }
    }

    fun setFocus(x: Int, y: Int) {
        mFocusX = x
        mFocusY = y
        setCircle(mFocusX, mFocusY)
    }

    fun alignFocus(x: Int, y: Int) {
        mOverlay!!.removeCallbacks(mDisappear)
        mAnimation.cancel()
        mAnimation.reset()
        mFocusX = x
        mFocusY = y
        mDialAngle = DIAL_HORIZONTAL
        setCircle(x, y)
        mFocused = false
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        super.layout(l, t, r, b)
        mCenterX = (r - l) / 2
        mCenterY = (b - t) / 2
        mFocusX = mCenterX
        mFocusY = mCenterY
        setCircle(mFocusX, mFocusY)
        if (isVisible && mState == STATE_PIE) {
            setCenter(mCenterX, mCenterY)
            layoutPie()
        }
    }

    private fun setCircle(cx: Int, cy: Int) {
        mCircle!!.set((cx - mCircleSize).toFloat(), (cy - mCircleSize).toFloat(),
                (cx + mCircleSize).toFloat(), (cy + mCircleSize).toFloat())
        mDial!!.set((cx - mCircleSize + mInnerOffset).toFloat(), (cy - mCircleSize + mInnerOffset).toFloat(),
                (cx + mCircleSize - mInnerOffset).toFloat(), (cy + mCircleSize - mInnerOffset).toFloat())
    }

    fun drawFocus(canvas: Canvas) {
        if (mBlockFocus) {
            return
        }
        mFocusPaint!!.strokeWidth = mOuterStroke.toFloat()
        canvas.drawCircle(mFocusX.toFloat(), mFocusY.toFloat(), mCircleSize.toFloat(), mFocusPaint!!)
        if (mState == STATE_PIE) {
            return
        }
        val color = mFocusPaint!!.color
        if (mState == STATE_FINISHING) {
            mFocusPaint!!.color = if (mFocused) mSuccessColor else mFailColor
        }
        mFocusPaint!!.strokeWidth = mInnerStroke.toFloat()
        drawLine(canvas, mDialAngle, mFocusPaint!!)
        drawLine(canvas, mDialAngle + 45, mFocusPaint!!)
        drawLine(canvas, mDialAngle + 180, mFocusPaint!!)
        drawLine(canvas, mDialAngle + 225, mFocusPaint!!)
        canvas.save()
        // rotate the arc instead of its offset to better use framework's shape caching
        canvas.rotate(mDialAngle.toFloat(), mFocusX.toFloat(), mFocusY.toFloat())
        canvas.drawArc(mDial!!, 0f, 45f, false, mFocusPaint!!)
        canvas.drawArc(mDial!!, 180f, 45f, false, mFocusPaint!!)
        canvas.restore()
        mFocusPaint!!.color = color
    }

    private fun drawLine(canvas: Canvas, angle: Int, p: Paint) {
        convertCart(angle, mCircleSize - mInnerOffset, mPoint1!!)
        convertCart(angle, mCircleSize - mInnerOffset + mInnerOffset / 3, mPoint2!!)
        canvas.drawLine((mPoint1!!.x + mFocusX).toFloat(), (mPoint1!!.y + mFocusY).toFloat(),
                (mPoint2!!.x + mFocusX).toFloat(), (mPoint2!!.y + mFocusY).toFloat(), p)
    }

    override fun showStart() {
        if (mState == STATE_PIE) {
            return
        }
        cancelFocus()
        mStartAnimationAngle = 67
        val range = randomRange
        startAnimation(SCALING_UP_TIME.toLong(),
                false, mStartAnimationAngle.toFloat(), (mStartAnimationAngle + range).toFloat())
        mState = STATE_FOCUSING
    }

    override fun showSuccess(timeout: Boolean) {
        if (mState == STATE_FOCUSING) {
            startAnimation(SCALING_DOWN_TIME.toLong(),
                    timeout, mStartAnimationAngle.toFloat())
            mState = STATE_FINISHING
            mFocused = true
        }
    }

    override fun showFail(timeout: Boolean) {
        if (mState == STATE_FOCUSING) {
            startAnimation(SCALING_DOWN_TIME.toLong(),
                    timeout, mStartAnimationAngle.toFloat())
            mState = STATE_FINISHING
            mFocused = false
        }
    }

    private fun cancelFocus() {
        mFocusCancelled = true
        mOverlay!!.removeCallbacks(mDisappear)
        mAnimation?.cancel()
        mFocusCancelled = false
        mFocused = false
        mState = STATE_IDLE
    }

    override fun clear() {
        if (mState == STATE_PIE) {
            return
        }
        cancelFocus()
        mOverlay!!.post(mDisappear)
    }

    private fun startAnimation(duration: Long, timeout: Boolean,
                               toScale: Float) {
        startAnimation(duration, timeout, mDialAngle.toFloat(),
                toScale)
    }

    private fun startAnimation(duration: Long, timeout: Boolean,
                               fromScale: Float, toScale: Float) {
        isVisible = true
        mAnimation.reset()
        mAnimation.duration = duration
        mAnimation.setScale(fromScale, toScale)
        mAnimation.setAnimationListener(if (timeout) mEndAction else null)
        mOverlay!!.startAnimation(mAnimation)
        update()
    }

    interface PieListener {
        fun onPieOpened(centerX: Int, centerY: Int)

        fun onPieClosed()
    }

    private inner class EndAction : AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            // Keep the focus indicator for some time.
            if (!mFocusCancelled) {
                mOverlay!!.postDelayed(mDisappear, DISAPPEAR_TIMEOUT.toLong())
            }
        }

        override fun onAnimationRepeat(animation: Animation) {}

        override fun onAnimationStart(animation: Animation) {}
    }

    private inner class Disappear : Runnable {
        override fun run() {
            if (mState == STATE_PIE) {
                return
            }
            isVisible = false
            mFocusX = mCenterX
            mFocusY = mCenterY
            mState = STATE_IDLE
            setCircle(mFocusX, mFocusY)
            mFocused = false
        }
    }

    private inner class ScaleAnimation : Animation() {
        private var mFrom = 1f
        private var mTo = 1f

        init {
            fillAfter = true
        }

        fun setScale(from: Float, to: Float) {
            mFrom = from
            mTo = to
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            mDialAngle = (mFrom + (mTo - mFrom) * interpolatedTime).toInt()
        }
    }


    private inner class LinearAnimation(private val mFrom: Float, private val mTo: Float) : Animation() {
        var value: Float = 0.toFloat()
            private set

        init {
            fillAfter = true
            interpolator = LinearInterpolator()
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            value = mFrom + (mTo - mFrom) * interpolatedTime
        }
    }

    companion object {
        private val STATE_IDLE = 0
        private val STATE_FOCUSING = 1
        private val STATE_FINISHING = 2
        private val STATE_PIE = 8
        private val SCALING_UP_TIME = 600
        private val SCALING_DOWN_TIME = 100
        private val DISAPPEAR_TIMEOUT = 200
        private val DIAL_HORIZONTAL = 157
        private val PIE_FADE_IN_DURATION: Long = 200
        private val PIE_XFADE_DURATION: Long = 200
        private val PIE_SELECT_FADE_DURATION: Long = 300
        private val MSG_OPEN = 0
        private val MSG_CLOSE = 1
        private val PIE_SWEEP = (Math.PI * 2 / 3).toFloat()

        private fun convertCart(angle: Int, radius: Int, out: Point) {
            val a = 2.0 * Math.PI * (angle % 360).toDouble() / 360
            out.x = (radius * Math.cos(a) + 0.5).toInt()
            out.y = (radius * Math.sin(a) + 0.5).toInt()
        }
    }

}