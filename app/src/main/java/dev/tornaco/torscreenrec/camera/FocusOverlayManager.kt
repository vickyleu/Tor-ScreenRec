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

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.hardware.Camera
import android.hardware.Camera.Area
import android.hardware.Camera.Parameters
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

import java.util.ArrayList


/* A class that handles everything about focus in still picture mode.
 * This also handles the metering area because it is the same as focus area.
 *
 * The test cases:
 * (1) The camera has continuous autofocus. Move the camera. Take a picture when
 *     CAF is not in progress.
 * (2) The camera has continuous autofocus. Move the camera. Take a picture when
 *     CAF is in progress.
 * (3) The camera has face detection. Point the camera at some faces. Hold the
 *     shutter. Release to take a picture.
 * (4) The camera has face detection. Point the camera at some faces. Single tap
 *     the shutter to take a picture.
 * (5) The camera has autofocus. Single tap the shutter to take a picture.
 * (6) The camera has autofocus. Hold the shutter. Release to take a picture.
 * (7) The camera has no autofocus. Single tap the shutter and take a picture.
 * (8) The camera has autofocus and supports focus area. Touch the screen to
 *     trigger autofocus. Take a picture.
 * (9) The camera has autofocus and supports focus area. Touch the screen to
 *     trigger autofocus. Wait until it times out.
 * (10) The camera has no autofocus and supports metering area. Touch the screen
 *     to change metering area.
 */
class FocusOverlayManager(internal var mListener: Listener, looper: Looper) {
    /* package */ internal var focusState = STATE_IDLE
        private set
    private var mInitialized: Boolean = false
    private var mFocusAreaSupported: Boolean = false
    private var mMeteringAreaSupported: Boolean = false
    private var mLockAeAwbNeeded: Boolean = false
    var aeAwbLock: Boolean = false
    private val mMatrix: Matrix?
    private var mPieRenderer: PieRenderer? = null
    private var mPreviewWidth: Int = 0 // The width of the preview frame layout.
    private var mPreviewHeight: Int = 0 // The height of the preview frame layout.
    private var mMirror: Boolean = false // true if the camera is front-facing.
    private var mDisplayOrientation: Int = 0
    private var mFocusArea: MutableList<Area>? = null // focus area in driver format
    private var mMeteringArea: MutableList<Area>? = null // metering area in driver format
    private var mFocusMode: String? = null
    private var mOverrideFocusMode: String? = null
    private var mParameters: Parameters? = null
    private val mHandler: Handler

    // Always use autofocus in tap-to-focus.
    // For some reasons, the driver does not support the current
    // focus mode. Fall back to auto.
    val focusMode: String?
        get() {
            if (mOverrideFocusMode != null) {
                return mOverrideFocusMode
            }
            val supportedFocusModes = mParameters!!.supportedFocusModes

            if (mFocusAreaSupported && mFocusArea != null) {
                mFocusMode = Parameters.FOCUS_MODE_AUTO
            } else {
                mFocusMode = Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            }

            if (!isSupported(mFocusMode?:"", supportedFocusModes)) {
                if (isSupported(Parameters.FOCUS_MODE_AUTO,
                                mParameters!!.supportedFocusModes)) {
                    mFocusMode = Parameters.FOCUS_MODE_AUTO
                } else {
                    mFocusMode = mParameters!!.focusMode
                }
            }
            return mFocusMode
        }

    val focusAreas: List<Area>?
        get() = mFocusArea

    val meteringAreas: List<Area>?
        get() = mMeteringArea

    val isFocusCompleted: Boolean
        get() = focusState == STATE_SUCCESS || focusState == STATE_FAIL

    val isFocusingSnapOnFinish: Boolean
        get() = focusState == STATE_FOCUSING_SNAP_ON_FINISH

    init {
        mHandler = MainHandler(looper)
        mMatrix = Matrix()
    }

    fun setFocusRenderer(renderer: PieRenderer?) {
        mPieRenderer = renderer
        mInitialized = mMatrix != null
    }

    fun setParameters(parameters: Parameters?) {
        // parameters can only be null when onConfigurationChanged is called
        // before camera is open. We will just return in this case, because
        // parameters will be set again later with the right parameters after
        // camera is open.
        if (parameters == null) {
            return
        }
        mParameters = parameters
        mFocusAreaSupported = isFocusAreaSupported(parameters)
        mMeteringAreaSupported = isMeteringAreaSupported(parameters)
        mLockAeAwbNeeded = isAutoExposureLockSupported(mParameters!!) || isAutoWhiteBalanceLockSupported(mParameters!!)
    }

    fun setPreviewSize(previewWidth: Int, previewHeight: Int) {
        if (mPreviewWidth != previewWidth || mPreviewHeight != previewHeight) {
            mPreviewWidth = previewWidth
            mPreviewHeight = previewHeight
            setMatrix()
        }
    }

    fun setMirror(mirror: Boolean) {
        mMirror = mirror
        setMatrix()
    }

    fun setDisplayOrientation(displayOrientation: Int) {
        mDisplayOrientation = displayOrientation
        setMatrix()
    }

    private fun setMatrix() {
        if (mPreviewWidth != 0 && mPreviewHeight != 0) {
            val matrix = Matrix()
            prepareMatrix(matrix, mMirror, mDisplayOrientation,
                    mPreviewWidth, mPreviewHeight)
            // In face detection, the matrix converts the driver coordinates to UI
            // coordinates. In tap focus, the inverted matrix converts the UI
            // coordinates to driver coordinates.
            matrix.invert(mMatrix)
            mInitialized = mPieRenderer != null
        }
    }

    private fun lockAeAwbIfNeeded() {
        if (mLockAeAwbNeeded && !aeAwbLock) {
            aeAwbLock = true
            mListener.setFocusParameters()
        }
    }

    private fun unlockAeAwbIfNeeded() {
        if (mLockAeAwbNeeded && aeAwbLock && focusState != STATE_FOCUSING_SNAP_ON_FINISH) {
            aeAwbLock = false
            mListener.setFocusParameters()
        }
    }

    fun onShutterDown() {
        if (!mInitialized) {
            return
        }

        var autoFocusCalled = false
        if (needAutoFocusCall()) {
            // Do not focus if touch focus has been triggered.
            if (focusState != STATE_SUCCESS && focusState != STATE_FAIL) {
                autoFocus()
                autoFocusCalled = true
            }
        }

        if (!autoFocusCalled) {
            lockAeAwbIfNeeded()
        }
    }

    fun onShutterUp() {
        if (!mInitialized) {
            return
        }

        if (needAutoFocusCall()) {
            // User releases half-pressed focus key.
            if (focusState == STATE_FOCUSING || focusState == STATE_SUCCESS
                    || focusState == STATE_FAIL) {
                cancelAutoFocus()
            }
        }

        // Unlock AE and AWB after cancelAutoFocus. Camera API does not
        // guarantee setParameters can be called during autofocus.
        unlockAeAwbIfNeeded()
    }

    fun doSnap() {
        if (!mInitialized) {
            return
        }

        // If the user has half-pressed the shutter and focus is completed, we
        // can take the photo right away. If the focus mode is infinity, we can
        // also take the photo.
        if (!needAutoFocusCall() || focusState == STATE_SUCCESS || focusState == STATE_FAIL) {
            capture()
        } else if (focusState == STATE_FOCUSING) {
            // Half pressing the shutter (i.e. the focus button event) will
            // already have requested AF for us, so just request capture on
            // focus here.
            focusState = STATE_FOCUSING_SNAP_ON_FINISH
        } else if (focusState == STATE_IDLE) {
            // We didn'data do focus. This can happen if the user press focus key
            // while the snapshot is still in progress. The user probably wants
            // the next snapshot as soon as possible, so we just do a snapshot
            // without focusing again.
            capture()
        }
    }

    fun onAutoFocus(focused: Boolean, shutterButtonPressed: Boolean) {
        if (focusState == STATE_FOCUSING_SNAP_ON_FINISH) {
            // Take the picture no matter focus succeeds or fails. No need
            // to play the AF sound if we're about to play the shutter
            // sound.
            if (focused) {
                focusState = STATE_SUCCESS
            } else {
                focusState = STATE_FAIL
            }
            updateFocusUI()
            capture()
        } else if (focusState == STATE_FOCUSING) {
            // This happens when (1) user is half-pressing the focus key or
            // (2) touch focus is triggered. Play the focus tone. Do not
            // take the picture now.
            if (focused) {
                focusState = STATE_SUCCESS
            } else {
                focusState = STATE_FAIL
            }
            updateFocusUI()
            // If this is triggered by touch focus, cancel focus after a
            // while.
            if (mFocusArea != null) {
                mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY.toLong())
            }
            if (shutterButtonPressed) {
                // Lock AE & AWB so users can half-press shutter and recompose.
                lockAeAwbIfNeeded()
            }
        } else if (focusState == STATE_IDLE) {
            // User has released the focus key before focus completes.
            // Do nothing.
        }
    }

    fun onAutoFocusMoving(moving: Boolean) {
        if (!mInitialized) {
            return
        }

        // Ignore if we have requested autofocus. This method only handles
        // continuous autofocus.
        if (focusState != STATE_IDLE) {
            return
        }

        if (moving) {
            mPieRenderer!!.showStart()
        } else {
            mPieRenderer!!.showSuccess(true)
        }
    }

    private fun initializeFocusAreas(focusWidth: Int, focusHeight: Int,
                                     x: Int, y: Int, previewWidth: Int, previewHeight: Int) {
        if (mFocusArea == null) {
            mFocusArea = ArrayList()
            mFocusArea!!.add(Area(Rect(), 1))
        }

        // Convert the coordinates to driver format.
        calculateTapArea(focusWidth, focusHeight, 1f, x, y, previewWidth, previewHeight,
                (mFocusArea!![0] as Area).rect)
    }

    private fun initializeMeteringAreas(focusWidth: Int, focusHeight: Int,
                                        x: Int, y: Int, previewWidth: Int, previewHeight: Int) {
        if (mMeteringArea == null) {
            mMeteringArea = ArrayList()
            mMeteringArea!!.add(Area(Rect(), 1))
        }

        // Convert the coordinates to driver format.
        // AE area is bigger because exposure is sensitive and
        // easy to over- or underexposure if area is too small.
        calculateTapArea(focusWidth, focusHeight, 1.5f, x, y, previewWidth, previewHeight,
                (mMeteringArea!![0] as Area).rect)
    }

    fun onSingleTapUp(x: Int, y: Int) {
        if (!mInitialized || focusState == STATE_FOCUSING_SNAP_ON_FINISH) {
            return
        }

        // Let users be able to cancel previous touch focus.
        if (mFocusArea != null && (focusState == STATE_FOCUSING ||
                        focusState == STATE_SUCCESS || focusState == STATE_FAIL)) {
            cancelAutoFocus()
        }
        // Initialize variables.
        val focusWidth = mPieRenderer!!.size
        val focusHeight = mPieRenderer!!.size
        if (focusWidth == 0 || mPieRenderer!!.width == 0 || mPieRenderer!!.height == 0) {
            return
        }
        val previewWidth = mPreviewWidth
        val previewHeight = mPreviewHeight
        // Initialize mFocusArea.
        if (mFocusAreaSupported) {
            initializeFocusAreas(focusWidth, focusHeight, x, y, previewWidth, previewHeight)
        }
        // Initialize mMeteringArea.
        if (mMeteringAreaSupported) {
            initializeMeteringAreas(focusWidth, focusHeight, x, y, previewWidth, previewHeight)
        }

        // Use margin to set the focus indicator to the touched area.
        mPieRenderer!!.setFocus(x, y)

        // Set the focus area and metering area.
        mListener.setFocusParameters()
        if (mFocusAreaSupported) {
            autoFocus()
        } else {  // Just show the indicator in all other cases.
            updateFocusUI()
            // Reset the metering area in 3 seconds.
            mHandler.removeMessages(RESET_TOUCH_FOCUS)
            mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS, RESET_TOUCH_FOCUS_DELAY.toLong())
        }
    }

    fun onPreviewStarted() {
        focusState = STATE_IDLE
    }

    fun onPreviewStopped() {
        // If auto focus was in progress, it would have been stopped.
        focusState = STATE_IDLE
        resetTouchFocus()
        updateFocusUI()
    }

    fun onCameraReleased() {
        onPreviewStopped()
    }

    private fun autoFocus() {
        Log.v(TAG, "Start autofocus.")
        mListener.autoFocus()
        focusState = STATE_FOCUSING
        updateFocusUI()
        mHandler.removeMessages(RESET_TOUCH_FOCUS)
    }

    private fun cancelAutoFocus() {
        Log.v(TAG, "Cancel autofocus.")

        // Reset the tap area before calling mListener.cancelAutofocus.
        // Otherwise, focus mode stays at auto and the tap area passed to the
        // driver is not reset.
        resetTouchFocus()
        mListener.cancelAutoFocus()
        focusState = STATE_IDLE
        updateFocusUI()
        mHandler.removeMessages(RESET_TOUCH_FOCUS)
    }

    private fun capture() {
        if (mListener.capture()) {
            focusState = STATE_IDLE
            mHandler.removeMessages(RESET_TOUCH_FOCUS)
        }
    }

    fun updateFocusUI() {
        if (!mInitialized) {
            return
        }
        val focusIndicator = mPieRenderer

        if (focusState == STATE_IDLE) {
            if (mFocusArea == null) {
                focusIndicator!!.clear()
            } else {
                // Users touch on the preview and the indicator represents the
                // metering area. Either focus area is not supported or
                // autoFocus call is not required.
                focusIndicator!!.showStart()
            }
        } else if (focusState == STATE_FOCUSING || focusState == STATE_FOCUSING_SNAP_ON_FINISH) {
            focusIndicator!!.showStart()
        } else {
            if (Parameters.FOCUS_MODE_CONTINUOUS_PICTURE == mFocusMode) {
                // TODO: check HAL behavior and decide if this can be removed.
                focusIndicator!!.showSuccess(false)
            } else if (focusState == STATE_SUCCESS) {
                focusIndicator!!.showSuccess(false)
            } else if (focusState == STATE_FAIL) {
                focusIndicator!!.showFail(false)
            }
        }
    }

    fun resetTouchFocus() {
        if (!mInitialized) {
            return
        }

        // Put focus indicator to the center. clear reset position
        mPieRenderer!!.clear()

        mFocusArea = null
        mMeteringArea = null
    }

    private fun calculateTapArea(focusWidth: Int, focusHeight: Int, areaMultiple: Float,
                                 x: Int, y: Int, previewWidth: Int, previewHeight: Int, rect: Rect) {
        val areaWidth = (focusWidth * areaMultiple).toInt()
        val areaHeight = (focusHeight * areaMultiple).toInt()
        val left = clamp(x - areaWidth / 2, 0, previewWidth - areaWidth)
        val top = clamp(y - areaHeight / 2, 0, previewHeight - areaHeight)

        val rectF = RectF(left.toFloat(), top.toFloat(), (left + areaWidth).toFloat(), (top + areaHeight).toFloat())
        mMatrix!!.mapRect(rectF)
        rectFToRect(rectF, rect)
    }

    fun removeMessages() {
        mHandler.removeMessages(RESET_TOUCH_FOCUS)
    }

    fun overrideFocusMode(focusMode: String) {
        mOverrideFocusMode = focusMode
    }

    private fun needAutoFocusCall(): Boolean {
        val focusMode = focusMode
        return !(focusMode == Parameters.FOCUS_MODE_INFINITY
                || focusMode == Parameters.FOCUS_MODE_FIXED
                || focusMode == Parameters.FOCUS_MODE_EDOF)
    }

    interface Listener {
        fun autoFocus()

        fun cancelAutoFocus()

        fun capture(): Boolean

        fun setFocusParameters()
    }

    private inner class MainHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                RESET_TOUCH_FOCUS -> {
                    cancelAutoFocus()
                }
            }
        }
    }

    companion object {
        private val TAG = "FocusOverlayManager"
        private val TRUE = "true"
        private val AUTO_EXPOSURE_LOCK_SUPPORTED = "auto-exposure-lock-supported"
        private val AUTO_WHITE_BALANCE_LOCK_SUPPORTED = "auto-whitebalance-lock-supported"

        private val RESET_TOUCH_FOCUS = 0
        private val RESET_TOUCH_FOCUS_DELAY = 3000
        private val STATE_IDLE = 0 // Focus is not active.
        private val STATE_FOCUSING = 1 // Focus is in progress.
        // Focus is in progress and the camera should take a picture after focus finishes.
        private val STATE_FOCUSING_SNAP_ON_FINISH = 2
        private val STATE_SUCCESS = 3 // Focus finishes and succeeds.
        private val STATE_FAIL = 4 // Focus finishes and fails.

        fun isAutoExposureLockSupported(params: Parameters): Boolean {
            return TRUE == params.get(AUTO_EXPOSURE_LOCK_SUPPORTED)
        }

        fun isAutoWhiteBalanceLockSupported(params: Parameters): Boolean {
            return TRUE == params.get(AUTO_WHITE_BALANCE_LOCK_SUPPORTED)
        }

        fun isSupported(value: String, supported: List<String>?): Boolean {
            return supported != null && supported.indexOf(value) >= 0
        }

        fun isMeteringAreaSupported(params: Parameters): Boolean {
            return params.maxNumMeteringAreas > 0
        }

        fun isFocusAreaSupported(params: Parameters): Boolean {
            return params.maxNumFocusAreas > 0 && isSupported(Parameters.FOCUS_MODE_AUTO,
                    params.supportedFocusModes)
        }

        fun prepareMatrix(matrix: Matrix, mirror: Boolean, displayOrientation: Int,
                          viewWidth: Int, viewHeight: Int) {
            // Need mirror for front camera.
            matrix.setScale((if (mirror) -1 else 1).toFloat(), 1f)
            // This is the value for android.hardware.Camera.setDisplayOrientation.
            matrix.postRotate(displayOrientation.toFloat())
            // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
            // UI coordinates range from (0, 0) to (width, height).
            matrix.postScale(viewWidth / 2000f, viewHeight / 2000f)
            matrix.postTranslate(viewWidth / 2f, viewHeight / 2f)
        }

        fun clamp(x: Int, min: Int, max: Int): Int {
            if (x > max) {
                return max
            }
            return if (x < min) {
                min
            } else x
        }

        fun rectFToRect(rectF: RectF, rect: Rect) {
            rect.left = Math.round(rectF.left)
            rect.top = Math.round(rectF.top)
            rect.right = Math.round(rectF.right)
            rect.bottom = Math.round(rectF.bottom)
        }
    }
}
