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
import android.content.res.Configuration
import android.hardware.Camera
import android.view.View
import android.view.View.MeasureSpec

import java.io.IOException


/**
 * Contains shared code for SoftwareCameraPreview and HardwareCameraPreview, cannot use inheritance
 * because those classes must inherit from separate Views, so those classes delegate calls to this
 * helper class.  Specifics for each implementation are in CameraPreviewHost
 */
class CameraPreview(private val mHost: CameraPreviewHost) {
    private var mCameraWidth = -1
    private var mCameraHeight = -1

    val context: Context
        get() = mHost.view.context

    val height: Int
        get() = mHost.view.height

    /**
     * @return True if the view is valid and prepared for the camera to start showing the preview
     */
    val isValid: Boolean
        get() = mHost.isValid

    fun setSize(size: Camera.Size, orientation: Int) {
        when (orientation) {
            0, 180 -> {
                mCameraWidth = size.width
                mCameraHeight = size.height
            }
            90, 270 -> {
                mCameraWidth = size.height
                mCameraHeight = size.width
            }
            else -> {
                mCameraWidth = size.height
                mCameraHeight = size.width
            }
        }
        mHost.view.requestLayout()
    }

    fun getWidthMeasureSpec(widthMeasureSpec: Int, heightMeasureSpec: Int): Int {
        if (mCameraHeight >= 0) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            return MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        } else {
            return widthMeasureSpec
        }
    }

    fun getHeightMeasureSpec(widthMeasureSpec: Int, heightMeasureSpec: Int): Int {
        if (mCameraHeight >= 0) {
            val orientation = context.resources.configuration.orientation
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val aspectRatio = mCameraWidth.toFloat() / mCameraHeight.toFloat()
            val height: Int
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                height = (width * aspectRatio).toInt()
            } else {
                height = (width / aspectRatio).toInt()
            }
            return MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        } else {
            return heightMeasureSpec
        }
    }

    fun onVisibilityChanged(visibility: Int) {
        if (CameraManager.hasCameraPermission()) {
            if (visibility == View.VISIBLE) {
                CameraManager.get().openCamera()
            } else {
                CameraManager.get().closeCamera()
            }
        }
    }

    fun setOnTouchListener(listener: View.OnTouchListener) {
        mHost.view.setOnTouchListener(listener)
    }

    fun onAttachedToWindow() {
        if (CameraManager.hasCameraPermission()) {
            CameraManager.get().openCamera()
        }
    }

    fun onDetachedFromWindow() {
        CameraManager.get().closeCamera()
    }

    fun onRestoreInstanceState() {
        if (CameraManager.hasCameraPermission()) {
            CameraManager.get().openCamera()
        }
    }

    fun onCameraPermissionGranted() {
        CameraManager.get().openCamera()
    }

    /**
     * Starts the camera preview on the current surface.  Abstracts out the differences in API
     * from the CameraManager
     *
     * @throws IOException Which is caught by the CameraManager to display an error
     */
    @Throws(IOException::class)
    fun startPreview(camera: Camera?) {
        mHost.startPreview(camera)
    }

    interface CameraPreviewHost {
        val view: View

        val isValid: Boolean

        @Throws(IOException::class)
        fun startPreview(camera: Camera?)

        fun onCameraPermissionGranted()

    }
}
