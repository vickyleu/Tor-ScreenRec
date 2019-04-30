/* 
 * This file is part of the RootTools Project: http://code.google.com/p/RootTools/
 *  
 * Copyright (c) 2012 Stephen Erickson, Chris Ravenscroft, Dominik Schuermann, Adam Shanks
 *  
 * This code is dual-licensed under the terms of the Apache License Version 2.0 and
 * the terms of the General Public License (GPL) Version 2.
 * You may use this code according to either of these licenses as is most appropriate
 * for your project on a case-by-case basis.
 * 
 * The terms of each license can be found in the root directory of this project's repository as well as at:
 * 
 * * http://www.apache.org/licenses/LICENSE-2.0
 * * http://www.gnu.org/licenses/gpl-2.0.txt
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under these Licenses is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See each License for the specific language governing permissions and
 * limitations under that License.
 */

package com.stericson.rootools.internal

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

import android.util.Log

import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell
import com.stericson.rootools.RootTools

import android.content.Context

internal class Installer @Throws(IOException::class)
constructor(var context: Context) {
    var filesPath: String

    init {
        this.filesPath = context.filesDir.canonicalPath
    }

    /**
     * This method can be used to unpack a binary from the raw resources folder and store it in
     * /data/data/app.package/files/
     * This is typically useful if you provide your own C- or C++-based binary.
     * This binary can then be executed using sendShell() and its full path.
     *
     * @param sourceId resource id; typically `R.raw.id`
     * @param destName destination file name; appended to /data/data/app.package/files/
     * @param mode     chmod value for this file
     * @return a `boolean` which indicates whether or not we were
     * able to create the new file.
     */
    fun installBinary(sourceId: Int, destName: String, mode: String): Boolean {
        val mf = File(filesPath + File.separator + destName)
        if (!mf.exists() || getFileSignature(mf) != getStreamSignature(
                        context.resources.openRawResource(sourceId))) {
            Log.e(LOG_TAG, "Installing a new version of binary: $destName")
            // First, does our files/ directory even exist?
            // We cannot wait for android to lazily create it as we will soon
            // need it.
            try {
                val fis = context.openFileInput(BOGUS_FILE_NAME)
                fis.close()
            } catch (e: FileNotFoundException) {
                var fos: FileOutputStream? = null
                try {
                    fos = context.openFileOutput("bogus", Context.MODE_PRIVATE)
                    fos!!.write("justcreatedfilesdirectory".toByteArray())
                } catch (ex: Exception) {
                    if (RootTools.debugMode) {
                        Log.e(LOG_TAG, ex.toString())
                    }
                    return false
                } finally {
                    if (null != fos) {
                        try {
                            fos.close()
                            context.deleteFile(BOGUS_FILE_NAME)
                        } catch (e1: IOException) {
                        }

                    }
                }
            } catch (ex: IOException) {
                if (RootTools.debugMode) {
                    Log.e(LOG_TAG, ex.toString())
                }
                return false
            }

            // Only now can we start creating our actual file
            val iss = context.resources.openRawResource(sourceId)
            val rfc = Channels.newChannel(iss)
            var oss: FileOutputStream? = null
            try {
                oss = FileOutputStream(mf)
                val ofc = oss.channel
                var pos: Long = 0
                try {
                    val size = iss.available().toLong()
                    while (({pos += ofc.transferFrom(rfc, pos, size - pos);pos}()) < size) {
                    }
                } catch (ex: IOException) {
                    if (RootTools.debugMode) {
                        Log.e(LOG_TAG, ex.toString())
                    }
                    return false
                }

            } catch (ex: FileNotFoundException) {
                if (RootTools.debugMode) {
                    Log.e(LOG_TAG, ex.toString())
                }
                return false
            } finally {
                if (oss != null) {
                    try {
                        oss.flush()
                        oss.fd.sync()
                        oss.close()
                    } catch (e: Exception) {
                    }

                }
            }
            try {
                iss.close()
            } catch (ex: IOException) {
                if (RootTools.debugMode) {
                    Log.e(LOG_TAG, ex.toString())
                }
                return false
            }

            try {
                val command = Command(0, false, "chmod " + mode + " " + filesPath + File.separator + destName)
                Shell.startRootShell().add(command)
                commandWait(command)

            } catch (e: Exception) {
            }

        }
        return true
    }

    fun isBinaryInstalled(destName: String): Boolean {
        var installed = false
        val mf = File(filesPath + File.separator + destName)
        if (mf.exists()) {
            installed = true
            // TODO: pass mode as argument and check it matches
        }
        return installed
    }

    protected fun getFileSignature(f: File): String {
        var signature = ""
        try {
            signature = getStreamSignature(FileInputStream(f))
        } catch (ex: FileNotFoundException) {
            Log.e(LOG_TAG, ex.toString())
        }

        return signature
    }

    /*
     * Note: this method will close any string passed to it
     */
    protected fun getStreamSignature(`is`: InputStream): String {
        var signature = ""
        try {
            val md = MessageDigest.getInstance("MD5")
            val dis = DigestInputStream(`is`, md)
            val buffer = ByteArray(4096)
            while (-1 != dis.read(buffer)) {
            }
            val digest = md.digest()
            val sb = StringBuffer()

            for (i in digest.indices) {
                sb.append(Integer.toHexString(digest[i].toInt() and 0xFF))
            }

            signature = sb.toString()
        } catch (ex: IOException) {
            Log.e(LOG_TAG, ex.toString())
        } catch (ex: NoSuchAlgorithmException) {
            Log.e(LOG_TAG, ex.toString())
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
            }

        }
        return signature
    }

    private fun commandWait(cmd: Command) {
        synchronized(cmd) {
            try {
                if (!cmd.isFinished) {
                    cmd.p_wait(2000)
                }else{
                }
            } catch (ex: InterruptedException) {
                Log.e(LOG_TAG, ex.toString())
            }

        }
    }

    companion object {

        //-------------
        //# Installer #
        //-------------

        val LOG_TAG = "RootTools::Installer"

        val BOGUS_FILE_NAME = "bogus"
    }
}
