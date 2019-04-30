package dev.tornaco.torscreenrec.control

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import dev.nick.library.ServiceProxy


/**
 * Created by Nick on 2017/6/28 14:52
 */

class FloatingControllerServiceProxy(context: Context) : ServiceProxy(context, Intent(context, FloatingControlService::class.java)), FloatingController {

    private var controller: FloatingController? = null

    override val isShowing: Boolean
        get() = false

    fun start(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, FloatingControlService::class.java))
        } else {
            context.startService(Intent(context, FloatingControlService::class.java))
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, FloatingControlService::class.java))
    }

    override fun show() {
        setTask(object : ServiceProxy.ProxyTask() {
            @Throws(RemoteException::class)
            override fun run() {
                controller!!.show()
            }
        })
    }

    override fun hide() {
        setTask(object : ServiceProxy.ProxyTask() {
            @Throws(RemoteException::class)
            override fun run() {
                controller!!.hide()
            }
        })
    }

    override fun onConnected(binder: IBinder) {
        controller = binder as FloatingController
    }
}
