package dev.tornaco.torscreenrec.util

import android.content.Context

/**
 * Created by Tornaco on 2017/7/27.
 * Licensed with Apache.
 */

object FFMpegInstaller {

    fun installAsync(context: Context) {
        //        ThreadUtil.newThread(new Runnable() {
        //            @Override
        //            public void run() {
        //                try {
        //                    FFmpeg.getInstance(context).loadBinary(new FFmpegLoadBinaryResponseHandler() {
        //                        @Override
        //                        public void onFailure() {
        //                            Logger.d("FFMpeg loading onFailure");
        //                        }
        //
        //                        @Override
        //                        public void onSuccess() {
        //                            Logger.d("FFMpeg loading onSuccess");
        //                        }
        //
        //                        @Override
        //                        public void onStart() {
        //                            Logger.d("FFMpeg loading onStart");
        //                        }
        //
        //                        @Override
        //                        public void onFinish() {
        //                            Logger.d("FFMpeg loading onFinish");
        //                        }
        //                    });
        //                } catch (FFmpegNotSupportedException e) {
        //                    Logger.d("Fail load FFMPEG:" + e.getLocalizedMessage());
        //                }
        //            }
        //        }).start();
    }
}
