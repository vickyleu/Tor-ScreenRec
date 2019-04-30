package dev.tornaco.torscreenrec.control

import android.content.Context
import android.os.RemoteException
import android.widget.Toast

import org.newstand.logger.Logger

import dev.nick.library.BridgeManager
import dev.nick.library.IParam
import dev.nick.library.RecBridgeServiceProxy
import dev.nick.library.TokenAdapter
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

object RecRequestHandler {

    fun start(context: Context): Boolean {
        val isPlatformBridge = BridgeManager.getInstance().isInstalledInSystem(context)
        val settingsProvider = SettingsProvider.get()
        try {
            RecBridgeServiceProxy.from(context)
                    .start(IParam.builder()
                            .audioSource(settingsProvider!!.getInt(SettingsProvider.Key.AUDIO_SOURCE))
                            .frameRate(settingsProvider.getInt(SettingsProvider.Key.FAME_RATE))
                            .audioBitrate(settingsProvider.getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K))
                            .orientation(settingsProvider.getInt(SettingsProvider.Key.ORIENTATION))
                            .resolution(settingsProvider.getString(SettingsProvider.Key.RESOLUTION))
                            .stopOnScreenOff(settingsProvider.getBoolean(SettingsProvider.Key.SCREEN_OFF_STOP))
                            .useMediaProjection(!isPlatformBridge)
                            .stopOnShake(settingsProvider.getBoolean(SettingsProvider.Key.SHAKE_STOP))
                            .shutterSound(settingsProvider.getBoolean(SettingsProvider.Key.SHUTTER_SOUND))
                            .path(SettingsProvider.get()!!.createVideoFilePath())
                            .showNotification(true)
                            .showTouch(settingsProvider.getBoolean(SettingsProvider.Key.SHOW_TOUCH))
                            .build(),

                            object : TokenAdapter() {
                                @Throws(RemoteException::class)
                                override fun getDescription(): String? {
                                    return null // No need?
                                }
                            })
        } catch (e: RemoteException) {
            Toast.makeText(context, "start fail:" + Logger.getStackTraceString(e), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    fun stop(context: Context): Boolean {
        try {
            RecBridgeServiceProxy.from(context).stop()
        } catch (e: RemoteException) {
            Toast.makeText(context, "stop fail:" + Logger.getStackTraceString(e), Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }
}
