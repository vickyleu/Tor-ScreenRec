package dev.tornaco.torscreenrec.bridge

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.net.Uri
import com.google.common.io.Files
import com.stericson.rootools.RootTools
import dev.nick.library.BridgeManager
import dev.tornaco.torscreenrec.util.MediaTools
import dev.tornaco.torscreenrec.util.ThreadUtil
import org.newstand.logger.Logger
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Created by Tornaco on 2017/7/25.
 * Licensed with Apache.
 */

object Installer {

    val BRIDGE_PACKAGE_NAME = "dev.nick.systemrecapi"

    private val SRC_PATH_PLATFORM = "app-release-platform.apk"
    private val SRC_PATH_TORNACO = "app-release-platform.apk"
    private val TMP_APK_NAME = "tmp.apk"
    private val DEST_PATH = "/system/app/RecBridge.apk"
    private val DEST_PATH_V2 = "/system/app/RecBridge/RecBridge.apk"

    interface Callback {
        fun onSuccess()

        fun onFailure(throwable: Throwable, errTitle: String)
    }

    fun prebuiltVersionName(): String {
        return PrebuiltConfig.VERSION_NAME
    }

    fun checkForNewVersionFromPrebuilt(context: Context): Boolean {
        return BridgeManager.getInstance().isInstalled(context) && PrebuiltConfig.VERSION_CODE > BridgeManager.getInstance().getVersionCode(context)
    }

    fun installWithRootAsync(context: Context, call: Callback) {
        Thread(Runnable { installWithRoot(context, call) }).start()
    }

    fun installWithIntentAsync(context: Context, call: Callback) {
        Thread(Runnable { installWithIntent(context, call) }).start()
    }

    fun unInstallAsync(context: Context, call: Callback) {
        ThreadUtil.newThread(Runnable {
            val isPlatform = BridgeManager.getInstance().isInstalledInSystem(context)
            if (isPlatform) {
                unInstallWithRoot(call)
            } else {
                unInstallWithIntent(context)
                call.onSuccess()
            }
        }).start()
    }

    private fun unInstallWithIntent(context: Context) {
        val packageURI = Uri.parse("package:$BRIDGE_PACKAGE_NAME")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI)
        uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(uninstallIntent)
    }

    private fun installWithRoot(context: Context, callback: Callback) {
        val from = extractFromAssets(context, SRC_PATH_PLATFORM)
        if (from == null) {
            callback.onFailure(Throwable(), "Copy to tmp fail")
            return
        }
        installWithRoot(context, from, DEST_PATH_V2, callback)
    }

    private fun installWithIntent(context: Context, callback: Callback) {
        val from = extractFromAssets(context, SRC_PATH_TORNACO)
        if (from == null) {
            callback.onFailure(Throwable(), "Copy to tmp fail")
            return
        }
        installWithIntent(context, from, callback)
    }

    private fun installWithIntent(context: Context, from: String, callback: Callback) {
        Logger.d("installWithIntent: %s", from)
        context.startActivity(MediaTools.buildInstallIntent(context, File(from)))
        callback.onSuccess()
    }

    private fun extractFromAssets(context: Context, name: String): String? {
        val tmpPath = Files.createTempDir().path + File.separator + TMP_APK_NAME
        try {
            copy(context, name, tmpPath)
        } catch (e: IOException) {
            Logger.e(e, "Copy tmp file fail")
            return null
        }

        return tmpPath
    }

    private fun installWithRoot(context: Context, from: String, to: String, callback: Callback) {

        if (!RootTools.isRootAvailable) {
            callback.onFailure(Throwable(), "Root not available")
            return
        }

        // Try uninstall old version.
        unInstallWithRoot(DEST_PATH, object : Callback {
            override fun onSuccess() {

            }

            override fun onFailure(throwable: Throwable, errTitle: String) {

            }
        })

        // Create dir.
        if (!RootTools.mkdir(File(DEST_PATH_V2).parent, true, 755)) {
            callback.onFailure(Throwable(), "Fail mkdir in system")
            return
        }

        if (!RootTools.copyFile(from, to, true, true)) {
            callback.onFailure(Throwable(), "Fail copy to system")
            return
        }
        callback.onSuccess()
    }

    private fun unInstallWithRoot(callback: Callback) {
        unInstallWithRoot(DEST_PATH, object : Callback {
            override fun onSuccess() {

            }

            override fun onFailure(throwable: Throwable, errTitle: String) {

            }
        })
        unInstallWithRoot(DEST_PATH_V2, callback)
    }

    private fun unInstallWithRoot(path: String, callback: Callback) {
        val ok = RootTools.deleteFileOrDirectory(path, true)
        if (ok) {
            callback.onSuccess()
        } else {
            callback.onFailure(Throwable(), "Fail delete file")
        }
    }

    private fun openAssets(context: Context): AssetManager {
        return context.assets
    }

    @Throws(IOException::class)
    private fun openInput(context: Context, path: String): InputStream {
        return openAssets(context).open(path)
    }

    @Throws(IOException::class)
    private fun copy(context: Context, from: String, to: String) {
        Files.createParentDirs(File(to))
        if (Files.asByteSink(File(to)).writeFrom(openInput(context, from)) <= 0) {
            throw IOException("Copy assets file fail")
        }
    }
}
