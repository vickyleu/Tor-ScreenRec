package dev.tornaco.torscreenrec.pref

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Camera
import android.os.Environment

import java.io.File
import java.util.Observable

import dev.nick.library.AudioSource
import dev.nick.library.Orientations
import dev.nick.library.ValidResolutions
import dev.tornaco.torscreenrec.camera.PreviewSize
import dev.tornaco.torscreenrec.common.DateUtils
import dev.tornaco.torscreenrec.control.FloatControlTheme
import lombok.Getter

/**
 * Created by Tornaco on 2017/7/25.
 * Licensed with Apache.
 */
class SettingsProvider private constructor(context: Context) : Observable() {

    @Getter
    val pref: SharedPreferences

    enum class Key private constructor(defValue: Any) {
        USR_NAME("Fake.Name"),
        PAID(false),
        FIRST_RUN(true),
        VIDEO_ROOT_PATH(defaultVideoRootPath),
        AUDIO_SOURCE(AudioSource.NOOP),
        WITH_AUDIO(false),
        SHUTTER_SOUND(false),
        SHAKE_STOP(false),
        VOLUME_STOP(false),
        SCREEN_OFF_STOP(false),
        SHOW_TOUCH(false),
        FLOAT_WINDOW(false),
        FLOAT_WINDOW_ALPHA(50),
        FLOAT_WINDOW_THEME(FloatControlTheme.DefaultDark.name),
        FAME_RATE(30),
        AUDIO_BITRATE_RATE_K(64), // *1024
        RESOLUTION(ValidResolutions.DESC[ValidResolutions.INDEX_MASK_AUTO]),
        ORIENTATION(Orientations.AUTO),
        USER_PROJECTION(false),
        CAMERA(false),
        CAMERA_SIZE(PreviewSize.SMALL),
        PREFERRED_CAMERA(CAMERA_FACING_FRONT);

        @Getter
        var defValue: Any
            internal set

        init {
            this.defValue = defValue
        }
    }

    init {
        this.pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun toPrefKey(key: Key): String {
        return key.name.toLowerCase()
    }

    fun getBoolean(key: Key): Boolean {
        return pref.getBoolean(toPrefKey(key), key.defValue as Boolean)
    }

    fun putBoolean(key: Key, value: Boolean) {
        pref.edit().putBoolean(toPrefKey(key), value).apply()
        setChanged()
        notifyObservers(key)
    }

    fun getInt(key: Key): Int {
        return pref.getInt(toPrefKey(key), key.defValue as Int)
    }

    fun putInt(key: Key, value: Int) {
        pref.edit().putInt(toPrefKey(key), value).apply()
        setChanged()
        notifyObservers(key)
    }

    fun getString(key: Key): String? {
        return pref.getString(toPrefKey(key), key.defValue as String)
    }

    fun putString(key: Key, value: String) {
        pref.edit().putString(toPrefKey(key), value).apply()
        setChanged()
        notifyObservers(key)
    }

    fun createVideoFilePath(): String {
        return (getString(Key.VIDEO_ROOT_PATH)
                + File.separator
                + DateUtils.formatForFileName(System.currentTimeMillis()) + ".mp4")
    }

    companion object {

        private var sMe: SettingsProvider? = null

        private val PREF_NAME = "rec_app_settings"

        val CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT
        val CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK

        fun get(): SettingsProvider? {
            return sMe
        }

        fun init(context: Context) {
            sMe = SettingsProvider(context)
        }

        val defaultVideoRootPath: String
            get() = (Environment.getExternalStorageDirectory().path
                    + File.separator + "ScreenRecorder")

        val REQUEST_CODE_FILE_PICKER = 0x100
    }
}
