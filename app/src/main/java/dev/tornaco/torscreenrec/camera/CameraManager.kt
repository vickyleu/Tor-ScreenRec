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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.AsyncTask
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.*
import com.google.common.base.Preconditions
import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.pref.SettingsProvider
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

/**
 * Class which manages interactions with the camera, but does not do any UI.  This class is
 * designed to be a singleton to ensure there is one component managing the camera and releasing
 * the native resources.
 * In order to acquire a camera, a caller must:
 *
 *  * Call selectCamera to select front or back camera
 *  * Call setSurface to control where the preview is shown
 *  * Call openCamera to request the camera start preview
 *
 * Callers should call onPause and onResume to ensure that the camera is release while the activity
 * is not active.
 * This class is not thread safe.  It should only be called from one thread (the UI thread or test
 * thread)
 */
class CameraManager private constructor() : FocusOverlayManager.Listener {
    /**
     * The CameraInfo for the currently selected camera
     */
    private val mCameraInfo: CameraInfo
    /**
     * True if the device has front and back cameras
     */
    private val mHasFrontAndBackCamera: Boolean
    /**
     * Manages auto focus visual and behavior
     */
    private val mFocusOverlayManager: FocusOverlayManager
    /**
     * The index of the selected camera or NO_CAMERA_SELECTED if a camera hasn'data been selected yet
     */
    internal var cameraIndex: Int = 0
        private set
    /**
     * True if the camera should be open (may not yet be actually open)
     */
    private var mOpenRequested: Boolean = false
    /**
     * True if the camera is requested to be in video mode
     */
    private var mVideoModeRequested: Boolean = false
    /**
     * The media recorder for video mode
     */
    private var mMediaRecorder: MmsVideoRecorder? = null
    /**
     * Callback to call with video recording updates
     */
    private var mVideoCallback: MediaCallback? = null
    /**
     * The preview view to show the preview on
     */
    private var mCameraPreview: CameraPreview? = null
    /**
     * The helper classs to handle orientation changes
     */
    private var mOrientationHandler: OrientationHandler? = null
    /**
     * Tracks whether the preview has hardware acceleration
     */
    private var mIsHardwareAccelerationSupported: Boolean = false
    /**
     * The task for opening the camera, so it doesn'data block the UI thread
     * Using AsyncTask rather than SafeAsyncTask because the tasks need to be serialized, but don'data
     * need to be on the UI thread
     * TODO: If we have other AyncTasks (not SafeAsyncTasks) this may contend and we may
     * need to create a dedicated thread, or synchronize the threads in the thread pool
     */
    private var mOpenCameraTask: AsyncTask<Int, Void, Camera>? = null
    /**
     * The camera index that is queued to be opened, but not completed yet, or NO_CAMERA_SELECTED if
     * no open task is pending
     */
    private var mPendingOpenCameraIndex = NO_CAMERA_SELECTED
    /**
     * The instance of the currently opened camera
     */
    private var mCamera: Camera? = null
    /**
     * The rotation of the screen relative to the camera's natural orientation
     */
    private var mRotation: Int = 0
    /**
     * The callback to notify when errors or other events occur
     */
    private var mListener: CameraManagerListener? = null
    /**
     * True if the camera is currently in the process of taking an image
     */
    private val mTakingPicture: Boolean = false

    private var mSavedOrientation: Int? = null

    internal val cameraInfo: CameraInfo?
        get() = if (cameraIndex == NO_CAMERA_SELECTED) {
            null
        } else mCameraInfo

    internal var isVideoMode: Boolean
        get() = mVideoModeRequested
        set(videoMode) {
            if (mVideoModeRequested == videoMode) {
                return
            }
            mVideoModeRequested = videoMode
            tryInitOrCleanupVideoMode()
        }

    internal val isRecording: Boolean
        get() = mVideoModeRequested && mVideoCallback != null

    internal val isCameraAvailable: Boolean
        get() = mCamera != null && !mTakingPicture && mIsHardwareAccelerationSupported

    init {
        mCameraInfo = CameraInfo()
        cameraIndex = NO_CAMERA_SELECTED

        // Check to see if a front and back camera exist
        var hasFrontCamera = false
        var hasBackCamera = false
        val cameraInfo = CameraInfo()
        val cameraCount = sCameraWrapper.numberOfCameras
        try {
            for (i in 0 until cameraCount) {
                sCameraWrapper.getCameraInfo(i, cameraInfo)
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
                    hasFrontCamera = true
                } else if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
                    hasBackCamera = true
                }
                if (hasFrontCamera && hasBackCamera) {
                    break
                }
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Unable to load camera info", e)
        }

        mHasFrontAndBackCamera = hasFrontCamera && hasBackCamera
        mFocusOverlayManager = FocusOverlayManager(this, Looper.getMainLooper())

        // Assume the best until we are proven otherwise
        mIsHardwareAccelerationSupported = true
    }

    /**
     * Sets the surface to use to display the preview
     * This must only be called AFTER the CameraPreview has a texture ready
     *
     * @param preview The preview surface view
     */
    internal fun setSurface(preview: CameraPreview?) {
        if (preview === mCameraPreview) {
            return
        }

        if (preview != null) {
            Preconditions.checkArgument(preview.isValid)
            preview.setOnTouchListener(object : View.OnTouchListener {
                @SuppressLint("ClickableViewAccessibility")
                override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
                    if (motionEvent?.actionMasked ?: 0 and MotionEvent.ACTION_UP == MotionEvent.ACTION_UP) {
                        mFocusOverlayManager.setPreviewSize(view?.width ?: 0, view?.height ?: 0)
                        mFocusOverlayManager.onSingleTapUp(
                                motionEvent?.x?.toInt() ?: 0 + (view?.left ?: 0),
                                motionEvent?.y?.toInt() ?: 0 + (view?.top ?: 0))
                    }
                    return true
                }
            })

        }
        mCameraPreview = preview
        tryShowPreview()
    }

    internal fun setRenderOverlay(renderOverlay: RenderOverlay?) {
        mFocusOverlayManager.setFocusRenderer(renderOverlay?.pieRenderer)
    }

    /**
     * Convenience function to swap between front and back facing cameras
     */
    fun swapCamera() {
        if (cameraIndex >= 0) {
            selectCamera(if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
                CameraInfo.CAMERA_FACING_BACK
            else
                CameraInfo.CAMERA_FACING_FRONT)
        }
    }

    /**
     * Selects the first camera facing the desired direction, or the first camera if there is no
     * camera in the desired direction
     *
     * @param desiredFacing One of the CameraInfo.CAMERA_FACING_* constants
     * @return True if a camera was selected, or false if selecting a camera failed
     */
    internal fun selectCamera(desiredFacing: Int): Boolean {
        try {
            // We already selected a camera facing that direction
            if (cameraIndex >= 0 && mCameraInfo.facing == desiredFacing) {
                return true
            }

            val cameraCount = sCameraWrapper.numberOfCameras
            Preconditions.checkArgument(cameraCount > 0)

            cameraIndex = NO_CAMERA_SELECTED
            setCamera(null)
            val cameraInfo = CameraInfo()
            for (i in 0 until cameraCount) {
                sCameraWrapper.getCameraInfo(i, cameraInfo)
                if (cameraInfo.facing == desiredFacing) {
                    cameraIndex = i
                    sCameraWrapper.getCameraInfo(i, mCameraInfo)
                    break
                }
            }

            // There's no camera in the desired facing direction, just select the first camera
            // regardless of direction
            if (cameraIndex < 0) {
                cameraIndex = 0
                sCameraWrapper.getCameraInfo(0, mCameraInfo)
            }

            if (mOpenRequested) {
                // The camera is open, so reopen with the newly selected camera
                openCamera()
            }
            return true
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException in CameraManager.selectCamera", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_OPENING_CAMERA, e)
            }
            return false
        }

    }

    internal fun selectCameraByIndex(cameraIndex: Int) {
        if (this.cameraIndex == cameraIndex) {
            return
        }

        try {
            this.cameraIndex = cameraIndex
            sCameraWrapper.getCameraInfo(this.cameraIndex, mCameraInfo)
            if (mOpenRequested) {
                openCamera()
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException in CameraManager.selectCameraByIndex", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_OPENING_CAMERA, e)
            }
        }

    }

    /**
     * @return True if this device has camera capabilities
     */
    internal fun hasAnyCamera(): Boolean {
        return sCameraWrapper.numberOfCameras > 0
    }

    /**
     * @return True if the device has both a front and back camera
     */
    internal fun hasFrontAndBackCamera(): Boolean {
        return mHasFrontAndBackCamera
    }

    /**
     * Opens the camera on a separate thread and initiates the preview if one is available
     */
    internal fun openCamera() {
        if (cameraIndex == NO_CAMERA_SELECTED) {
            // Ensure a selected camera if none is currently selected. This may happen if the
            // camera chooser is not the default media chooser.
            selectCamera(SettingsProvider.get()!!.getInt(SettingsProvider.Key.PREFERRED_CAMERA))
        }
        mOpenRequested = true
        // We're already opening the camera or already have the camera handle, nothing more to do
        if (mPendingOpenCameraIndex == cameraIndex || mCamera != null) {
            return
        }

        // True if the task to open the camera has to be delayed until the current one completes
        var delayTask = false

        // Cancel any previous open camera tasks
        if (mOpenCameraTask != null) {
            mPendingOpenCameraIndex = NO_CAMERA_SELECTED
            delayTask = true
        }

        mPendingOpenCameraIndex = cameraIndex
        mOpenCameraTask = @SuppressLint("StaticFieldLeak")
        object : AsyncTask<Int, Void, Camera>() {
            private var mException: Exception? = null

            protected override fun doInBackground(vararg params: Int?): Camera? {
                try {
                    val cameraIndex = params[0]
                    if (Log.isLoggable(TAG, Log.VERBOSE)) {
                        Log.v(TAG, "Opening camera " + cameraIndex)
                    }
                    return sCameraWrapper.open(cameraIndex)
                } catch (e: Exception) {
                    Log.e(TAG, "Exception while opening camera", e)
                    mException = e
                    return null
                }

            }

            override fun onPostExecute(camera: Camera?) {
                // If we completed, but no longer want this camera, then release the camera
                if (mOpenCameraTask !== this || !mOpenRequested) {
                    releaseCamera(camera)
                    cleanup()
                    return
                }

                cleanup()

                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Opened camera " + cameraIndex + " " + (camera != null))
                }

                setCamera(camera)
                if (camera == null) {
                    if (mListener != null) {
                        mListener!!.onCameraError(ERROR_OPENING_CAMERA, mException)
                    }
                    Log.e(TAG, "Error opening camera")
                }
            }

            override fun onCancelled() {
                super.onCancelled()
                cleanup()
            }

            private fun cleanup() {
                mPendingOpenCameraIndex = NO_CAMERA_SELECTED
                if (mOpenCameraTask != null && mOpenCameraTask!!.status == AsyncTask.Status.PENDING) {
                    // If there's another task waiting on this one to complete, start it now
                    mOpenCameraTask!!.execute(cameraIndex)
                } else {
                    mOpenCameraTask = null
                }

            }
        }
        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Start opening camera $cameraIndex")
        }

        if (!delayTask) {
            mOpenCameraTask!!.execute(cameraIndex)
        }
    }

    /**
     * Closes the camera releasing the resources it uses
     */
    internal fun closeCamera() {
        mOpenRequested = false
        setCamera(null)
    }

    /**
     * Temporarily closes the camera if it is open
     */
    internal fun onPause() {
        setCamera(null)
    }

    /**
     * Reopens the camera if it was opened when onPause was called
     */
    internal fun onResume() {
        if (mOpenRequested) {
            openCamera()
        }
    }

    /**
     * Sets the listener which will be notified of errors or other events in the camera
     *
     * @param listener The listener to notify
     */
    internal fun setListener(listener: CameraManagerListener) {
        mListener = listener
        if (!mIsHardwareAccelerationSupported && mListener != null) {
            mListener!!.onCameraError(ERROR_HARDWARE_ACCELERATION_DISABLED, null)
        }
    }

    internal fun startVideo(callback: MediaCallback) {
        Preconditions.checkArgument(!isRecording)
        mVideoCallback = callback
        tryStartVideoCapture()
    }

    /**
     * Asynchronously releases a camera
     *
     * @param camera The camera to release
     */
    private fun releaseCamera(camera: Camera?) {
        if (camera == null) {
            return
        }

        mFocusOverlayManager.onCameraReleased()

        object : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "Releasing camera $cameraIndex")
                }
                sCameraWrapper.release(camera)
                return null
            }
        }.execute()
    }

    private fun releaseMediaRecorder(cleanupFile: Boolean) {
        if (mMediaRecorder == null) {
            return
        }
        mVideoModeRequested = false

        if (cleanupFile) {
            mMediaRecorder!!.cleanupTempFile()
            if (mVideoCallback != null) {
                val callback = mVideoCallback
                mVideoCallback = null
                // Notify the callback that we've stopped recording
                callback!!.onMediaReady(null, null, 0 /*width*/,
                        0 /*height*/)/*uri*//*contentType*/
            }
        }

        mMediaRecorder!!.release()
        mMediaRecorder = null

        if (mCamera != null) {
            try {
                mCamera!!.reconnect()
            } catch (e: IOException) {
                Log.e(TAG, "IOException in CameraManager.releaseMediaRecorder", e)
                if (mListener != null) {
                    mListener!!.onCameraError(ERROR_OPENING_CAMERA, e)
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "RuntimeException in CameraManager.releaseMediaRecorder", e)
                if (mListener != null) {
                    mListener!!.onCameraError(ERROR_OPENING_CAMERA, e)
                }
            }

        }
        restoreRequestedOrientation()
    }

    /**
     * Updates the orientation of the camera to match the orientation of the device
     */
    private fun updateCameraOrientation() {
        if (mCamera == null || mCameraPreview == null || mTakingPicture) {
            return
        }

        val windowManager = mCameraPreview!!.context.getSystemService(
                Context.WINDOW_SERVICE) as WindowManager

        var degrees = 0
        when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        // The display orientation of the camera (this controls the preview image).
        var orientation: Int

        // The clockwise rotation angle relative to the orientation of the camera. This affects
        // pictures returned by the camera in Camera.PictureCallback.
        val rotation: Int
        if (mCameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            orientation = (mCameraInfo.orientation + degrees) % 360
            rotation = orientation
            // compensate the mirror but only for orientation
            orientation = (360 - orientation) % 360
        } else {  // back-facing
            orientation = (mCameraInfo.orientation - degrees + 360) % 360
            rotation = orientation
        }
        mRotation = rotation
        if (mMediaRecorder == null) {
            try {
                mCamera!!.setDisplayOrientation(orientation)
                val params = mCamera!!.parameters
                params.setRotation(rotation)
                mCamera!!.parameters = params
            } catch (e: RuntimeException) {
                Log.e(TAG, "RuntimeException in CameraManager.updateCameraOrientation", e)
                if (mListener != null) {
                    mListener!!.onCameraError(ERROR_OPENING_CAMERA, e)
                }
            }

        }
    }

    /**
     * Sets the current camera, releasing any previously opened camera
     */
    private fun setCamera(camera: Camera?) {
        if (mCamera === camera) {
            return
        }

        releaseMediaRecorder(true /* cleanupFile */)
        releaseCamera(mCamera)
        mCamera = camera
        tryShowPreview()
        if (mListener != null) {
            mListener!!.onCameraChanged()
        }
    }

    /**
     * Shows the preview if the camera is open and the preview is loaded
     */
    private fun tryShowPreview() {
        if (mCameraPreview == null || mCamera == null) {
            if (mOrientationHandler != null) {
                mOrientationHandler!!.disable()
                mOrientationHandler = null
            }
            releaseMediaRecorder(true /* cleanupFile */)
            mFocusOverlayManager.onPreviewStopped()
            return
        }
        try {
            mCamera!!.stopPreview()
            updateCameraOrientation()

            val params = mCamera!!.parameters
            val pictureSize = chooseBestPictureSize()
            val previewSize = chooseBestPreviewSize()
            params.setPreviewSize(previewSize.width, previewSize.height)
            params.setPictureSize(pictureSize.width, pictureSize.height)
            logCameraSize("Setting preview size: ", previewSize)
            logCameraSize("Setting picture size: ", pictureSize)
            mCameraPreview!!.setSize(previewSize, mCameraInfo.orientation)
            for (focusMode in params.supportedFocusModes) {
                if (TextUtils.equals(focusMode, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    // Use continuous focus if available
                    params.focusMode = focusMode
                    break
                }
            }

            mCamera!!.parameters = params
            mCameraPreview!!.startPreview(mCamera)
            mCamera!!.startPreview()
            mCamera!!.setAutoFocusMoveCallback { start, camera -> mFocusOverlayManager.onAutoFocusMoving(start) }
            mFocusOverlayManager.setParameters(mCamera!!.parameters)
            mFocusOverlayManager.setMirror(mCameraInfo.facing == CameraInfo.CAMERA_FACING_BACK)
            mFocusOverlayManager.onPreviewStarted()
            tryInitOrCleanupVideoMode()
            if (mOrientationHandler == null) {
                mOrientationHandler = OrientationHandler(mCameraPreview!!.context)
                mOrientationHandler!!.enable()
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException in CameraManager.tryShowPreview", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_SHOWING_PREVIEW, e)
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException in CameraManager.tryShowPreview", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_SHOWING_PREVIEW, e)
            }
        }

    }

    private fun tryInitOrCleanupVideoMode() {
        if (!mVideoModeRequested || mCamera == null || mCameraPreview == null) {
            releaseMediaRecorder(true /* cleanupFile */)
            return
        }

        if (mMediaRecorder != null) {
            return
        }

        try {
            mCamera!!.unlock()
            val maxMessageSize = 1024 * 1024//FIXME
            mMediaRecorder = MmsVideoRecorder(mCamera, cameraIndex, mRotation, maxMessageSize)
            mMediaRecorder!!.prepare()
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "FileNotFoundException in CameraManager.tryInitOrCleanupVideoMode", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_STORAGE_FAILURE, e)
            }
            isVideoMode = false
            return
        } catch (e: IOException) {
            Log.e(TAG, "IOException in CameraManager.tryInitOrCleanupVideoMode", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_INITIALIZING_VIDEO, e)
            }
            isVideoMode = false
            return
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException in CameraManager.tryInitOrCleanupVideoMode", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_INITIALIZING_VIDEO, e)
            }
            isVideoMode = false
            return
        }

        tryStartVideoCapture()
    }

    private fun tryStartVideoCapture() {
        if (mMediaRecorder == null || mVideoCallback == null) {
            return
        }

        mMediaRecorder!!.setOnErrorListener { mediaRecorder, what, extra ->
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_RECORDING_VIDEO, null)
            }
            restoreRequestedOrientation()
        }

        mMediaRecorder!!.setOnInfoListener { mediaRecorder, what, extra ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                stopVideo()
            }
        }

        try {
            mMediaRecorder?.start()
            val activity = TorScreenRecApp.app?.topActivity
            activity!!.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lockOrientation()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "IllegalStateException in CameraManager.tryStartVideoCapture", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_RECORDING_VIDEO, e)
            }
            isVideoMode = false
            restoreRequestedOrientation()
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException in CameraManager.tryStartVideoCapture", e)
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_RECORDING_VIDEO, e)
            }
            isVideoMode = false
            restoreRequestedOrientation()
        }

    }

    internal fun stopVideo() {
        var width = -1
        var height = -1
        var uri: Uri? = null
        var contentType: String? = null
        try {
            //            final Activity activity = Factory.get().getTopActivity();
            //            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mMediaRecorder!!.stop()
            width = mMediaRecorder!!.videoWidth
            height = mMediaRecorder!!.videoHeight
            uri = mMediaRecorder!!.videoUri
            contentType = mMediaRecorder!!.contentType
        } catch (e: RuntimeException) {
            // MediaRecorder.stop will throw a RuntimeException if the video was too short, let the
            // finally clause call the callback with null uri and handle cleanup
            Log.e(TAG, "RuntimeException in CameraManager.stopVideo", e)
        } finally {
            val videoCallback = mVideoCallback
            mVideoCallback = null
            releaseMediaRecorder(false /* cleanupFile */)
            if (uri == null) {
                tryInitOrCleanupVideoMode()
            }
            videoCallback!!.onMediaReady(uri, contentType, width, height)
        }
    }

    /**
     * External components call into this to report if hardware acceleration is supported.  When
     * hardware acceleration isn'data supported, we need to report an error through the listener
     * interface
     *
     * @param isHardwareAccelerationSupported True if the preview is rendering in a hardware
     * accelerated view.
     */
    internal fun reportHardwareAccelerationSupported(isHardwareAccelerationSupported: Boolean) {
        if (mIsHardwareAccelerationSupported == isHardwareAccelerationSupported) {
            // If the value hasn'data changed nothing more to do
            return
        }

        mIsHardwareAccelerationSupported = isHardwareAccelerationSupported
        if (!isHardwareAccelerationSupported) {
            Log.e(TAG, "Software rendering - cannot open camera")
            if (mListener != null) {
                mListener!!.onCameraError(ERROR_HARDWARE_ACCELERATION_DISABLED, null)
            }
        }
    }

    /**
     * Returns the scale factor to scale the width/height to max allowed in Config
     */
    private fun getScaleFactorForMaxAllowedSize(width: Int, height: Int,
                                                maxWidth: Int, maxHeight: Int): Float {
        if (maxWidth <= 0 || maxHeight <= 0) {
            // MmsConfig initialization runs asynchronously on application startup, so there's a
            // chance (albeit a very slight one) that we don'data have it yet.
            Log.w(TAG, "Max image size not loaded in MmsConfig")
            return 1.0f
        }

        return if (width <= maxWidth && height <= maxHeight) {
            // Already meeting requirements.
            1.0f
        } else Math.min(maxWidth * 1.0f / width, maxHeight * 1.0f / height)

    }

    /**
     * Choose the best picture size by trying to find a size close to the MmsConfig's max size,
     * which is closest to the screen aspect ratio
     */
    private fun chooseBestPictureSize(): Camera.Size {
        val context = mCameraPreview!!.context
        val resources = context.resources
        val displayMetrics = resources.displayMetrics
        val displayOrientation = resources.configuration.orientation
        var cameraOrientation = mCameraInfo.orientation

        var screenWidth: Int
        var screenHeight: Int
        if (displayOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Rotate the camera orientation 90 degrees to compensate for the rotated display
            // metrics. Direction doesn'data matter because we're just using it for width/height
            cameraOrientation += 90
        }

        // Check the camera orientation relative to the display.
        // For 0, 180, 360, the screen width/height are the display width/height
        // For 90, 270, the screen width/height are inverted from the display
        if (cameraOrientation % 180 == 0) {
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
        } else {
            screenWidth = displayMetrics.heightPixels
            screenHeight = displayMetrics.widthPixels
        }

        val maxWidth = screenWidth / 3
        val maxHeight = screenHeight / 3

        // Constrain the size within the max width/height defined by MmsConfig.
        val scaleFactor = getScaleFactorForMaxAllowedSize(screenWidth, screenHeight,
                maxWidth, maxHeight)
        screenWidth *= scaleFactor.toInt()
        screenHeight *= scaleFactor.toInt()

        val aspectRatio = 0.5f//FIXME
        val sizes = ArrayList(
                mCamera!!.parameters.supportedPictureSizes)
        val maxPixels = maxWidth * maxHeight

        // Sort the sizes so the best size is first
        Collections.sort<Camera.Size>(sizes, SizeComparator(maxWidth, maxHeight, aspectRatio, maxPixels))

        return sizes[0]
    }

    /**
     * Chose the best preview size based on the picture size.  Try to find a size with the same
     * aspect ratio and size as the picture if possible
     */
    private fun chooseBestPreviewSize(): Camera.Size {
        val sizes = ArrayList(
                mCamera!!.parameters.supportedPreviewSizes)
        return sizes[sizes.size - 1]
    }

    override// From FocusOverlayManager.Listener
    fun autoFocus() {
        if (mCamera == null) {
            return
        }

        try {
            mCamera!!.autoFocus { success, camera -> mFocusOverlayManager.onAutoFocus(success, false /* shutterDown */) }
        } catch (e: RuntimeException) {
            Log.e(TAG, "RuntimeException in CameraManager.autoFocus", e)
            // If autofocus fails, the camera should have called the callback with success=false,
            // but some throw an exception here
            mFocusOverlayManager.onAutoFocus(false /*success*/, false /*shutterDown*/)
        }

    }

    override// From FocusOverlayManager.Listener
    fun cancelAutoFocus() {
        if (mCamera == null) {
            return
        }
        try {
            mCamera!!.cancelAutoFocus()
        } catch (e: RuntimeException) {
            // Ignore
            Log.e(TAG, "RuntimeException in CameraManager.cancelAutoFocus", e)
        }

    }

    override// From FocusOverlayManager.Listener
    fun capture(): Boolean {
        return false
    }

    override// From FocusOverlayManager.Listener
    fun setFocusParameters() {
        if (mCamera == null) {
            return
        }
        try {
            val parameters = mCamera!!.parameters
            parameters.focusMode = mFocusOverlayManager.focusMode
            if (parameters.maxNumFocusAreas > 0) {
                // Don'data set focus areas (even to null) if focus areas aren'data supported, camera may
                // crash
                parameters.focusAreas = mFocusOverlayManager.focusAreas
            }
            parameters.meteringAreas = mFocusOverlayManager.meteringAreas
            mCamera!!.parameters = parameters
        } catch (e: RuntimeException) {
            // This occurs when the device is out of space or when the camera is locked
            Log.e(TAG, "RuntimeException in CameraManager setFocusParameters")
        }

    }

    private fun logCameraSize(prefix: String, size: Camera.Size) {
        // Log the camera size and aspect ratio for help when examining bug reports for camera
        // failures
        Log.i(TAG, prefix + size.width + "x" + size.height +
                " (" + size.width / size.height.toFloat() + ")")
    }

    private fun lockOrientation() {
        // when we start recording, lock our orientation
        val a = TorScreenRecApp.app!!.topActivity
        val windowManager = a!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val rotation = windowManager.defaultDisplay.rotation

        mSavedOrientation = a!!.getRequestedOrientation()
        when (rotation) {
            Surface.ROTATION_0 -> a!!.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            Surface.ROTATION_90 -> a!!.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            Surface.ROTATION_180 -> a!!.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
            Surface.ROTATION_270 -> a!!.setRequestedOrientation(
                    ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
        }

    }

    private fun restoreRequestedOrientation() {
        if (mSavedOrientation != null) {
            val a = TorScreenRecApp.app!!.topActivity
            if (a != null) {
                a!!.setRequestedOrientation(mSavedOrientation!!)
            }
            mSavedOrientation = null
        }
    }

    /**
     * Wrapper around the framework camera API to allow mocking different hardware scenarios while
     * unit testing
     */
    internal interface CameraWrapper {
        val numberOfCameras: Int

        fun getCameraInfo(index: Int, cameraInfo: CameraInfo)

        fun open(cameraId: Int?): Camera?

        /**
         * Add a wrapper for release because a final method cannot be mocked
         */
        fun release(camera: Camera?)
    }


    /**
     * Callbacks for the camera manager listener
     */
    internal interface CameraManagerListener {
        fun onCameraError(errorCode: Int, e: Exception?)

        fun onCameraChanged()
    }

    /**
     * Callback when taking image or video
     */
    internal interface MediaCallback {

        fun onMediaReady(uriToMedia: Uri?, contentType: String?, width: Int, height: Int)

        fun onMediaFailed(exception: Exception)

        fun onMediaInfo(what: Int)

        companion object {
            val MEDIA_CAMERA_CHANGED = 1
            val MEDIA_NO_DATA = 2
        }
    }

    private class SizeComparator(// The max width/height for the preferred size. Integer.MAX_VALUE if no size limit
            private val mMaxWidth: Int, private val mMaxHeight: Int,
            // The desired aspect ratio
            private val mTargetAspectRatio: Float, // The desired size (width x height) to try to match
            private val mTargetPixels: Int) : Comparator<Camera.Size> {

        /**
         * Returns a negative value if left is a better choice than right, or a positive value if
         * right is a better choice is better than left.  0 if they are equal
         */
        override fun compare(left: Camera.Size, right: Camera.Size): Int {
            // If one size is less than the max size prefer it over the other
            if ((left.width <= mMaxWidth && left.height <= mMaxHeight) != (right.width <= mMaxWidth && right.height <= mMaxHeight)) {
                return if (left.width <= mMaxWidth) PREFER_LEFT else PREFER_RIGHT
            }

            // If one is closer to the target aspect ratio, prefer it.
            val leftAspectRatio = left.width / left.height.toFloat()
            val rightAspectRatio = right.width / right.height.toFloat()
            val leftAspectRatioDiff = Math.abs(leftAspectRatio - mTargetAspectRatio)
            val rightAspectRatioDiff = Math.abs(rightAspectRatio - mTargetAspectRatio)
            if (leftAspectRatioDiff != rightAspectRatioDiff) {
                return if (leftAspectRatioDiff - rightAspectRatioDiff < 0)
                    PREFER_LEFT
                else
                    PREFER_RIGHT
            }

            // At this point they have the same aspect ratio diff and are either both bigger
            // than the max size or both smaller than the max size, so prefer the one closest
            // to target size
            val leftDiff = Math.abs(left.width * left.height - mTargetPixels)
            val rightDiff = Math.abs(right.width * right.height - mTargetPixels)
            return leftDiff - rightDiff
        }

        companion object {
            private val PREFER_LEFT = -1
            private val PREFER_RIGHT = 1
        }
    }

    private inner class OrientationHandler internal constructor(context: Context) : OrientationEventListener(context) {

        override fun onOrientationChanged(orientation: Int) {
            updateCameraOrientation()
        }
    }

    companion object {
        // Error codes
        internal val ERROR_OPENING_CAMERA = 1
        internal val ERROR_SHOWING_PREVIEW = 2
        internal val ERROR_INITIALIZING_VIDEO = 3
        internal val ERROR_STORAGE_FAILURE = 4
        internal val ERROR_RECORDING_VIDEO = 5
        internal val ERROR_HARDWARE_ACCELERATION_DISABLED = 6
        internal val ERROR_TAKING_PICTURE = 7
        private val TAG = CameraManager::class.java.simpleName
        private val NO_CAMERA_SELECTED = -1
        private var sInstance: CameraManager? = null
        /**
         * Default camera wrapper which directs calls to the framework APIs
         */
        private var sCameraWrapper: CameraWrapper = object : CameraWrapper {
            override val numberOfCameras: Int
                get() = Camera.getNumberOfCameras()

            override fun getCameraInfo(index: Int, cameraInfo: CameraInfo) {
                Camera.getCameraInfo(index, cameraInfo)
            }

            override fun open(cameraId: Int?): Camera? {
                return Camera.open(cameraId ?: 0)
            }

            override fun release(camera: Camera?) {
                camera?.release()
            }
        }

        /**
         * Gets the singleton instance
         */
        fun get(): CameraManager {
            if (sInstance == null) {
                sInstance = CameraManager()
            }
            return sInstance!!
        }

        /**
         * Allows tests to inject a custom camera wrapper
         */
        internal fun setCameraWrapper(cameraWrapper: CameraWrapper) {
            sCameraWrapper = cameraWrapper
            sInstance = null
        }

        internal fun hasCameraPermission(): Boolean {
            return OsUtil.hasPermission(Manifest.permission.CAMERA)
        }
    }
}
