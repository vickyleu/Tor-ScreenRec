package dev.tornaco.torscreenrec.control

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.ui.widget.FloatView
import ezy.assist.compat.SettingsCompat

/**
 * Created by Nick on 2017/6/28 14:43
 */

class FloatingControlService : Service(), FloatingController {

    private var floatView: FloatView? = null

    override val isShowing: Boolean
        get() = floatView!!.isAttachedToWindow

    override fun onBind(intent: Intent): IBinder? {
        return Stub()
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "156"
            val name = "Channel One"
            val notificationChannel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.setShowBadge(true)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel)

            val notification = Notification.Builder(this, channelId)
                    .build()
            notification.flags = Notification.FLAG_NO_CLEAR
            startForeground(channelId.toInt(), notification)
        }
        floatView = FloatView(application as TorScreenRecApp)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as TorScreenRecApp

        if (SettingsCompat.canDrawOverlays(app.topActivity) || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                show()
                return START_STICKY
            } catch (e: Throwable) {
                Toast.makeText(applicationContext, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
            }

        }

        try {
            SettingsCompat.manageDrawOverlays(app.topActivity)
        } catch (e: Throwable) {
            Toast.makeText(applicationContext, Log.getStackTraceString(e), Toast.LENGTH_LONG).show()
        }

        SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.FLOAT_WINDOW, false)

        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        hide()
    }

    override fun show() {
        floatView!!.attach()
    }

    override fun hide() {
        floatView!!.detach()
    }

    private inner class Stub : Binder(), FloatingController {

        override val isShowing: Boolean
            get() = this@FloatingControlService.isShowing

        override fun show() {
            this@FloatingControlService.show()
        }

        override fun hide() {
            this@FloatingControlService.hide()
        }
    }
}
