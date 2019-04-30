package dev.tornaco.torscreenrec.ui.widget

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.RemoteException
import android.view.*
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GestureDetectorCompat
import dev.nick.library.WatcherAdapter
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.control.FloatControlTheme
import dev.tornaco.torscreenrec.control.RecRequestHandler
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.util.ThreadUtil
import java.util.*

class FloatView(private val mApp: TorScreenRecApp) : FrameLayout(mApp) {

    private val mRect = Rect()
    private var mWm: WindowManager
    private val mLp = WindowManager.LayoutParams()

    private var mTextView: TextView? = null
    private var mContainerView: View? = null
    private var mImageView: ImageView? = null

    internal var mTouchSlop: Int = 0
    internal var density = resources.displayMetrics.density

    private var isRecording: Boolean = false

    private val watcher = object : WatcherAdapter() {
        @Throws(RemoteException::class)
        override fun onStart() {
            super.onStart()
            mImageView?.setImageResource(R.drawable.ic_stop)
            isRecording = true
        }

        @Throws(RemoteException::class)
        override fun onStop() {
            super.onStop()
            mImageView?.setImageResource(R.drawable.ic_play_arrow_black_24dp)
            mTextView?.setText(R.string.elapse_time_zero)
            isRecording = false
        }

        override fun onElapsedTimeChange(formatedTime: String?) {
            mTextView?.text = formatedTime
        }
    }

    private val observer = Observer { o, arg ->
        if (arg === SettingsProvider.Key.FLOAT_WINDOW_ALPHA) {
            ThreadUtil.mainThreadHandler
                    .post { mContainerView?.alpha = alphaSettings }
        }
    }

    private val alphaSettings: Float
        get() {
            val alpha = SettingsProvider.get()!!
                    .getInt(SettingsProvider.Key.FLOAT_WINDOW_ALPHA)
            return alpha.toFloat() / 100f
        }

    private val detectorCompat: GestureDetectorCompat

    protected val layoutId: Int
        get() = FloatControlTheme.valueOf(
                SettingsProvider.get()!!.getString(SettingsProvider.Key.FLOAT_WINDOW_THEME) ?: "")
                .layoutRes

    private var isDragging: Boolean = false
    private var inDragMode: Boolean = false

    init {

        detectorCompat = GestureDetectorCompat(mApp, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                //Toast.makeText(context, "onDoubleTap", Toast.LENGTH_SHORT).show();
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                //Toast.makeText(context, "onSingleTapUp", Toast.LENGTH_SHORT).show();
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                //Toast.makeText(context, "onLongPress", Toast.LENGTH_SHORT).show();
                inDragMode = true
                super.onLongPress(e)
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

                val x = e2.x - e1.x
                val y = e2.y - e1.y

                if (x > 0) {
                    //Toast.makeText(context, "RG:" + x, Toast.LENGTH_SHORT).show();
                } else if (x < 0) {
                    //Toast.makeText(context, "L:" + x, Toast.LENGTH_SHORT).show();
                }


                return true

            }
        })

        val rootView = LayoutInflater.from(mApp).inflate(layoutId, this)
        mContainerView = rootView.findViewById(R.id.container)
        mContainerView?.alpha = alphaSettings

        mTextView = rootView.findViewById(R.id.text)

        val clickListener = OnClickListener {
            ThreadUtil.newThread(Runnable {
                if (isRecording) {
                    RecRequestHandler.stop(context)
                } else {
                    RecRequestHandler.start(context)
                }
            }).start()
        }

        mImageView = rootView.findViewById(R.id.image)
        mImageView?.setOnClickListener(clickListener)

        getWindowVisibleDisplayFrame(mRect)

        mTouchSlop = ViewConfiguration.get(mApp).scaledTouchSlop
        mTouchSlop = mTouchSlop * mTouchSlop

        mWm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mLp.gravity = Gravity.START or Gravity.TOP
        mLp.format = PixelFormat.RGBA_8888
        mLp.width = WindowManager.LayoutParams.WRAP_CONTENT
        mLp.height = WindowManager.LayoutParams.WRAP_CONTENT
        mLp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mLp.type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        val touchListener = object : View.OnTouchListener {
            private var touchX: Float = 0.toFloat()
            private var touchY: Float = 0.toFloat()
            private var startX: Float = 0.toFloat()
            private var startY: Float = 0.toFloat()

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        touchX = event.x + left
                        touchY = event.y + top
                        startX = event.rawX
                        startY = event.rawY
                        isDragging = false
                        inDragMode = false
                    }
                    MotionEvent.ACTION_MOVE -> if (inDragMode) {
                        val dx = (event.rawX - startX).toInt()
                        val dy = (event.rawY - startY).toInt()
                        if (dx * dx + dy * dy > mTouchSlop) {
                            isDragging = true
                            mLp.x = (event.rawX - touchX).toInt()
                            mLp.y = (event.rawY - touchY).toInt()
                            mWm.updateViewLayout(this@FloatView, mLp)
                            return true
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        touchY = 0.0f
                        touchX = touchY
                        if (isDragging) {
                            reposition()
                            isDragging = false
                            inDragMode = false
                            return true
                        }
                    }
                }
                return detectorCompat.onTouchEvent(event)
            }
        }
        setOnTouchListener(touchListener)

        mApp.watch(watcher)
        SettingsProvider.get()!!.addObserver(observer)
    }

    fun attach() {
        if (parent == null) {
            mWm.addView(this, mLp)
        }
        mWm.updateViewLayout(this, mLp)
        getWindowVisibleDisplayFrame(mRect)
        mRect.top += dp2px(50)
        mLp.y = dp2px(150)
        mLp.x = mRect.width() - dp2px(55)
        reposition()
    }

    fun detach() {
        try {
            mWm.removeViewImmediate(this)
        } catch (ignored: Exception) {

        } finally {
            mApp.unWatch(watcher)
            SettingsProvider.get()!!.deleteObserver(observer)
        }
    }


    private fun dp2px(dp: Int): Int {
        return (dp * density).toInt()
    }

    private fun reposition() {
        if (mLp.x < (mRect.width() - width) / 2) {
            mLp.x = dp2px(5)
        } else {
            mLp.x = mRect.width() - dp2px(55)
        }
        if (mLp.y < mRect.top) {
            mLp.y = mRect.top
        }
        mWm.updateViewLayout(this, mLp)
    }
}