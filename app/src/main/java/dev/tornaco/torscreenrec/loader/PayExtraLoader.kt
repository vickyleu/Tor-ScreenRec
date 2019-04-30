package dev.tornaco.torscreenrec.loader

import android.text.TextUtils

import com.google.common.io.Files
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.koushikdutta.async.http.AsyncHttpClient
import com.koushikdutta.async.http.AsyncHttpGet
import com.koushikdutta.async.http.AsyncHttpResponse

import org.newstand.logger.Logger

import java.io.File
import java.util.ArrayList

import dev.tornaco.torscreenrec.common.SharedExecutor
import dev.tornaco.torscreenrec.modle.PayExtra

/**
 * Created by Tornaco on 2017/7/29.
 * Licensed with Apache.
 */

class PayExtraLoader {

    interface Callback {
        fun onError(e: Throwable?)

        fun onSuccess(extras: List<PayExtra>)
    }

    fun loadAsync(from: String, callback: Callback) {
        SharedExecutor.execute(Runnable {
            load(from, callback)
        })
    }

    fun load(from: String, callback: Callback) {
        val tmpDir = Files.createTempDir().path
        val fileName = tmpDir + File.separator + "pays"

        AsyncHttpClient.getDefaultInstance().executeFile(AsyncHttpGet(from),
                fileName,
                object : AsyncHttpClient.FileCallback() {
                    override fun onCompleted(e: Exception?, source: AsyncHttpResponse, result: File) {
                        Logger.i("onCompleted %s, %s", e, result)
                        if (e == null) {

                            val content = dev.tornaco.torscreenrec.common.Files.readString(result.path)
                            if (TextUtils.isEmpty(content)) {
                                callback.onError(Exception("Empty content"))
                                return
                            }

                            try {
                                val payExtras = Gson().fromJson<ArrayList<PayExtra>>(content,
                                        object : TypeToken<ArrayList<PayExtra>>() {

                                        }.type)
                                callback.onSuccess(payExtras)
                            } catch (w: Throwable) {
                                Logger.e("Fail to json %s", Logger.getStackTraceString(w))
                                callback.onError(w)
                            }


                        } else {
                            callback.onError(e)
                        }
                    }
                })

    }
}
