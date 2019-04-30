package dev.tornaco.torscreenrec.ui

import android.app.ProgressDialog
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AlertDialog
import dev.nick.library.BridgeManager
import dev.nick.library.WatcherAdapter
import dev.nick.tiles.tile.Category
import dev.nick.tiles.tile.DashboardFragment
import dev.tornaco.torscreenrec.DrawerNavigatorActivity
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.common.SharedExecutor
import dev.tornaco.torscreenrec.control.RecRequestHandler
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.ui.tiles.*
import dev.tornaco.torscreenrec.ui.widget.RecordingButton
import org.newstand.logger.Logger
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Tornaco on 2017/7/26.
 * Licensed with Apache.
 */

class ScreenCastFragment : DashboardFragment() {
    var rootView: View? = null
        private set
    var isRecording: AtomicBoolean? = null
        private set
    var settingsProvider: SettingsProvider? = null
        private set
    var appContext: Context? = null
    private var floatingActionButton: RecordingButton? = null

    override val layoutId: Int
        get() = R.layout.layout_screen_cast

    private val watcher = object : WatcherAdapter() {
        @Throws(RemoteException::class)
        override fun onStart() {
            if (isRecording == null) {
                isRecording = AtomicBoolean(true)
            } else {
                isRecording!!.set(true)
            }
            refreshFabState()
            Logger.i("onStart")
        }

        @Throws(RemoteException::class)
        override fun onStop() {
            if (isRecording == null) {
                isRecording = AtomicBoolean(false)
            } else {
                isRecording!!.set(false)
            }
            refreshFabState()
            Logger.i("onStop")
        }

        @Throws(RemoteException::class)
        override fun onElapsedTimeChange(s: String?) {

        }
    }

    protected val isDead: Boolean
        get() = isDetached || !isAdded || activity!!.isDestroyed

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = super.onCreateView(inflater, container, savedInstanceState)
        setupView()
        return rootView
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        appContext = context!!.applicationContext
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupStatus()
    }

    private fun setupStatus() {
        settingsProvider = SettingsProvider.get()
        val app = activity!!.application as TorScreenRecApp
        app.watch(watcher)
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = activity!!.application as TorScreenRecApp
        app.unWatch(watcher)
    }

    override fun onCreateDashCategories(categories: MutableList<Category>) {
        super.onCreateDashCategories(categories)
        val quickFunc = Category()
        quickFunc.titleRes = R.string.quick_function
        quickFunc.addTile(RecordingBrowserTile(requireContext()))

        val ad = Category()
        if (!SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.PAID)) {
            ad.titleRes = R.string.title_ad_area
            ad.addTile(AdTile(requireContext()))
        }

        val quickSettings = Category()
        quickSettings.titleRes = R.string.quick_settings
        quickSettings.addTile(AudioSourceTile(requireContext()))
        quickSettings.addTile(FlowViewTile(requireContext()))
        quickSettings.addTile(WithCameraTile(requireContext()))

        val moreSettings = Category()
        moreSettings.titleRes = R.string.category_others
        moreSettings.addTile(MoreSettingsTile(requireContext()))

        categories.add(quickFunc)
        categories.add(ad)
        categories.add(quickSettings)
        categories.add(moreSettings)
    }

    protected fun <T : View> findView(@IdRes idRes: Int): T {
        return rootView!!.findViewById<View>(idRes) as T
    }

    protected fun <T : View> findView(root: View, @IdRes idRes: Int): T {
        return root.findViewById<View>(idRes) as T
    }

    override fun onResume() {
        super.onResume()
        activity!!.runOnUiThread { setupView() }
    }

    protected fun setupView() {

        val introView = findView<View>(android.R.id.text1)
        val firstRun = SettingsProvider.get()!!.getBoolean(SettingsProvider.Key.FIRST_RUN)
        if (firstRun) {
            introView.visibility = View.VISIBLE
        } else {
            introView.visibility = View.GONE
        }

        val cardView = findView<View>(R.id.card)

        cardView.setOnClickListener {
            if (introView.visibility == View.VISIBLE) {
                introView.visibility = View.GONE
            } else {
                introView.visibility = View.VISIBLE
            }
        }

        // Read status.
        val bridgeManager = BridgeManager.getInstance()
        val installed = bridgeManager.isInstalled(context)

        val statusView = findView<ImageView>(R.id.icon1)
        //        statusView.setColorFilter(ContextCompat.getColor(getContext(),
        //                installed ? R.color.white : R.color.red));
        statusView.setImageResource(if (installed)
            R.drawable.ic_check_circle_black_24dp
        else
            R.drawable.ic_remove_circle_black_24dp)

        val drawerNavigatorActivity = activity as DrawerNavigatorActivity?
        floatingActionButton = drawerNavigatorActivity!!.floatingActionButton

        if (installed) {
            floatingActionButton!!.show()
        } else {
            floatingActionButton!!.hide()
        }

        floatingActionButton!!.setOnClickListener {
            if (isRecording != null && isRecording!!.get()) {
                onRequestStop()
            } else {
                onRequestStart()
            }
        }

        findView<View>(R.id.button).setOnClickListener { startActivity(ContainerHostActivity.getIntent(requireContext(), BridgeManagerFragment::class.java)) }


        // Retrieve version.
        if (installed) {
            SharedExecutor.execute(Runnable {
                var name: String? = BridgeManager.getInstance().getVersionName(context)
                if (name == null)
                    name = appContext!!.getString(R.string.version_name_unknown)

                val isPlatform = BridgeManager.getInstance().isInstalledInSystem(context)

                val versionNameMessage: String = if (isPlatform && appContext != null)
                    appContext!!.getString(R.string.installed_version_name, name!! + "-Root")
                else
                    appContext!!.getString(R.string.installed_version_name, name)

                if (activity == null) return@Runnable

                activity!!.runOnUiThread { updateCardStatus(versionNameMessage) }
            })
        } else {
            updateCardStatus(appContext!!.getString(R.string.bridge_not_installed))
        }
    }


    private fun updateCardStatus(title: String) {
        val textView = findView<TextView>(R.id.bridge_status)
        textView.text = title
    }

    private fun onRequestStart() { //FIXME
        RecRequestHandler.start(activity!!.applicationContext)
    }

    private fun onRequestStop() {
        RecRequestHandler.stop(activity!!.applicationContext) //FIXME
    }

    private fun refreshFabState() {
        activity!!.runOnUiThread {
            if (isRecording!!.get()) {
                floatingActionButton!!.setImageResource(R.drawable.stop)
                floatingActionButton!!.onRecording()
            } else {
                floatingActionButton!!.setImageResource(R.drawable.record)
                floatingActionButton!!.onStopRecording()
            }
        }
    }

    private fun onRequestInstall(install: Boolean, view: View) {
        val p = ProgressDialog(activity)
        p.isIndeterminate = true
        if (install) {
        } else {

        }
    }

    protected fun onRemoteException(e: RemoteException) {
        Logger.e(e, "onRemoteException")
        AlertDialog.Builder(activity!!)
                .setTitle(R.string.remote_err)
                .setMessage(e.localizedMessage)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show()
    }

    companion object {

        fun create(): ScreenCastFragment {
            return ScreenCastFragment()
        }
    }
}
