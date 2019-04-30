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
import android.hardware.Camera
import android.os.Parcelable
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

import java.io.IOException

/**
 * A software rendered preview surface for the camera.  This renders slower and causes more jank, so
 * HardwareCameraPreview is preferred if possible.
 *
 *
 * There is a significant amount of duplication between HardwareCameraPreview and
 * SoftwareCameraPreview which we can'data easily share due to a lack of multiple inheritance, The
 * implementations of the shared methods are delegated to CameraPreview
 */
class SoftwareCameraPreview(context: Context) : SurfaceView(context), CameraPreview.CameraPreviewHost {

    private val mPreview: CameraPreview

    override val view: View
        get() = this

    override val isValid: Boolean
        get() = holder != null

    init {
        mPreview = CameraPreview(this)
        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                CameraManager.get().setSurface(mPreview)
            }

            override fun surfaceChanged(surfaceHolder: SurfaceHolder, format: Int, width: Int,
                                        height: Int) {
                CameraManager.get().setSurface(mPreview)
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                CameraManager.get().setSurface(null)
            }
        })
    }


    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        mPreview.onVisibilityChanged(visibility)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mPreview.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mPreview.onAttachedToWindow()
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        mPreview.onRestoreInstanceState()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = mPreview.getWidthMeasureSpec(widthMeasureSpec, heightMeasureSpec)
        heightMeasureSpec = mPreview.getHeightMeasureSpec(widthMeasureSpec, heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    @Throws(IOException::class)
    override fun startPreview(camera: Camera?) {
        camera?.setPreviewDisplay(holder)
    }

    override fun onCameraPermissionGranted() {
        mPreview.onCameraPermissionGranted()
    }
}


