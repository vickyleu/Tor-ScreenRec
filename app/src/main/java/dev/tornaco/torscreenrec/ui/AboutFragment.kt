package dev.tornaco.torscreenrec.ui

import android.os.Bundle

import dev.nick.tiles.tile.Category
import dev.nick.tiles.tile.DashboardFragment
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.tiles.AuthorIntroTile
import dev.tornaco.torscreenrec.ui.tiles.EmailTile
import dev.tornaco.torscreenrec.ui.tiles.OpenSourceTile
import dev.tornaco.torscreenrec.ui.tiles.ReleaseTile
import dev.tornaco.torscreenrec.ui.tiles.VersionTile

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class AboutFragment : DashboardFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setTitle(R.string.title_about)
    }

    override fun onCreateDashCategories(categories: MutableList<Category>) {
        super.onCreateDashCategories(categories)

        val me = Category()
        me.titleRes = R.string.me
        me.addTile(AuthorIntroTile(requireContext()))
        me.addTile(EmailTile(requireContext()))

        val app = Category()
        app.titleRes = R.string.app
        app.addTile(VersionTile(requireContext()))
        app.addTile(OpenSourceTile(requireContext()))
        app.addTile(ReleaseTile(requireContext()))

        categories.add(me)
        categories.add(app)
    }
}
