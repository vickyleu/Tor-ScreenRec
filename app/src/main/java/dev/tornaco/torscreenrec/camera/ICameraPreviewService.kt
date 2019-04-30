package dev.tornaco.torscreenrec.camera

interface ICameraPreviewService {

    val isShowing: Boolean
    fun show(size: Int)

    fun hide()

    fun setSize(sizeIndex: Int)
}
