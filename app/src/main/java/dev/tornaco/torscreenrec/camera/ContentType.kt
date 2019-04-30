/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

import android.webkit.MimeTypeMap

object ContentType {
    var THREE_GPP_EXTENSION = "3gp"
    var VIDEO_MP4_EXTENSION = "mp4"
    // Default extension used when we don'data know one.
    var DEFAULT_EXTENSION = "dat"

    val TYPE_IMAGE = 0
    val TYPE_VIDEO = 1
    val TYPE_AUDIO = 2
    val TYPE_VCARD = 3
    val TYPE_OTHER = 4

    val ANY_TYPE = "*/*"
    val MMS_MESSAGE = "application/vnd.wap.mms-message"
    // The phony content type for generic PDUs (e.g. ReadOrig.ind,
    // Notification.ind, Delivery.ind).
    val MMS_GENERIC = "application/vnd.wap.mms-generic"
    val MMS_MULTIPART_MIXED = "application/vnd.wap.multipart.mixed"
    val MMS_MULTIPART_RELATED = "application/vnd.wap.multipart.related"
    val MMS_MULTIPART_ALTERNATIVE = "application/vnd.wap.multipart.alternative"

    val TEXT_PLAIN = "text/plain"
    val TEXT_HTML = "text/html"
    val TEXT_VCALENDAR = "text/x-vCalendar"
    val TEXT_VCARD = "text/x-vCard"

    val IMAGE_PREFIX = "image/"
    val IMAGE_UNSPECIFIED = "image/*"
    val IMAGE_JPEG = "image/jpeg"
    val IMAGE_JPG = "image/jpg"
    val IMAGE_GIF = "image/gif"
    val IMAGE_WBMP = "image/vnd.wap.wbmp"
    val IMAGE_PNG = "image/png"
    val IMAGE_X_MS_BMP = "image/x-ms-bmp"

    val AUDIO_UNSPECIFIED = "audio/*"
    val AUDIO_AAC = "audio/aac"
    val AUDIO_AMR = "audio/amr"
    val AUDIO_IMELODY = "audio/imelody"
    val AUDIO_MID = "audio/mid"
    val AUDIO_MIDI = "audio/midi"
    val AUDIO_MP3 = "audio/mp3"
    val AUDIO_MPEG3 = "audio/mpeg3"
    val AUDIO_MPEG = "audio/mpeg"
    val AUDIO_MPG = "audio/mpg"
    val AUDIO_MP4 = "audio/mp4"
    val AUDIO_MP4_LATM = "audio/mp4-latm"
    val AUDIO_X_MID = "audio/x-mid"
    val AUDIO_X_MIDI = "audio/x-midi"
    val AUDIO_X_MP3 = "audio/x-mp3"
    val AUDIO_X_MPEG3 = "audio/x-mpeg3"
    val AUDIO_X_MPEG = "audio/x-mpeg"
    val AUDIO_X_MPG = "audio/x-mpg"
    val AUDIO_3GPP = "audio/3gpp"
    val AUDIO_X_WAV = "audio/x-wav"
    val AUDIO_OGG = "application/ogg"

    val MULTIPART_MIXED = "multipart/mixed"

    val VIDEO_UNSPECIFIED = "video/*"
    val VIDEO_3GP = "video/3gp"
    val VIDEO_3GPP = "video/3gpp"
    val VIDEO_3G2 = "video/3gpp2"
    val VIDEO_H263 = "video/h263"
    val VIDEO_M4V = "video/m4v"
    val VIDEO_MP4 = "video/mp4"
    val VIDEO_MPEG = "video/mpeg"
    val VIDEO_MPEG4 = "video/mpeg4"
    val VIDEO_WEBM = "video/webm"

    val APP_SMIL = "application/smil"
    val APP_WAP_XHTML = "application/vnd.wap.xhtml+xml"
    val APP_XHTML = "application/xhtml+xml"

    val APP_DRM_CONTENT = "application/vnd.oma.drm.content"
    val APP_DRM_MESSAGE = "application/vnd.oma.drm.message"

    fun isTextType(contentType: String): Boolean {
        return (TEXT_PLAIN == contentType
                || TEXT_HTML == contentType
                || APP_WAP_XHTML == contentType)
    }

    fun isMediaType(contentType: String): Boolean {
        return (isImageType(contentType)
                || isVideoType(contentType)
                || isAudioType(contentType)
                || isVCardType(contentType))
    }

    fun isImageType(contentType: String?): Boolean {
        return null != contentType && contentType.startsWith(IMAGE_PREFIX)
    }

    fun isAudioType(contentType: String?): Boolean {
        return null != contentType && (contentType.startsWith("audio/") || contentType.equals(AUDIO_OGG, ignoreCase = true))
    }

    fun isVideoType(contentType: String?): Boolean {
        return null != contentType && contentType.startsWith("video/")
    }

    fun isVCardType(contentType: String?): Boolean {
        return null != contentType && contentType.equals(TEXT_VCARD, ignoreCase = true)
    }

    fun isDrmType(contentType: String?): Boolean {
        return null != contentType && (contentType == APP_DRM_CONTENT || contentType == APP_DRM_MESSAGE)
    }

    fun isUnspecified(contentType: String?): Boolean {
        return null != contentType && contentType.endsWith("*")
    }

    /**
     * If the content type is a type which can be displayed in the conversation list as a preview.
     */
    fun isConversationListPreviewableType(contentType: String): Boolean {
        return ContentType.isAudioType(contentType) || ContentType.isVideoType(contentType) ||
                ContentType.isImageType(contentType) || ContentType.isVCardType(contentType)
    }

    /**
     * Given a filename, look at the extension and try and determine the mime type.
     *
     * @param fileName           a filename to determine the type from, such as img1231.jpg
     * @param contentTypeDefault type to use when the content type can'data be determined from the file
     * extension. It can be null or a type such as ContentType.IMAGE_UNSPECIFIED
     * @return Content type of the extension.
     */
    fun getContentTypeFromExtension(fileName: String,
                                    contentTypeDefault: String): String? {
        val mimeTypeMap = MimeTypeMap.getSingleton()
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileName)
        var contentType = mimeTypeMap.getMimeTypeFromExtension(extension)
        if (contentType == null) {
            contentType = contentTypeDefault
        }
        return contentType
    }

    /**
     * Get the common file extension for a given content type
     *
     * @param contentType The content type
     * @return The extension without the .
     */
    fun getExtension(contentType: String): String {
        return if (VIDEO_MP4 == contentType) {
            VIDEO_MP4_EXTENSION
        } else if (VIDEO_3GPP == contentType) {
            THREE_GPP_EXTENSION
        } else {
            DEFAULT_EXTENSION
        }
    }
}// This class should never be instantiated.
