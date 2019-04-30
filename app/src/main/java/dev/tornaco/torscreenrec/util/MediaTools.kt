package dev.tornaco.torscreenrec.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider

import java.io.File

import dev.tornaco.torscreenrec.BuildConfig


object MediaTools {

    fun buildSharedIntent(context: Context, imageFile: File): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "video/mp4"
            sharingIntent.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", imageFile))
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, imageFile.name)
            val chooserIntent = Intent.createChooser(sharingIntent, null)
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            return chooserIntent
        } else {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "video/mp4"
            val uri = Uri.parse("file://" + imageFile.absolutePath)
            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, imageFile.name)
            return sharingIntent

        }
    }

    fun buildInstallIntent(context: Context, file: File): Intent {

        val open = Intent(Intent.ACTION_VIEW)
        val contentUri: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            open.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)

        } else {
            contentUri = Uri.parse("file://" + file.absolutePath)
        }
        open.setDataAndType(contentUri, "application/vnd.android.package-archive")
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return open
    }

    fun buildOpenIntent(context: Context, imageFile: File): Intent {

        val open = Intent(Intent.ACTION_VIEW)
        val contentUri: Uri

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            open.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", imageFile)

        } else {
            contentUri = Uri.parse("file://" + imageFile.absolutePath)
        }
        open.setDataAndType(contentUri, "video/mp4")
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return open
    }
}
