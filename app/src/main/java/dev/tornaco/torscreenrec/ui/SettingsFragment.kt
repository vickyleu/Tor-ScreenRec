package dev.tornaco.torscreenrec.ui

import android.os.Bundle
import dev.nick.tiles.tile.Category
import dev.nick.tiles.tile.DashboardFragment
import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.ui.tiles.*

/**
 * Created by Tornaco on 2017/7/28.
 * Licensed with Apache.
 */

class SettingsFragment : DashboardFragment() {

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity!!.setTitle(R.string.title_more_settings)
    }

    override fun onCreateDashCategories(categories: MutableList<Category>) {
        super.onCreateDashCategories(categories)

        val video = Category()
        video.titleRes = R.string.category_video
        video.addTile(VideoResTile(context!!))
        video.addTile(OrientationTile(context!!))
        video.addTile(FrameRateTile(context!!))

        val audio = Category()
        audio.titleRes = R.string.category_audio
        audio.addTile(AudioSourceTile(requireContext()))
        audio.addTile(AudioBitRateTile(context!!))

        val access = Category()
        access.titleRes = R.string.category_accessibility
        access.addTile(ShowTouchTile(context!!))
        access.addTile(SoundEffectTile(context!!))
        access.addTile(StopWhenScreenOffTile(context!!))
        access.addTile(ShakeTile(context!!))

        val camera = Category()
        camera.titleRes = R.string.summary_camera
        camera.addTile(WithCameraTile(context!!))
        camera.addTile(PreviewSizeDropdownTile(context!!))
        camera.addTile(SwitchCameraTile(context!!))

        val floatView = Category()
        floatView.titleRes = R.string.title_float_window
        floatView.addTile(FlowViewTile(requireContext()))
        floatView.addTile(FloatControlAlphaTile(context!!))
        floatView.addTile(FloatControlThemeTile(context!!))

        val storage = Category()
        storage.titleRes = R.string.category_storage
        storage.addTile(StorageTile(activity!!))

        categories.add(video)
        categories.add(audio)
        categories.add(camera)
        categories.add(access)
        categories.add(floatView)
        categories.add(storage)

    }
}
