package dev.tornaco.torscreenrec.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment

import com.nononsenseapps.filepicker.Utils

import org.newstand.logger.Logger

import java.io.File

import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.pref.SettingsProvider

/**
 * Created by Tornaco on 2017/7/27.
 * Licensed with Apache.
 */

class ContainerHostActivity : TransitionSafeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_with_container_with_appbar_template)
        setupToolbar()
        showHomeAsUp()
        replaceV4(R.id.container, onCreateFragment(), null, false)
    }

    internal fun onCreateFragment(): Fragment? {
        val intent = intent
        val clz = intent.getStringExtra(EXTRA_FRAGMENT_CLZ)
        Logger.i("Extra clz:%s", clz)
        if (AudioSourceFragment::class.java.name == clz) {
            return AudioSourceFragment()
        }
        if (RecordingBrowserFragment::class.java.name == clz) {
            return RecordingBrowserFragment()
        }
        if (SettingsFragment::class.java.name == clz) {
            return SettingsFragment()
        }
        if (BridgeManagerFragment::class.java.name == clz) {
            return BridgeManagerFragment()
        }
        if (ShopFragment::class.java.name == clz) {
            return ShopFragment()
        }
        if (PayListBrowserFragment::class.java.name == clz) {
            return PayListBrowserFragment()
        }
        return if (AboutFragment::class.java.name == clz) {
            AboutFragment()
        } else null
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SettingsProvider.REQUEST_CODE_FILE_PICKER && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            val files = Utils.getSelectedFilesFromResult(data!!)
            val file = Utils.getFileForUri(files[0])
            // Do something with the result...
            onStorageDirPick(file)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onStorageDirPick(dir: File) {
        Logger.d("onStorageDirPick:$dir")
        SettingsProvider.get()!!.putString(SettingsProvider.Key.VIDEO_ROOT_PATH, dir.path)
    }

    companion object {

        val EXTRA_FRAGMENT_CLZ = "extra.fr.clz"

        fun getIntent(context: Context, clz: Class<out Fragment>): Intent {
            val i = Intent(context, ContainerHostActivity::class.java)
            i.putExtra(ContainerHostActivity.EXTRA_FRAGMENT_CLZ, clz.name)
            return i
        }
    }
}
