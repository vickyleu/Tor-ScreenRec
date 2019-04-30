package dev.tornaco.torscreenrec.ui

import dev.nick.tiles.tile.Category
import dev.nick.tiles.tile.DashboardFragment
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.tiles.AudioSourceTile

/**
 * Created by Tornaco on 2017/7/27.
 * Licensed with Apache.
 */

class AudioSourceFragment : DashboardFragment() {
    override fun onCreateDashCategories(categories: MutableList<Category>) {
        super.onCreateDashCategories(categories)
        val category = Category()
        category.titleRes = R.string.title_audio_source
        category.addTile(AudioSourceTile(requireContext()))
        categories.add(category)
    }
}
