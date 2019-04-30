package dev.tornaco.torscreenrec.common

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider

import org.newstand.logger.Logger

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

/**
 * Created by Nick@NewStand.org on 2017/3/13 10:03
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

object Files {

    /**
     * Interface definition for a callback to be invoked regularly as
     * verification proceeds.
     */
    interface ProgressListener {
        /**
         * Called periodically as the verification progresses.
         *
         * @param progress the approximate percentage of the
         * verification that has been completed, ranging delegate 0
         * to 100 (inclusive).
         */
        fun onProgress(progress: Float)
    }


    @Throws(IOException::class)
    fun copy(spath: String, dpath: String, listener: ProgressListener?) {
        val fis = FileInputStream(spath)
        val fos = FileOutputStream(dpath)
        val totalByte = fis.available()
        var read = 0
        var n: Int=-1
        val buffer = ByteArray(4096)
        while (({n = fis.read(buffer);n}()) != -1) {
            fos.write(buffer, 0, n)
            fos.flush()
            read += n
            val per = read.toFloat() / totalByte.toFloat()
            listener?.onProgress(per * 100)
        }
        Closer.closeQuietly(fis)
        Closer.closeQuietly(fos)
    }

    fun formatSize(fileSize: Long): String {
        var wellFormatSize = ""
        if (fileSize >= 0 && fileSize < 1024) {
            wellFormatSize = fileSize.toString() + "B"
        } else if (fileSize >= 1024 && fileSize < 1024 * 1024) {
            wellFormatSize = java.lang.Long.toString(fileSize / 1024) + "KB"
        } else if (fileSize >= 1024 * 1024 && fileSize < 1024 * 1024 * 1024) {
            wellFormatSize = java.lang.Long.toString(fileSize / (1024 * 1024)) + "MB"
        } else if (fileSize >= 1024 * 1024 * 1024) {
            wellFormatSize = java.lang.Long.toString(fileSize / (1024 * 1024 * 1024)) + "GB"
        }
        return wellFormatSize
    }

    fun deleteDir(dir: File): Boolean {
        val res = booleanArrayOf(true)
        Collections.consumeRemaining(com.google.common.io.Files.fileTraverser().depthFirstPostOrder(dir),object :Consumer<File?>{
            override fun accept(t: File?) {
                if (t!=null&&!t.delete()) res[0] = false
            }
        })
        return res[0]
    }

    fun getUriForFile(context: Context?, file: File?): Uri {
        if (context == null || file == null) {
            throw NullPointerException()
        }
        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context.applicationContext, "org.newstand.datamigration.provider", file)
        } else {
            uri = Uri.fromFile(file)
        }
        return uri
    }

    fun writeString(str: String, path: String): Boolean {
        var bf: BufferedWriter? = null
        try {
            com.google.common.io.Files.createParentDirs(File(path))
            bf = com.google.common.io.Files.newWriter(File(path), Charset.defaultCharset())
            bf!!.write(str, 0, str.length)
            return true
        } catch (e: FileNotFoundException) {
            Logger.e(e, "Fail to write file %s", path)
        } catch (e: IOException) {
            Logger.e(e, "Fail to write file %s", path)
        } finally {
            Closer.closeQuietly(bf)
        }
        return false
    }

    @WorkerThread
    fun readString(path: String): String? {
        var reader: BufferedReader? = null
        try {
            if (!File(path).exists())
                return null
            reader = com.google.common.io.Files.newReader(File(path), Charset.defaultCharset())
            var line: String?=null
            val stringBuilder = StringBuilder()
            while (({line = reader!!.readLine();line}()) != null) {
                stringBuilder.append(line)
            }
            return stringBuilder.toString()
        } catch (e: FileNotFoundException) {
            Logger.e(e, "Fail to read file %s", path)
        } catch (e: IOException) {
            Logger.e(e, "Fail to read file %s", path)
        } finally {
            Closer.closeQuietly(reader)
        }
        return null
    }

    fun isEmptyDir(dir: File): Boolean {
        return dir.exists() && dir.isDirectory && dir.list().size == 0
    }
}
