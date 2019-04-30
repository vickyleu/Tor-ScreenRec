package dev.tornaco.torscreenrec.ui.tiles

import android.app.ProgressDialog
import android.content.Context
import com.google.android.material.snackbar.Snackbar
import android.view.View

import com.stericson.rootools.RootTools

import dev.nick.tiles.tile.QuickTile
import dev.nick.tiles.tile.QuickTileView
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.bridge.Installer

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class RootInstallTile(context: Context) : QuickTile(context) {

    init {

        this.titleRes = R.string.title_root_install
        this.summaryRes = R.string.summary_root_install
        this.iconRes = R.mipmap.avatar_root

        this.tileView = object : QuickTileView(context, this@RootInstallTile) {


            override fun useStaticTintColor(): Boolean {
                return false
            }

            override fun onClick(v: View) {
                super.onClick(v)
                val p = ProgressDialog(context)
                p.isIndeterminate = true
                p.setMessage(context.getString(R.string.installing))
                p.setCancelable(false)
                p.show()

                Installer.installWithRootAsync(getContext(), object : Installer.Callback {
                    override fun onSuccess() {
                        p.dismiss()
                        Snackbar.make(tileView!!, R.string.install_success, Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.restart) { RootTools.restartAndroid() }.show()
                    }

                    override fun onFailure(throwable: Throwable, errTitle: String) {
                        p.dismiss()
                        Snackbar.make(tileView!!, context.getString(R.string.install_fail, errTitle),
                                Snackbar.LENGTH_LONG)
                                .setAction(R.string.report) { }.show()
                    }
                })
            }
        }
    }
}
