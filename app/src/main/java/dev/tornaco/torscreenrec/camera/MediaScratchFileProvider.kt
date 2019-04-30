/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.tornaco.torscreenrec.camera

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.database.MatrixCursor.RowBuilder
import android.net.Uri
import android.provider.OpenableColumns
import androidx.collection.SimpleArrayMap
import android.text.TextUtils
import android.util.Log

import java.io.File

import dev.tornaco.torscreenrec.TorScreenRecApp
import dev.tornaco.torscreenrec.camera.FileProvider.Companion.isValidFileId

/**
 * A very simple content provider that can serve media files from our cache directory.
 */
class MediaScratchFileProvider : FileProvider() {

    internal override fun getFile(path: String, extension: String?): File {
        return getFileWithExtension(path, extension)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        if (projection != null && projection.size > 0 &&
                TextUtils.equals(projection[0], OpenableColumns.DISPLAY_NAME) &&
                isMediaScratchSpaceUri(uri)) {
            // Retrieve the display name associated with a temp file. This is used by the Contacts
            // ImportVCardActivity to retrieve the name of the contact(s) being imported.
            val displayName: String?
            synchronized(sUriToDisplayNameMap) {
                displayName = sUriToDisplayNameMap.get(uri)
            }
            if (!TextUtils.isEmpty(displayName)) {
                val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME))
                val row = cursor.newRow()
                row.add(displayName)
                return cursor
            }
        }
        return null
    }

    companion object {
        val AUTHORITY = "com.android.messaging.datamodel.MediaScratchFileProvider"
        private val TAG = "ScratchFilePro"
        private val sUriToDisplayNameMap = SimpleArrayMap<Uri, String>()
        private val MEDIA_SCRATCH_SPACE_DIR = "mediascratchspace"

        fun isMediaScratchSpaceUri(uri: Uri?): Boolean {
            if (uri == null) {
                return false
            }

            val segments = uri.pathSegments
            return TextUtils.equals(uri.scheme, ContentResolver.SCHEME_CONTENT) &&
                    TextUtils.equals(uri.authority, AUTHORITY) &&
                    segments.size == 1 && isValidFileId(fileId = segments[0])
        }

        /**
         * Returns a uri that can be used to access a raw mms file.
         *
         * @return the URI for an raw mms file
         */
        fun buildMediaScratchSpaceUri(extension: String): Uri {
            val uri = buildFileUri(AUTHORITY, extension)
            val file = getFileWithExtension(uri.path, extension)
            if (!ensureFileExists(file)) {
                Log.e(TAG, "Failed to create temp file " + file.absolutePath)
            }
            return uri
        }

        fun getFileFromUri(uri: Uri): File {
            return getFileWithExtension(uri.path, getExtensionFromUri(uri))
        }

        val uriBuilder: Uri.Builder
            get() = Uri.Builder().authority(AUTHORITY).scheme(ContentResolver.SCHEME_CONTENT)

        private fun getFileWithExtension(path: String?, extension: String?): File {
            val context = TorScreenRecApp.app!!.getApplicationContext()
            return File(getDirectory(context),
                    if (TextUtils.isEmpty(extension)) path else "$path.$extension")
        }

        private fun getDirectory(context: Context): File {
            return File(context.cacheDir, MEDIA_SCRATCH_SPACE_DIR)
        }

        fun addUriToDisplayNameEntry(scratchFileUri: Uri,
                                     displayName: String) {
            if (TextUtils.isEmpty(displayName)) {
                return
            }
            synchronized(sUriToDisplayNameMap) {
                sUriToDisplayNameMap.put(scratchFileUri, displayName)
            }
        }
    }
}
