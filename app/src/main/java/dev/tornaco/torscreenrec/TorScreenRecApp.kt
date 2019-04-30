package dev.tornaco.torscreenrec

import android.app.Activity
import android.app.Application
import android.app.ProgressDialog
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.common.collect.Lists
import com.stericson.rootools.RootTools
import dev.nick.library.IWatcher
import dev.nick.library.RecBridgeServiceProxy
import dev.nick.library.WatcherAdapter
import dev.tornaco.torscreenrec.bridge.Installer
import dev.tornaco.torscreenrec.common.Collections
import dev.tornaco.torscreenrec.common.Consumer
import dev.tornaco.torscreenrec.control.FloatingControllerServiceProxy
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.util.ThreadUtil
import lombok.experimental.Delegate
import org.newstand.logger.Logger
import org.newstand.logger.Settings
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Tornaco on 2017/7/26.
 * Licensed with Apache.
 */

class TorScreenRecApp : Application() {

    @Delegate
    private var watcherProxy: WatcherProxy? = null

    @Delegate
    private var lifeCycleHandler: LifeCycleHandler? = null

    private var floatViewHandler: FloatViewHandler? = null

    override fun onCreate() {
        super.onCreate()
        app = this
        Logger.config(Settings.builder().tag("TorScreenRec").logLevel(Logger.LogLevel.ALL).build())
        SettingsProvider.init(applicationContext)
        watcherProxy = WatcherProxy()
        lifeCycleHandler = LifeCycleHandler()
        checkForUpdate()
    }


    // FIXME Impl is too ugly!!!!!!!!
    private fun checkForUpdate() {
        Handler().postDelayed({
            val prebuiltUpdateAvailable = Installer.checkForNewVersionFromPrebuilt(applicationContext)
            if (prebuiltUpdateAvailable)
                onPrebuiltUpdateAvailable(Installer.prebuiltVersionName())
        }, 3000)
    }

    val topActivity: Activity?
        get() {
            return lifeCycleHandler?.topActivity
        }

    fun unWatch(watcher: IWatcher) {
        watcherProxy?.unWatchInner(watcher)
    }

    fun watch(watcher: IWatcher) {
        watcherProxy?.watchInner(watcher)
    }

    private fun onPrebuiltUpdateAvailable(versionName: String) {
        if (topActivity == null) return
        val alertDialog = AlertDialog.Builder(topActivity!!)
                .setTitle(R.string.update_available)
                .setMessage(getString(R.string.update_available_message, versionName))
                .setPositiveButton(android.R.string.ok) { dialogInterface, i -> onRequestUpdate() }
                .setNegativeButton(android.R.string.cancel, null)
                .setCancelable(false)
                .create()
        alertDialog.show()
    }

    private fun onRequestUpdate() {
        val p = ProgressDialog(topActivity)
        p.isIndeterminate = true
        p.setMessage(getString(R.string.installing))
        p.setCancelable(false)
        p.show()
        Installer.unInstallAsync(applicationContext, object : Installer.Callback {
            override fun onSuccess() {
                Installer.installWithRootAsync(applicationContext, object : Installer.Callback {
                    override fun onSuccess() {
                        p.dismiss()
                        Snackbar.make(topActivity!!.getWindow().getDecorView(),
                                R.string.install_success, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.restart) { RootTools.restartAndroid() }.show()
                    }

                    override fun onFailure(throwable: Throwable, errTitle: String) {
                        p.dismiss()
                        Snackbar.make(topActivity!!.getWindow().getDecorView(), getString(R.string.install_fail, errTitle),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.report) { }.show()
                    }
                })
            }

            override fun onFailure(throwable: Throwable, errTitle: String) {
                Snackbar.make(topActivity!!.getWindow().getDecorView(),
                        R.string.uninstall_fail, Snackbar.LENGTH_INDEFINITE).show()
                p.dismiss()
            }
        })
    }

    @Synchronized
    private fun setupFloatView() {
        if (floatViewHandler == null) {
            floatViewHandler = FloatViewHandler()
            floatViewHandler!!.listen()
        }
    }

    private inner class FloatViewHandler {

        internal fun listen() {
            if (SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.FLOAT_WINDOW)) {
                FloatingControllerServiceProxy(applicationContext)
                        .start(applicationContext)
            }

            SettingsProvider.get()!!.addObserver { observable, o ->
                if (o === SettingsProvider.Key.FLOAT_WINDOW) {
                    val show = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.FLOAT_WINDOW)
                    if (show) {
                        FloatingControllerServiceProxy(applicationContext).start(applicationContext)
                    } else {
                        FloatingControllerServiceProxy(applicationContext).stop(applicationContext)
                    }
                }
            }
        }
    }

    private inner class LifeCycleHandler internal constructor() {

        internal var topActivity: Activity? = null

        init {

            registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
                override fun onActivityPaused(activity: Activity?) {
                }

                override fun onActivityResumed(activity: Activity?) {
                    topActivity = activity
                    setupFloatView()
                }

                override fun onActivityStarted(activity: Activity?) {
                }

                override fun onActivityDestroyed(activity: Activity?) {
                    SettingsProvider.get()!!.putBoolean(SettingsProvider.Key.FIRST_RUN, false)
                }

                override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
                }

                override fun onActivityStopped(activity: Activity?) {
                }

                override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
                }

            })
        }
    }

    private inner class WatcherProxy() : WatcherAdapter() {

        internal val watchers: MutableList<IWatcher> = Lists.newArrayList()

        internal var ready: AtomicBoolean? = null

        var recording = AtomicBoolean(false)
            internal set

        init {
            try {
                RecBridgeServiceProxy.from(applicationContext)
                        .watch(this)
                ready = AtomicBoolean(true)
            } catch (e: RemoteException) {
                Logger.e(e, "Fail watch")
                ready = AtomicBoolean(false)
            }

        }

        fun waitForReady() {
            while (ready == null || !ready!!.get()) {
                ThreadUtil.sleep(100)
            }
        }

        fun watchInner(watcher: IWatcher) {
            synchronized(watchers) {
                watchers.remove(watcher)
                watchers.add(watcher)
            }

            // Send sticky event.
            if (recording.get()) {
                try {
                    watcher.onStart()
                } catch (e: RemoteException) {
                    Logger.e(e, "WatcherProxy: Error call onStart")
                }

            } else {
                try {
                    watcher.onStop()
                } catch (e: RemoteException) {
                    Logger.e(e, "WatcherProxy: Error call onStop")
                }

            }
        }

        fun unWatchInner(watcher: IWatcher) {
            synchronized(watchers) {
                watchers.remove(watcher)
            }
        }

        @Throws(RemoteException::class)
        override fun onStart() {
            super.onStart()

            recording.set(true)

            synchronized(watchers) {
                Collections.consumeRemaining(watchers, object : Consumer<IWatcher> {
                    override fun accept(t: IWatcher) {
                        ThreadUtil.mainThreadHandler.post {
                            try {
                                t.onStart()
                            } catch (e: RemoteException) {
                                Logger.e(e, "WatcherProxy: Error call onStart")
                            }
                        }
                    }
                })
            }
        }

        @Throws(RemoteException::class)
        override fun onStop() {
            super.onStop()

            recording.set(false)

            synchronized(watchers) {
                Collections.consumeRemaining(watchers, object : Consumer<IWatcher> {
                    override fun accept(t: IWatcher) {
                        ThreadUtil.mainThreadHandler.post {
                            try {
                                t.onStop()
                            } catch (e: RemoteException) {
                                Logger.e(e, "WatcherProxy: Error call onStop")
                            }
                        }
                    }
                })
            }
        }

        @Throws(RemoteException::class)
        override fun onElapsedTimeChange(formatedTime: String?) {
            super.onElapsedTimeChange(formatedTime)
            Collections.consumeRemaining(watchers, object : Consumer<IWatcher> {
                override fun accept(t: IWatcher) {
                    ThreadUtil.mainThreadHandler.post {
                        try {
                            t.onElapsedTimeChange(formatedTime)
                        } catch (e: RemoteException) {
                            Logger.e(e, "WatcherProxy: Error call onElapsedTimeChange")
                        }
                    }
                }
            })
        }
    }

    companion object {
        var app: TorScreenRecApp? = null
            private set
    }

}
