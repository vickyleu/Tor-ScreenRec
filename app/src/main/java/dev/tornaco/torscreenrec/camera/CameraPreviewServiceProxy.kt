package dev.tornaco.torscreenrec.camera

import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException

import dev.nick.library.ServiceProxy


class CameraPreviewServiceProxy private constructor(context: Context) : ServiceProxy(context, Intent(context, CameraPreviewService::class.java)), ICameraPreviewService {

    internal var mService: ICameraPreviewService?=null

    override val isShowing: Boolean
        get() = throw UnsupportedOperationException()

    init {
        context.startService(Intent(context, CameraPreviewService::class.java))
    }

    override fun show(size: Int) {
        setTask(object : ServiceProxy.ProxyTask() {
            @Throws(RemoteException::class)
            override fun run() {
                mService?.show(size)
            }

            override fun forUI(): Boolean {
                return true
            }
        })
    }

    override fun hide() {
        setTask(object : ServiceProxy.ProxyTask() {
            @Throws(RemoteException::class)
            override fun run() {
                mService?.hide()
            }

            override fun forUI(): Boolean {
                return true
            }
        })
    }

    override fun setSize(index: Int) {
        setTask(object : ServiceProxy.ProxyTask() {
            @Throws(RemoteException::class)
            override fun run() {
                mService?.setSize(index)
            }

            override fun forUI(): Boolean {
                return true
            }
        })
    }

    override fun onConnected(binder: IBinder) {
        mService = binder as? ICameraPreviewService
    }

    companion object {

        fun show(context: Context, size: Int) {
            CameraPreviewServiceProxy(context).show(size)
        }

        fun hide(context: Context) {
            CameraPreviewServiceProxy(context).hide()
        }

        fun setSize(context: Context, index: Int) {
            CameraPreviewServiceProxy(context).setSize(index)
        }
    }
}
