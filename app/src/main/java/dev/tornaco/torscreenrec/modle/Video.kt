package dev.tornaco.torscreenrec.modle

import lombok.AllArgsConstructor
import lombok.Setter
import lombok.ToString

@ToString
@AllArgsConstructor
data class Video(
        var id: Int = 0,
        var title: String? = null,
        var album: String? = null,
        var artist: String? = null,
        var displayName: String? = null,
        var mimeType: String? = null,
        var path: String? = null,
        var size: Long = 0,
        var duration: String? = null,
        var sizeDesc: String? = null
)