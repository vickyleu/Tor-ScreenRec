package dev.tornaco.torscreenrec.loader

import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.format.DateUtils

import java.io.File
import java.util.ArrayList

import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.modle.Video
import dev.tornaco.torscreenrec.pref.SettingsProvider
import dev.tornaco.torscreenrec.util.MiscUtils

class VideoProvider(private val context: Context?) {

    val list: List<Video>
        get() {
            val list = ArrayList<Video>()
            if (context != null) {
                val cursor = context.contentResolver.query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.Media.DATE_MODIFIED + " desc")
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(cursor
                                .getColumnIndexOrThrow(MediaStore.Video.Media._ID))
                        val title = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.TITLE))
                        val album = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM))
                        val artist = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST))
                        val displayName = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
                        val mimeType = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE))
                        val path = cursor
                                .getString(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
                        val file = File(path)
                        if (!file.exists()) continue
                        if (file.parentFile.path != SettingsProvider.get()!!.getString(
                                        SettingsProvider.Key.VIDEO_ROOT_PATH
                                )) {
                            continue
                        }
                        val duration = cursor
                                .getInt(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)).toLong()
                        val size = cursor
                                .getLong(cursor
                                        .getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                        val video = Video(id, title, album, artist, displayName, mimeType, path, size, formatDuration(context, duration),
                                context.getString(R.string.file_size,
                                        MiscUtils.formatFileSize(size)))
                        list.add(video)
                    }
                    cursor.close()
                }
            }
            return list
        }

    private fun formatDuration(c: Context, time: Long): String {
        return c.getString(R.string.video_length,
                DateUtils.formatElapsedTime(time / 1000))
    }

}
