package dev.tornaco.torscreenrec.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.widget.LinearLayout
import android.widget.Toast
import com.tbruyelle.rxpermissions2.RxPermissions
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.pref.SettingsProvider
import ezy.assist.compat.SettingsCompat

class CameraPreviewService : Service() {

    private var mFloatView: View? = null
    private var mWindowManager: WindowManager? = null
    private var mFloatContainerParams: LayoutParams? = null
    private var mFloatViewContainer: AutoFadeLayout? = null

    private var mSize: WindowSize? = null

    private var mBinder: ServiceBinder? = null

    private val mFloatViewTouchListener = object : OnTouchListener {

        private var initialX: Int = 0
        private var initialY: Int = 0
        private var initialTouchX: Float = 0.toFloat()
        private var initialTouchY: Float = 0.toFloat()

        override fun onTouch(v: View, event: MotionEvent): Boolean {

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = mFloatContainerParams!!.x
                    initialY = mFloatContainerParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    mFloatViewContainer!!.stopFading()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    mWindowManager?.updateViewLayout(mFloatViewContainer,
                            mFloatContainerParams)
                    mFloatViewContainer!!.startFading(WINDOW_FADE_TIME)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val diffX = (event.rawX - initialTouchX).toInt()
                    val diffY = (event.rawY - initialTouchY).toInt()
                    mFloatContainerParams?.x = initialX + diffX
                    mFloatContainerParams?.y = initialY + diffY
                    mWindowManager?.updateViewLayout(mFloatViewContainer,
                            mFloatContainerParams)
                    return true
                }
            }
            return false
        }
    }

    private val isShowing: Boolean
        get() = mFloatViewContainer != null && mFloatViewContainer!!.isAttachedToWindow && mFloatView!!.isAttachedToWindow

    override fun onBind(intent: Intent): IBinder? {
        if (mBinder == null) mBinder = ServiceBinder()
        return mBinder
    }

    private fun showPreviewWithOverlayChecked(size: WindowSize) {
        // Check permission.
        if (SettingsCompat.canDrawOverlays(TorScreenRecApp.app!!.topActivity) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                showPreview(size)
            } catch (e: Throwable) {
                Toast.makeText(applicationContext, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
            }

        } else {
            try {
                SettingsCompat.manageDrawOverlays(TorScreenRecApp.app!!.topActivity)
            } catch (e: Throwable) {
                Toast.makeText(applicationContext, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    internal fun showPreview(size: WindowSize) {
        if (isShowing) {
            return
        }

        // Check settings.
        val app = application as TorScreenRecApp

        if (SettingsCompat.canDrawOverlays(app.topActivity)) {
            // EMPTY.
        } else {
            try {
                SettingsCompat.manageDrawOverlays(app.topActivity)
            } catch (e: Throwable) {
                Toast.makeText(applicationContext, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
            }

            SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.CAMERA, false)
            return
        }

        mSize = size
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        mFloatView = SoftwareCameraPreview(this)
        mFloatContainerParams = LayoutParams(
                mSize!!.w,
                mSize!!.h,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    LayoutParams.TYPE_SYSTEM_ALERT,
                LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
        mFloatContainerParams!!.y = 0
        mFloatContainerParams!!.x = 0
        mFloatViewContainer = LayoutInflater.from(this).inflate(R.layout.float_containor, null) as AutoFadeLayout
        mFloatViewContainer?.setOnTouchListener(mFloatViewTouchListener)
        mFloatViewContainer?.layoutParams = LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        mWindowManager?.addView(mFloatViewContainer, mFloatContainerParams)
        mFloatViewContainer!!.addView(mFloatView)
        mFloatViewContainer!!.startFading(WINDOW_FADE_TIME)
    }

    private fun hidePreview() {
        if (isShowing) {
            mWindowManager?.removeView(mFloatViewContainer)
        }
    }

    internal fun setSize(size: WindowSize) {
        this.mSize = size
        if (isShowing) {
            mFloatContainerParams?.width = size.w
            mFloatContainerParams?.height = size.h
            mWindowManager?.updateViewLayout(mFloatViewContainer,
                    mFloatContainerParams)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        if (mFloatView != null) {
            mWindowManager?.removeView(mFloatView)
        }
        super.onDestroy()
    }

    internal class WindowSize internal constructor(internal var w: Int, internal var h: Int) {

        override fun toString(): String {
            return "WindowSize{" +
                    "w=" + w +
                    ", h=" + h +
                    '}'.toString()
        }

        companion object {

            internal var DEFAULT = WindowSize(320, 480)
            internal var LARGE = WindowSize(480, 720)
            internal var SMALL = WindowSize(240, 360)
        }
    }

    private inner class ServiceBinder : Binder(), ICameraPreviewService {

        override val isShowing: Boolean
            get() = this@CameraPreviewService.isShowing

        override fun show(sizeIndex: Int) {
            val size: WindowSize
            when (sizeIndex) {
                PreviewSize.LARGE -> size = WindowSize.LARGE
                PreviewSize.SMALL -> size = WindowSize.SMALL
                PreviewSize.NORMAL -> size = WindowSize.DEFAULT
                else -> throw IllegalArgumentException("Bad size index:$sizeIndex")
            }

            showPreviewChecked(size)
        }

        private fun showPreviewChecked(size: WindowSize) {
            val rxPermissions = RxPermissions(TorScreenRecApp.app!!.topActivity!!)
            rxPermissions.request(Manifest.permission.CAMERA)
                    .subscribe { granted ->
                        if (granted!!) {
                            this@CameraPreviewService.showPreviewWithOverlayChecked(size)
                        } else {
                            SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.CAMERA, false)
                        }
                    }.toString()
        }

        override fun hide() {
            this@CameraPreviewService.hidePreview()
        }

        override fun setSize(sizeIndex: Int) {
            val size: WindowSize
            when (sizeIndex) {
                PreviewSize.LARGE -> size = WindowSize.LARGE
                PreviewSize.SMALL -> size = WindowSize.SMALL
                PreviewSize.NORMAL -> size = WindowSize.DEFAULT
                else -> throw IllegalArgumentException("Bad size index:$sizeIndex")
            }
            this@CameraPreviewService.setSize(size)
        }
    }

    companion object {

        private val WINDOW_FADE_TIME = (5 * 1000).toLong()
    }
}
