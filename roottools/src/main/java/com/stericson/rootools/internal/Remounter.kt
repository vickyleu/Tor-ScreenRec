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
import java.io.IOException
import java.util.ArrayList

import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell
import com.stericson.rootools.Constants
import com.stericson.rootools.RootTools
import com.stericson.rootools.containers.Mount

class Remounter {

    //-------------
    //# Remounter #
    //-------------

    /**
     * This will take a path, which can contain the file name as well,
     * and attempt to remount the underlying partition.
     *
     *
     * For example, passing in the following string:
     * "/system/bin/some/directory/that/really/would/never/exist"
     * will result in /system ultimately being remounted.
     * However, keep in mind that the longer the path you supply, the more work this has to do,
     * and the slower it will run.
     *
     * @param file      file path
     * @param mountType mount type: pass in RO (Read only) or RW (Read Write)
     * @return a `boolean` which indicates whether or not the partition
     * has been remounted as specified.
     */

    fun remount(file: String, mountType: String): Boolean {
        var file = file

        //if the path has a trailing slash get rid of it.
        if (file.endsWith("/") && file != "/") {
            file = file.substring(0, file.lastIndexOf("/"))
        }
        //Make sure that what we are trying to remount is in the mount list.
        var foundMount = false

        while (!foundMount) {
            try {
                for (mount in RootTools.mounts) {
                    RootTools.log(mount.mountPoint.toString())

                    if (file == mount.mountPoint.toString()) {
                        foundMount = true
                        break
                    }
                }
            } catch (e: Exception) {
                if (RootTools.debugMode) {
                    e.printStackTrace()
                }
                return false
            }

            if (!foundMount) {
                try {
                    file = File(file).parent
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }

            }
        }

        var mountPoint = findMountPointRecursive(file)

        if (mountPoint != null) {

            RootTools.log(Constants.TAG, "Remounting " + mountPoint.mountPoint.absolutePath + " as " + mountType.toLowerCase())
            val isMountMode = mountPoint.flags.contains(mountType.toLowerCase())

            if (!isMountMode) {
                //grab an instance of the internal class
                try {
                    val command = Command(0,
                            true,
                            "busybox mount -o remount," + mountType.toLowerCase() + " " + mountPoint.device.absolutePath + " " + mountPoint.mountPoint.absolutePath,
                            "toolbox mount -o remount," + mountType.toLowerCase() + " " + mountPoint.device.absolutePath + " " + mountPoint.mountPoint.absolutePath,
                            "toybox mount -o remount," + mountType.toLowerCase() + " " + mountPoint.device.absolutePath + " " + mountPoint.mountPoint.absolutePath,
                            "mount -o remount," + mountType.toLowerCase() + " " + mountPoint.device.absolutePath + " " + mountPoint.mountPoint.absolutePath,
                            "mount -o remount," + mountType.toLowerCase() + " " + file,
                            "/system/bin/toolbox mount -o remount," + mountType.toLowerCase() + " " + mountPoint.device.absolutePath + " " + mountPoint.mountPoint.absolutePath,
                            "/system/bin/toybox mount -o remount," + mountType.toLowerCase() + " " + mountPoint.device.absolutePath + " " + mountPoint.mountPoint.absolutePath
                    )
                    Shell.startRootShell().add(command)
                    commandWait(command)

                } catch (e: Exception) {
                }

                mountPoint = findMountPointRecursive(file)
            }

            if (mountPoint != null) {
                RootTools.log(Constants.TAG, "${mountPoint.flags} AND ${mountType.toLowerCase()}")
                if (mountPoint.flags.contains(mountType.toLowerCase())) {
                    RootTools.log(mountPoint.flags.toString())
                    return true
                } else {
                    RootTools.log(mountPoint.flags.toString())
                    return false
                }
            } else {
                RootTools.log("mount is null, file was: $file mountType was: $mountType")
            }
        } else {
            RootTools.log("mount is null, file was: $file mountType was: $mountType")
        }

        return false
    }

    private fun findMountPointRecursive(file: String): Mount? {
        try {
            val mounts = RootTools.mounts

            val path = File(file)
            while (path != null) {
                for (mount in mounts) {
                    if (mount.mountPoint == path) {
                        return mount
                    }
                }
            }

            return null

        } catch (e: IOException) {
            if (RootTools.debugMode) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            if (RootTools.debugMode) {
                e.printStackTrace()
            }
        }

        return null
    }

    private fun commandWait(cmd: Command) {
        synchronized(cmd) {
            try {
                if (!cmd.isFinished) {
                    cmd.p_wait(2000)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
    }
}
