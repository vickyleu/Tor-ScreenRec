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

import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.net.Uri
import dev.tornaco.torscreenrec.TorScreenRecApp
import java.io.FileNotFoundException

internal class MmsVideoRecorder @Throws(FileNotFoundException::class)
constructor(camera: Camera?, cameraIndex: Int, orientation: Int,
            maxMessageSize: Int) : MediaRecorder() {
    /**
     * The settings used for video recording
     */
    private val mCamcorderProfile: CamcorderProfile
    /**
     * The uri where video is being recorded to
     */
    var videoUri: Uri? = null
        private set

    val videoWidth: Int
        get() = mCamcorderProfile.videoFrameWidth

    val videoHeight: Int
        get() = mCamcorderProfile.videoFrameHeight

    // 3GPP is the only other video format with a constant in OutputFormat
    val contentType: String
        get() = if (mCamcorderProfile.fileFormat == OutputFormat.MPEG_4) {
            ContentType.VIDEO_MP4
        } else {
            ContentType.VIDEO_3GPP
        }

    init {
        mCamcorderProfile = CamcorderProfile.get(cameraIndex, CamcorderProfile.QUALITY_LOW)
        videoUri = MediaScratchFileProvider.buildMediaScratchSpaceUri(
                ContentType.getExtension(contentType))

        // The video recorder can sometimes return a file that's larger than the max we
        // say we can handle. Try to handle that overshoot by specifying an 85% limit.
        val sizeLimit = (maxMessageSize * VIDEO_OVERSHOOT_SLOP).toLong()

        // The QUALITY_LOW profile might not be low enough to allow for video of a reasonable
        // minimum duration.  Adjust a/v bitrates to allow at least MIN_DURATION_LIMIT video
        // to be recorded.
        var audioBitRate = mCamcorderProfile.audioBitRate
        var videoBitRate = mCamcorderProfile.videoBitRate
        val initialDurationLimit = sizeLimit * BITS_PER_BYTE / (audioBitRate + videoBitRate).toDouble()
        if (initialDurationLimit < MIN_DURATION_LIMIT_SECONDS) {
            // Reduce the suggested bitrates.  These bitrates are only requests, if implementation
            // can'data actually hit these goals it will still record video at higher rate and stop when
            // it hits the size limit.
            val bitRateAdjustmentFactor = initialDurationLimit / MIN_DURATION_LIMIT_SECONDS
            audioBitRate *= bitRateAdjustmentFactor.toInt()
            videoBitRate *= bitRateAdjustmentFactor.toInt()
        }

        setCamera(camera)
        setOrientationHint(orientation)
        setAudioSource(AudioSource.CAMCORDER)
        setVideoSource(VideoSource.CAMERA)
        setOutputFormat(mCamcorderProfile.fileFormat)
        setOutputFile(
                TorScreenRecApp.app!!.getApplicationContext().getContentResolver().openFileDescriptor(
                        videoUri!!, "w")!!.getFileDescriptor())

        // Copy settings from CamcorderProfile to MediaRecorder
        setAudioEncodingBitRate(audioBitRate)
        setAudioChannels(mCamcorderProfile.audioChannels)
        setAudioEncoder(mCamcorderProfile.audioCodec)
        setAudioSamplingRate(mCamcorderProfile.audioSampleRate)
        setVideoEncodingBitRate(videoBitRate)
        setVideoEncoder(mCamcorderProfile.videoCodec)
        setVideoFrameRate(mCamcorderProfile.videoFrameRate)
        setVideoSize(
                mCamcorderProfile.videoFrameWidth, mCamcorderProfile.videoFrameHeight)
        setMaxFileSize(sizeLimit)
    }

    fun cleanupTempFile() {
        val tempUri = videoUri
        SafeAsyncTask.executeOnThreadPool(Runnable {
            TorScreenRecApp.app?.applicationContext?.contentResolver?.delete(
                    tempUri!!, null, null)
        })
        videoUri = null
    }

    companion object {
        private val VIDEO_OVERSHOOT_SLOP = .85f

        private val BITS_PER_BYTE = 8

        // We think user will expect to be able to record videos at least this long
        private val MIN_DURATION_LIMIT_SECONDS: Long = 25
    }
}
