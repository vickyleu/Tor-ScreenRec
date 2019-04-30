package dev.tornaco.torscreenrec.ui

import android.os.Bundle

import dev.nick.library.BridgeManager
import dev.nick.tiles.tile.Category
import dev.nick.tiles.tile.DashboardFragment
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.tiles.MagiskInstallTile
import dev.tornaco.torscreenrec.ui.tiles.NormalInstallTile
import dev.tornaco.torscreenrec.ui.tiles.RootInstallTile
import dev.tornaco.torscreenrec.ui.tiles.UnInstallTile
import dev.tornaco.torscreenrec.ui.tiles.XposedInstallTile

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class BridgeManagerFragment : DashboardFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setTitle(R.string.title_bridge_manager)
    }

    override fun onCreateDashCategories(categories: MutableList<Category>) {
        super.onCreateDashCategories(categories)

        val unInstall = Category()
        unInstall.titleRes = R.string.title_uninstall
        unInstall.addTile(UnInstallTile(requireActivity()))

        val category = Category()
        category.titleRes = R.string.title_install
        category.addTile(NormalInstallTile(requireActivity()))
        val categoryRoot = Category()
        categoryRoot.titleRes = R.string.title_install
        categoryRoot.addTile(RootInstallTile(requireActivity()))
        val categoryXposed = Category()
        categoryXposed.addTile(XposedInstallTile(requireContext()))
        val categoryMagisk = Category()
        categoryMagisk.addTile(MagiskInstallTile(requireContext()))

        if (BridgeManager.getInstance().isInstalled(requireContext())) categories.add(unInstall)
        categories.add(categoryRoot)
        categories.add(categoryXposed)
        categories.add(categoryMagisk)
    }
}
