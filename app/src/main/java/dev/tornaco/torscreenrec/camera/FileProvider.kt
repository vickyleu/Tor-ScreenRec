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

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.TextUtils

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Random

/**
 * A very simple content provider that can serve files.
 */
abstract class FileProvider : ContentProvider() {

    internal abstract fun getFile(path: String, extension: String?): File

    override fun onCreate(): Boolean {
        return true
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val fileId = uri.path
        if (isValidFileId(fileId!!)) {
            val file = getFile(fileId, getExtensionFromUri(uri))
            return if (file.delete()) 1 else 0
        }
        return 0
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, fileMode: String): ParcelFileDescriptor? {
        val fileId = uri.path
        if (isValidFileId(fileId!!)) {
            val file = getFile(fileId, getExtensionFromUri(uri))
            val mode = if (TextUtils.equals(fileMode, "r"))
                ParcelFileDescriptor.MODE_READ_ONLY
            else
                ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_TRUNCATE
            return ParcelFileDescriptor.open(file, mode)
        }
        return null
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                       selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
        // Don'data support queries.
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // Don'data support inserts.
        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?,
                        selectionArgs: Array<String>?): Int {
        // Don'data support updates.
        return 0
    }

    override fun getType(uri: Uri): String? {
        // No need for mime types.
        return null
    }

    companion object {
        // Object to generate random id for temp images.
        private val RANDOM_ID = Random()

        private val FILE_EXTENSION_PARAM_KEY = "ext"

        /**
         * Check if filename conforms to requirement for our provider
         *
         * @param fileId filename (optionally starting with path character
         * @return true if filename consists only of digits
         */
        @JvmStatic
        protected fun isValidFileId(fileId: String): Boolean {
            // Ignore initial "/"
            for (index in (if (fileId.startsWith("/")) 1 else 0) until fileId.length) {
                val c = fileId[index]
                if (!Character.isDigit(c)) {
                    return false
                }
            }
            return true
        }

        /**
         * Create a temp file (to allow writing to that one particular file)
         *
         * @param file the file to create
         * @return true if file successfully created
         */
        @JvmStatic
        protected fun ensureFileExists(file: File): Boolean {
            try {
                val parentDir = file.parentFile
                if (parentDir.exists() || parentDir.mkdirs()) {
                    return file.createNewFile()
                }
            } catch (e: IOException) {
                // fail on exceptions creating the file
            }

            return false
        }

        /**
         * Build uri for a ic_new temporary file (creating file)
         *
         * @param authority authority with which to populate uri
         * @param extension optional file extension
         * @return unique uri that can be used to write temporary files
         */
        @JvmStatic
        protected fun buildFileUri(authority: String, extension: String): Uri {
            val fileId = Math.abs(RANDOM_ID.nextLong())
            val builder = Uri.Builder().authority(authority).scheme(
                    ContentResolver.SCHEME_CONTENT)
            builder.appendPath(fileId.toString())
            if (!TextUtils.isEmpty(extension)) {
                builder.appendQueryParameter(FILE_EXTENSION_PARAM_KEY, extension)
            }
            return builder.build()
        }
        @JvmStatic
        protected fun getExtensionFromUri(uri: Uri): String? {
            return uri.getQueryParameter(FILE_EXTENSION_PARAM_KEY)
        }
    }
}
