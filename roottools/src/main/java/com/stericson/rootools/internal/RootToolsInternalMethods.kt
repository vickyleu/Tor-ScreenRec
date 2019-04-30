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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log

import com.stericson.rootools.Constants
import com.stericson.rootools.RootTools
import com.stericson.rootools.containers.Mount
import com.stericson.rootools.containers.Permissions
import com.stericson.rootools.containers.Symlink
import com.stericson.rootshell.RootShell
import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.TimeoutException
import java.util.regex.Matcher

class RootToolsInternalMethods
// --------------------
// # Internal methods #
// --------------------

protected constructor() {

    /**
     * This will return an ArrayList of the class Mount. The class mount contains the following
     * property's: device mountPoint type flags
     *
     *
     * These will provide you with any information you need to work with the mount points.
     *
     * @return `ArrayList<Mount></Mount>` an ArrayList of the class Mount.
     * @throws Exception if we cannot return the mount points.
     */
    // device
    // mountPoint
    // fstype
    // flags
    val mounts: ArrayList<Mount>
        @Throws(Exception::class)
        get() {

            InternalVariables.mounts = ArrayList()

            if (null == InternalVariables.mounts || InternalVariables.mounts!!.isEmpty()) {
                val shell = RootTools.getShell(true)

                val cmd = object : Command(Constants.GET_MOUNTS,
                        false,
                        "cat /proc/mounts") {

                    override fun commandOutput(id: Int, line: String?) {
                        if (id == Constants.GET_MOUNTS) {
                            RootTools.log(line)

                            val fields = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            InternalVariables.mounts!!.add(Mount(File(fields[0]),
                                    File(fields[1]),
                                    fields[2],
                                    fields[3]
                            ))
                        }

                        super.commandOutput(id, line)
                    }
                }
                shell.add(cmd)
                this.commandWait(shell, cmd)
            }

            return InternalVariables.mounts?: arrayListOf()
        }

    /**
     * This will return to you a string to be used in your shell commands which will represent the
     * valid working toolbox with correct permissions. For instance, if Busybox is available it will
     * return "busybox", if busybox is not available but toolbox is then it will return "toolbox"
     *
     * @return String that indicates the available toolbox to use for accessing applets.
     */
    val workingToolbox: String
        get() = if (RootTools.checkUtil("busybox")) {
            "busybox"
        } else if (RootTools.checkUtil("toolbox")) {
            "toolbox"
        } else {
            ""
        }

    fun getPermissions(line: String): Permissions? {

        val lineArray = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val rawPermissions = lineArray[0]

        if (rawPermissions.length == 10
                && (rawPermissions[0] == '-'
                        || rawPermissions[0] == 'd' || rawPermissions[0] == 'l')
                && (rawPermissions[1] == '-' || rawPermissions[1] == 'r')
                && (rawPermissions[2] == '-' || rawPermissions[2] == 'w')) {
            RootTools.log(rawPermissions)

            val permissions = Permissions()

            permissions.type = rawPermissions.substring(0, 1)

            RootTools.log(permissions.type)

            permissions.userPermissions = rawPermissions.substring(1, 4)

            RootTools.log(permissions.userPermissions)

            permissions.groupPermissions = rawPermissions.substring(4, 7)

            RootTools.log(permissions.groupPermissions)

            permissions.otherPermissions = rawPermissions.substring(7, 10)

            RootTools.log(permissions.otherPermissions)

            val finalPermissions = StringBuilder()
            finalPermissions.append(parseSpecialPermissions(rawPermissions))
            finalPermissions.append(parsePermissions(permissions.userPermissions?:""))
            finalPermissions.append(parsePermissions(permissions.groupPermissions?:""))
            finalPermissions.append(parsePermissions(permissions.otherPermissions?:""))

            permissions.permissions = Integer.parseInt(finalPermissions.toString())

            return permissions
        }

        return null
    }

    fun parsePermissions(permission: String): Int {
        var permission = permission
        permission = permission.toLowerCase(Locale.US)
        var tmp: Int
        if (permission[0] == 'r') {
            tmp = 4
        } else {
            tmp = 0
        }

        RootTools.log("permission $tmp")
        RootTools.log("character " + permission[0])

        if (permission[1] == 'w') {
            tmp += 2
        } else {
            tmp += 0
        }

        RootTools.log("permission $tmp")
        RootTools.log("character " + permission[1])

        if (permission[2] == 'x' || permission[2] == 's'
                || permission[2] == 't') {
            tmp += 1
        } else {
            tmp += 0
        }

        RootTools.log("permission $tmp")
        RootTools.log("character " + permission[2])

        return tmp
    }

    fun parseSpecialPermissions(permission: String): Int {
        var tmp = 0
        if (permission[2] == 's') {
            tmp += 4
        }

        if (permission[5] == 's') {
            tmp += 2
        }

        if (permission[8] == 't') {
            tmp += 1
        }

        RootTools.log("special permissions $tmp")

        return tmp
    }

    /**
     * Copys a file to a destination. Because cp is not available on all android devices, we have a
     * fallback on the cat command
     *
     * @param source                 example: /data/data/org.adaway/files/hosts
     * @param destination            example: /system/etc/hosts
     * @param remountAsRw            remounts the destination as read/write before writing to it
     * @param preserveFileAttributes tries to copy file attributes from source to destination, if only cat is available
     * only permissions are preserved
     * @return true if it was successfully copied
     */
    fun copyFile(source: String, destination: String, remountAsRw: Boolean,
                 preserveFileAttributes: Boolean, mode: Int): Boolean {

        var command: Command? = null
        var result = true

        try {
            // mount destination as rw before writing to it
            if (remountAsRw) {
                RootTools.remount(destination, "RW")
            }

            // if cp is available and has appropriate permissions
            if (checkUtil("cp")) {
                RootTools.log("cp command is available!")

                if (preserveFileAttributes) {
                    command = Command(0, false, "cp -rfp $source $destination")
                    Shell.startRootShell().add(command)
                    commandWait(Shell.startRootShell(), command)

                    //ensure that the file was copied, an exitcode of zero means success
                    result = command.exitCode == 0

                } else {
                    command = Command(0, false, "cp -rf $source $destination")
                    Shell.startRootShell().add(command)
                    commandWait(Shell.startRootShell(), command)

                    //ensure that the file was copied, an exitcode of zero means success
                    result = command.exitCode == 0

                }
            } else {
                if (checkUtil("busybox") && hasUtil("cp", "busybox")) {
                    RootTools.log("busybox cp command is available!")

                    if (preserveFileAttributes) {
                        command = Command(0, false, "busybox cp -rfp $source $destination")
                        Shell.startRootShell().add(command)
                        commandWait(Shell.startRootShell(), command)

                    } else {
                        command = Command(0, false, "busybox cp -rf $source $destination")
                        Shell.startRootShell().add(command)
                        commandWait(Shell.startRootShell(), command)

                    }
                } else { // if cp is not available use cat
                    // if cat is available and has appropriate permissions
                    if (checkUtil("cat")) {
                        RootTools.log("cp is not available, use cat!")

                        var filePermission = -1
                        if (preserveFileAttributes) {
                            // get permissions of source before overwriting
                            val permissions = getFilePermissionsSymlinks(source)
                            filePermission = permissions!!.permissions
                        }

                        // copy with cat
                        command = Command(0, false, "cat $source > $destination")
                        Shell.startRootShell().add(command)
                        commandWait(Shell.startRootShell(), command)

                        if (preserveFileAttributes) {
                            // set premissions of source to destination
                            command = Command(0, false, "chmod $filePermission $destination")
                            Shell.startRootShell().add(command)
                            commandWait(Shell.startRootShell(), command)
                        }
                    } else {
                        result = false
                    }
                }
            }

            command = Command(0, false, "chmod $mode $destination")
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)

            // mount destination back to ro
            if (remountAsRw) {
                RootTools.remount(destination, "RO")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = false
        }

        if (command != null) {
            //ensure that the file was copied, an exitcode of zero means success
            result = command.exitCode == 0
        }

        return result
    }

    fun mkdir(path: String, remountAsRw: Boolean, mode: Int): Boolean {
        var command: Command? = null
        var result = true

        try {
            // mount destination as rw before writing to it
            if (remountAsRw) {
                RootTools.remount(path, "RW")
            }

            command = Command(0, false, "mkdir -p $path")
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)

            command = Command(0, false, "chmod $mode $path")
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)
            // mount destination back to ro
            if (remountAsRw) {
                RootTools.remount(path, "RO")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = false
        }

        if (command != null) {
            //ensure that the file was copied, an exitcode of zero means success
            result = command.exitCode == 0
        }

        return result
    }

    /**
     * This will check a given binary, determine if it exists and determine that
     * it has either the permissions 755, 775, or 777.
     *
     * @param util Name of the utility to check.
     * @return boolean to indicate whether the binary is installed and has
     * appropriate permissions.
     */
    fun checkUtil(util: String): Boolean {
        val foundPaths = RootShell.findBinary(util, true)
        if (foundPaths.size > 0) {

            for (path in foundPaths) {
                val permissions = RootTools
                        .getFilePermissionsSymlinks("$path/$util")

                if (permissions != null) {
                    val permission: String

                    if (Integer.toString(permissions.permissions).length > 3) {
                        permission = Integer.toString(permissions.permissions).substring(1)
                    } else {
                        permission = Integer.toString(permissions.permissions)
                    }

                    if (permission == "755" || permission == "777"
                            || permission == "775") {
                        RootTools.utilPath = "$path/$util"
                        return true
                    }
                }
            }
        }

        return false

    }

    /**
     * Deletes a file or directory
     *
     * @param target      example: /data/data/org.adaway/files/hosts
     * @param remountAsRw remounts the destination as read/write before writing to it
     * @return true if it was successfully deleted
     */
    fun deleteFileOrDirectory(target: String, remountAsRw: Boolean): Boolean {
        var result = true

        try {
            // mount destination as rw before writing to it
            if (remountAsRw) {
                RootTools.remount(target, "RW")
            }

            if (hasUtil("rm", "toolbox")) {
                RootTools.log("rm command is available!")

                val command = Command(0, false, "rm -r $target")
                Shell.startRootShell().add(command)
                commandWait(Shell.startRootShell(), command)

                if (command.exitCode != 0) {
                    RootTools.log("target not exist or unable to delete file")
                    result = false
                }
            } else {
                if (checkUtil("busybox") && hasUtil("rm", "busybox")) {
                    RootTools.log("busybox rm command is available!")

                    val command = Command(0, false, "busybox rm -rf $target")
                    Shell.startRootShell().add(command)
                    commandWait(Shell.startRootShell(), command)

                    if (command.exitCode != 0) {
                        RootTools.log("target not exist or unable to delete file")
                        result = false
                    }
                }
            }

            val command = Command(0, false, "rm -rf $target")
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)

            // mount destination back to ro
            if (remountAsRw) {
                RootTools.remount(target, "RO")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = false
        }

        return result
    }

    /**
     * This will try and fix a given binary. (This is for Busybox applets or Toolbox applets) By
     * "fix", I mean it will try and symlink the binary from either toolbox or Busybox and fix the
     * permissions if the permissions are not correct.
     *
     * @param util     Name of the utility to fix.
     * @param utilPath path to the toolbox that provides ln, rm, and chmod. This can be a blank string, a
     * path to a binary that will provide these, or you can use
     * RootTools.getWorkingToolbox()
     */
    fun fixUtil(util: String, utilPath: String?) {
        try {
            RootTools.remount("/system", "rw")

            val foundPaths = RootShell.findBinary(util, true)

            if (foundPaths.size > 0) {
                for (path in foundPaths) {
                    val command = Command(0, false, "$utilPath rm $path/$util")
                    RootShell.getShell(true).add(command)
                    commandWait(RootShell.getShell(true), command)

                }

                val command = Command(0, false, "$utilPath ln -s $utilPath /system/bin/$util", "$utilPath chmod 0755 /system/bin/$util")
                RootShell.getShell(true).add(command)
                commandWait(RootShell.getShell(true), command)

            }

            RootTools.remount("/system", "ro")
        } catch (e: Exception) {
        }

    }

    /**
     * This will check an array of binaries, determine if they exist and determine that it has
     * either the permissions 755, 775, or 777. If an applet is not setup correctly it will try and
     * fix it. (This is for Busybox applets or Toolbox applets)
     *
     * @param utils Name of the utility to check.
     * @return boolean to indicate whether the operation completed. Note that this is not indicative
     * of whether the problem was fixed, just that the method did not encounter any
     * exceptions.
     * @throws Exception if the operation cannot be completed.
     */
    @Throws(Exception::class)
    fun fixUtils(utils: Array<String>): Boolean {

        for (util in utils) {
            if (!checkUtil(util)) {
                if (checkUtil("busybox")) {
                    if (hasUtil(util, "busybox")) {
                        fixUtil(util, RootTools.utilPath)
                    }
                } else {
                    if (checkUtil("toolbox")) {
                        if (hasUtil(util, "toolbox")) {
                            fixUtil(util, RootTools.utilPath)
                        }
                    } else {
                        return false
                    }
                }
            }
        }

        return true
    }

    /**
     * This will return an List of Strings. Each string represents an applet available from BusyBox.
     *
     *
     *
     * @param path Path to the busybox binary that you want the list of applets from.
     * @return `null` If we cannot return the list of applets.
     */
    @Throws(Exception::class)
    fun getBusyBoxApplets(path: String?): List<String> {
        var path = path

        if (path != null && !path.endsWith("/") && path != "") {
            path += "/"
        } else if (path == null) {
            //Don't know what the user wants to do...what am I pshycic?
            throw Exception("Path is null, please specifiy a path")
        }

        val results = ArrayList<String>()

        var command: Command = object : Command(Constants.BBA, false, path + "busybox --list") {
            override fun commandOutput(id: Int, line: String?) {
                if (id == Constants.BBA) {
                    if (line!!.trim { it <= ' ' } != "" && !line.trim { it <= ' ' }.contains("not found") && !line.trim { it <= ' ' }.contains("file busy")) {
                        results.add(line)
                    }
                }

                super.commandOutput(id, line)
            }
        }

        //try without root first...
        RootShell.getShell(false).add(command)
        commandWait(RootShell.getShell(false), command)

        if (results.size <= 0) {
            //try with root...

            command = object : Command(Constants.BBA, false, path + "busybox --list") {
                override fun commandOutput(id: Int, line: String?) {
                    if (id == Constants.BBA) {
                        if (line!!.trim { it <= ' ' } != "" && !line.trim { it <= ' ' }.contains("not found") && !line.trim { it <= ' ' }.contains("file busy")) {
                            results.add(line)
                        }
                    }

                    super.commandOutput(id, line)
                }
            }

            RootShell.getShell(true).add(command)
            commandWait(RootShell.getShell(true), command)
        }

        return results
    }

    /**
     * @return BusyBox version if found, "" if not found.
     */
    fun getBusyBoxVersion(path: String): String {
        var path = path

        val version = StringBuilder()

        if (path != "" && !path.endsWith("/")) {
            path += "/"
        }

        try {
            var command: Command = object : Command(Constants.BBV, false, path + "busybox") {
                override fun commandOutput(id: Int, line: String?) {
                    var line = line
                    line = line!!.trim { it <= ' ' }

                    var foundVersion = false

                    if (id == Constants.BBV) {
                        RootTools.log("Version Output: $line")

                        val temp = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                        if (temp.size > 1 && temp[1].contains("v1.") && !foundVersion) {
                            foundVersion = true
                            version.append(temp[1])
                            RootTools.log("Found Version: $version")
                        }
                    }

                    super.commandOutput(id, line)
                }
            }

            //try without root first
            RootTools.log("Getting BusyBox Version without root")
            val shell = RootTools.getShell(false)
            shell.add(command)
            commandWait(shell, command)

            if (version.length <= 0) {

                command = object : Command(Constants.BBV, false, path + "busybox") {
                    override fun commandOutput(id: Int, line: String?) {
                        var line = line
                        line = line!!.trim { it <= ' ' }

                        var foundVersion = false

                        if (id == Constants.BBV) {
                            RootTools.log("Version Output: " + line!!)

                            val temp = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                            if (temp.size > 1 && temp[1].contains("v1.") && !foundVersion) {
                                foundVersion = true
                                version.append(temp[1])
                                RootTools.log("Found Version: $version")
                            }
                        }

                        super.commandOutput(id, line)
                    }
                }

                RootTools.log("Getting BusyBox Version with root")
                val rootShell = RootTools.getShell(true)
                //Now look for it...
                rootShell.add(command)
                commandWait(rootShell, command)
            }

        } catch (e: Exception) {
            RootTools.log("BusyBox was not found, more information MAY be available with Debugging on.")
            return ""
        }

        RootTools.log("Returning found version: $version")
        return version.toString()
    }

    /**
     * @return long Size, converted to kilobytes (from xxx or xxxm or xxxk etc.)
     */
    fun getConvertedSpace(spaceStr: String): Long {
        try {
            var multiplier = 1.0
            var c: Char
            val sb = StringBuffer()
            for (i in 0 until spaceStr.length) {
                c = spaceStr[i]
                if (!Character.isDigit(c) && c != '.') {
                    if (c == 'm' || c == 'M') {
                        multiplier = 1024.0
                    } else if (c == 'g' || c == 'G') {
                        multiplier = 1024.0 * 1024.0
                    }
                    break
                }
                sb.append(spaceStr[i])
            }
            return Math.ceil(java.lang.Double.valueOf(sb.toString()) * multiplier).toLong()
        } catch (e: Exception) {
            return -1
        }

    }

    /**
     * This method will return the inode number of a file. This method is dependent on having a version of
     * ls that supports the -i parameter.
     *
     * @param file path to the file that you wish to return the inode number
     * @return String The inode number for this file or "" if the inode number could not be found.
     */
    fun getInode(file: String): String {
        try {
            val command = object : Command(Constants.GI, false, "/data/local/ls -i $file") {

                override fun commandOutput(id: Int, line: String?) {
                    if (id == Constants.GI) {
                        if (line!!.trim { it <= ' ' } != "" && Character.isDigit(line.trim { it <= ' ' }.substring(0, 1).toCharArray()[0])) {
                            InternalVariables.inode = line.trim { it <= ' ' }.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                        }
                    }

                    super.commandOutput(id, line)
                }
            }
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)

            return InternalVariables.inode
        } catch (ignore: Exception) {
            return ""
        }

    }

    fun isNativeToolsReady(nativeToolsId: Int, context: Context): Boolean {
        RootTools.log("Preparing Native Tools")
        InternalVariables.nativeToolsReady = false

        val installer: Installer
        try {
            installer = Installer(context)
        } catch (ex: IOException) {
            if (RootTools.debugMode) {
                ex.printStackTrace()
            }
            return false
        }

        if (installer.isBinaryInstalled("nativetools")) {
            InternalVariables.nativeToolsReady = true
        } else {
            InternalVariables.nativeToolsReady = installer.installBinary(nativeToolsId,
                    "nativetools", "700")
        }
        return InternalVariables.nativeToolsReady
    }

    /**
     * @param file String that represent the file, including the full path to the
     * file and its name.
     * @return An instance of the class permissions from which you can get the
     * permissions of the file or if the file could not be found or
     * permissions couldn't be determined then permissions will be null.
     */
    fun getFilePermissionsSymlinks(file: String): Permissions? {
        RootTools.log("Checking permissions for $file")
        if (RootTools.exists(file)) {
            RootTools.log("$file was found.")
            try {

                val command = object : Command(
                        Constants.FPS, false, "ls -l $file",
                        "busybox ls -l $file",
                        "/system/bin/failsafe/toolbox ls -l $file",
                        "toolbox ls -l $file") {
                    override fun commandOutput(id: Int, line: String?) {
                        if (id == Constants.FPS) {
                            var symlink_final = ""

                            val lineArray = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                            if (lineArray[0].length != 10) {
                                super.commandOutput(id, line)
                                return
                            }

                            RootTools.log("Line $line")

                            try {
                                val symlink = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                if (symlink[symlink.size - 2] == "->") {
                                    RootTools.log("Symlink found.")
                                    symlink_final = symlink[symlink.size - 1]
                                }
                            } catch (e: Exception) {
                            }

                            try {
                                InternalVariables.permissions = getPermissions(line)
                                if (InternalVariables.permissions != null) {
                                    InternalVariables.permissions!!.symlink = symlink_final
                                }
                            } catch (e: Exception) {
                                RootTools.log(e.message)
                            }

                        }

                        super.commandOutput(id, line)
                    }
                }
                RootShell.getShell(true).add(command)
                commandWait(RootShell.getShell(true), command)

                return InternalVariables.permissions

            } catch (e: Exception) {
                RootTools.log(e.message)
                return null
            }

        }

        return null
    }

    /**
     * This will tell you how the specified mount is mounted. rw, ro, etc...
     *
     *
     *
     * @param path mount you want to check
     * @return `String` What the mount is mounted as.
     * @throws Exception if we cannot determine how the mount is mounted.
     */
    @Throws(Exception::class)
    fun getMountedAs(path: String): String {
        InternalVariables.mounts = mounts
        var mp: String
        if (InternalVariables.mounts != null) {
            for (mount in InternalVariables.mounts!!) {

                mp = mount.mountPoint.absolutePath

                if (mp == "/") {
                    return if (path == "/") {
                        mount.flags.toTypedArray()[0] as String
                    } else {
                        continue
                    }
                }

                if (path == mp || path.startsWith("$mp/")) {
                    RootTools.log(mount.flags.toTypedArray()[0] as String)
                    return mount.flags.toTypedArray()[0] as String
                }
            }

            throw Exception()
        } else {
            throw Exception()
        }
    }

    /**
     * Get the space for a desired partition.
     *
     * @param path The partition to find the space for.
     * @return the amount if space found within the desired partition. If the space was not found
     * then the value is -1
     * @throws TimeoutException
     */
    fun getSpace(path: String): Long {
        InternalVariables.getSpaceFor = path
        var found = false
        RootTools.log("Looking for Space")
        try {
            val command = object : Command(Constants.GS, false, "df $path") {

                override fun commandOutput(id: Int, line: String?) {
                    if (id == Constants.GS) {
                        if (line!!.contains(InternalVariables.getSpaceFor!!.trim { it <= ' ' })) {
                            InternalVariables.space = line.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        }
                    }

                    super.commandOutput(id, line)
                }
            }
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)

        } catch (e: Exception) {
        }

        if (InternalVariables.space != null) {
            RootTools.log("First Method")

            for (spaceSearch in InternalVariables.space!!) {

                RootTools.log(spaceSearch)

                if (found) {
                    return getConvertedSpace(spaceSearch)
                } else if (spaceSearch == "used,") {
                    found = true
                }
            }

            // Try this way
            var count = 0
            var targetCount = 3

            RootTools.log("Second Method")

            if (InternalVariables.space!![0].length <= 5) {
                targetCount = 2
            }

            for (spaceSearch in InternalVariables.space!!) {

                RootTools.log(spaceSearch)
                if (spaceSearch.length > 0) {
                    RootTools.log(spaceSearch + "Valid")
                    if (count == targetCount) {
                        return getConvertedSpace(spaceSearch)
                    }
                    count++
                }
            }
        }
        RootTools.log("Returning -1, space could not be determined.")
        return -1
    }

    /**
     * This will return a String that represent the symlink for a specified file.
     *
     *
     *
     * @param file file to get the Symlink for. (must have absolute path)
     * @return `String` a String that represent the symlink for a specified file or an
     * empty string if no symlink exists.
     */
    fun getSymlink(file: String): String {
        RootTools.log("Looking for Symlink for $file")

        try {
            val results = ArrayList<String>()

            val command = object : Command(Constants.GSYM, false, "ls -l $file") {

                override fun commandOutput(id: Int, line: String?) {
                    if (id == Constants.GSYM) {
                        if (line!!.trim { it <= ' ' } != "") {
                            results.add(line)
                        }
                    }

                    super.commandOutput(id, line)
                }
            }
            Shell.startRootShell().add(command)
            commandWait(Shell.startRootShell(), command)

            val symlink = results[0].split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (symlink.size > 2 && symlink[symlink.size - 2] == "->") {
                RootTools.log("Symlink found.")

                val final_symlink: String

                if (symlink[symlink.size - 1] != "" && !symlink[symlink.size - 1].contains("/")) {
                    //We assume that we need to get the path for this symlink as it is probably not absolute.
                    val paths = RootShell.findBinary(symlink[symlink.size - 1], true)
                    if (paths.size > 0) {
                        //We return the first found location.
                        final_symlink = paths[0] + symlink[symlink.size - 1]
                    } else {
                        //we couldnt find a path, return the symlink by itself.
                        final_symlink = symlink[symlink.size - 1]
                    }
                } else {
                    final_symlink = symlink[symlink.size - 1]
                }

                return final_symlink
            }
        } catch (e: Exception) {
            if (RootTools.debugMode) {
                e.printStackTrace()
            }
        }

        RootTools.log("Symlink not found")
        return ""
    }

    /**
     * This will return an ArrayList of the class Symlink. The class Symlink contains the following
     * property's: path SymplinkPath
     *
     *
     * These will provide you with any Symlinks in the given path.
     *
     * @param path path to search for Symlinks.
     * @return `ArrayList<Symlink></Symlink>` an ArrayList of the class Symlink.
     * @throws Exception if we cannot return the Symlinks.
     */
    @Throws(Exception::class)
    fun getSymlinks(path: String): ArrayList<Symlink> {

        // this command needs find
        if (!checkUtil("find")) {
            throw Exception()
        }

        InternalVariables.symlinks = ArrayList()

        val command = object : Command(0, false, "find $path -type l -exec ls -l {} \\;") {
            override fun commandOutput(id: Int, line: String?) {
                if (id == Constants.GET_SYMLINKS) {
                    RootTools.log(line)

                    val fields = line!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    InternalVariables.symlinks!!.add(Symlink(File(fields[fields.size - 3]), // file
                            File(fields[fields.size - 1]) // SymlinkPath
                    ))

                }

                super.commandOutput(id, line)
            }
        }
        Shell.startRootShell().add(command)
        commandWait(Shell.startRootShell(), command)

        return if (InternalVariables.symlinks != null) {
            InternalVariables.symlinks?: arrayListOf()
        } else {
            throw Exception()
        }
    }

    /**
     * Checks if there is enough Space on SDCard
     *
     * @param updateSize size to Check (long)
     * @return `true` if the Update will fit on SDCard, `false` if not enough
     * space on SDCard. Will also return `false`, if the SDCard is not mounted as
     * read/write
     */
    fun hasEnoughSpaceOnSdCard(updateSize: Long): Boolean {
        RootTools.log("Checking SDcard size and that it is mounted as RW")
        val status = Environment.getExternalStorageState()
        if (status != Environment.MEDIA_MOUNTED) {
            return false
        }
        val path = Environment.getExternalStorageDirectory()
        val stat = StatFs(path.path)
        var blockSize: Long = 0
        var availableBlocks: Long = 0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.blockSize.toLong()
            availableBlocks = stat.availableBlocks.toLong()
        } else {
            blockSize = stat.blockSizeLong
            availableBlocks = stat.availableBlocksLong
        }
        return updateSize < availableBlocks * blockSize
    }

    /**
     * Checks whether the toolbox or busybox binary contains a specific util
     *
     * @param util
     * @param box  Should contain "toolbox" or "busybox"
     * @return true if it contains this util
     */
    fun hasUtil(util: String, box: String): Boolean {

        InternalVariables.found = false

        // only for busybox and toolbox
        if (!(box.endsWith("toolbox") || box.endsWith("busybox"))) {
            return false
        }

        try {

            val command = object : Command(0, false, if (box.endsWith("toolbox")) "$box $util" else "$box --list") {

                override fun commandOutput(id: Int, line: String?) {
                    if (box.endsWith("toolbox")) {
                        if (!line!!.contains("no such tool")) {
                            InternalVariables.found = true
                        }
                    } else if (box.endsWith("busybox")) {
                        // go through all lines of busybox --list
                        if (line!!.contains(util)) {
                            RootTools.log("Found util!")
                            InternalVariables.found = true
                        }
                    }

                    super.commandOutput(id, line)
                }
            }
            RootTools.getShell(true).add(command)
            commandWait(RootTools.getShell(true), command)

            if (InternalVariables.found) {
                RootTools.log("Box contains $util util!")
                return true
            } else {
                RootTools.log("Box does not contain $util util!")
                return false
            }
        } catch (e: Exception) {
            RootTools.log(e.message)
            return false
        }

    }

    /**
     * This method can be used to unpack a binary from the raw resources folder and store it in
     * /data/data/app.package/files/ This is typically useful if you provide your own C- or
     * C++-based binary. This binary can then be executed using sendShell() and its full path.
     *
     * @param context  the current activity's `Context`
     * @param sourceId resource id; typically `R.raw.id`
     * @param destName destination file name; appended to /data/data/app.package/files/
     * @param mode     chmod value for this file
     * @return a `boolean` which indicates whether or not we were able to create the new
     * file.
     */
    fun installBinary(context: Context, sourceId: Int, destName: String, mode: String): Boolean {
        val installer: Installer

        try {
            installer = Installer(context)
        } catch (ex: IOException) {
            if (RootTools.debugMode) {
                ex.printStackTrace()
            }
            return false
        }

        return installer.installBinary(sourceId, destName, mode)
    }

    /**
     * This method checks whether a binary is installed.
     *
     * @param context    the current activity's `Context`
     * @param binaryName binary file name; appended to /data/data/app.package/files/
     * @return a `boolean` which indicates whether or not
     * the binary already exists.
     */
    fun isBinaryAvailable(context: Context, binaryName: String): Boolean {
        val installer: Installer

        try {
            installer = Installer(context)
        } catch (ex: IOException) {
            if (RootTools.debugMode) {
                ex.printStackTrace()
            }
            return false
        }

        return installer.isBinaryInstalled(binaryName)
    }

    /**
     * This will let you know if an applet is available from BusyBox
     *
     *
     *
     * @param applet The applet to check for.
     * @return `true` if applet is available, false otherwise.
     */
    fun isAppletAvailable(applet: String, binaryPath: String): Boolean {
        try {
            for (aplet in getBusyBoxApplets(binaryPath)) {
                if (aplet == applet) {
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            RootTools.log(e.toString())
            return false
        }

    }

    /**
     * This method can be used to to check if a process is running
     *
     * @param processName name of process to check
     * @return `true` if process was found
     * @throws TimeoutException (Could not determine if the process is running)
     */
    fun isProcessRunning(processName: String): Boolean {

        RootTools.log("Checks if process is running: $processName")

        InternalVariables.processRunning = false

        try {
            val command = object : Command(0, false, "ps") {
                override fun commandOutput(id: Int, line: String?) {
                    if (line?.contains(processName)==true) {
                        InternalVariables.processRunning = true
                    }

                    super.commandOutput(id, line)
                }
            }
            RootTools.getShell(true).add(command)
            commandWait(RootTools.getShell(true), command)

        } catch (e: Exception) {
            RootTools.log(e.message)
        }

        return InternalVariables.processRunning
    }

    /**
     * This method can be used to kill a running process
     *
     * @param processName name of process to kill
     * @return `true` if process was found and killed successfully
     */
    fun killProcess(processName: String): Boolean {
        RootTools.log("Killing process $processName")

        InternalVariables.pid_list = ""

        //Assume that the process is running
        InternalVariables.processRunning = true

        try {

            var command: Command = object : Command(0, false, "ps") {
                override fun commandOutput(id: Int, line: String?) {
                    if (line?.contains(processName)==true) {
                        val psMatcher = InternalVariables.psPattern.matcher(line)

                        try {
                            if (psMatcher.find()) {
                                val pid = psMatcher.group(1)

                                InternalVariables.pid_list += " $pid"
                                InternalVariables.pid_list = InternalVariables.pid_list.trim { it <= ' ' }

                                RootTools.log("Found pid: $pid")
                            } else {
                                RootTools.log("Matching in ps command failed!")
                            }
                        } catch (e: Exception) {
                            RootTools.log("Error with regex!")
                            e.printStackTrace()
                        }

                    }

                    super.commandOutput(id, line)
                }
            }
            RootTools.getShell(true).add(command)
            commandWait(RootTools.getShell(true), command)

            // get all pids in one string, created in process method
            val pids = InternalVariables.pid_list

            // kill processes
            if (pids != "") {
                try {
                    // example: kill -9 1234 1222 5343
                    command = Command(0, false, "kill -9 $pids")
                    RootTools.getShell(true).add(command)
                    commandWait(RootTools.getShell(true), command)

                    return true
                } catch (e: Exception) {
                    RootTools.log(e.message)
                }

            } else {
                //no pids match, must be dead
                return true
            }
        } catch (e: Exception) {
            RootTools.log(e.message)
        }

        return false
    }

    /**
     * This will launch the Android market looking for BusyBox
     *
     * @param activity pass in your Activity
     */
    fun offerBusyBox(activity: Activity) {
        RootTools.log("Launching Market for BusyBox")
        val i = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=stericson.busybox"))
        activity.startActivity(i)
    }

    /**
     * This will launch the Android market looking for BusyBox, but will return the intent fired and
     * starts the activity with startActivityForResult
     *
     * @param activity    pass in your Activity
     * @param requestCode pass in the request code
     * @return intent fired
     */
    fun offerBusyBox(activity: Activity, requestCode: Int): Intent {
        RootTools.log("Launching Market for BusyBox")
        val i = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=stericson.busybox"))
        activity.startActivityForResult(i, requestCode)
        return i
    }

    /**
     * This will launch the Play Store looking for SuperUser
     *
     * @param activity pass in your Activity
     */
    fun offerSuperUser(activity: Activity) {
        RootTools.log("Launching Play Store for SuperSU")
        val i = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=eu.chainfire.supersu"))
        activity.startActivity(i)
    }

    /**
     * This will launch the Play Store looking for SuperSU, but will return the intent fired
     * and starts the activity with startActivityForResult
     *
     * @param activity    pass in your Activity
     * @param requestCode pass in the request code
     * @return intent fired
     */
    fun offerSuperUser(activity: Activity, requestCode: Int): Intent {
        RootTools.log("Launching Play Store for SuperSU")
        val i = Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=eu.chainfire.supersu"))
        activity.startActivityForResult(i, requestCode)
        return i
    }

    @Throws(Exception::class)
    private fun commandWait(shell: Shell, cmd: Command) {

        while (!cmd.isFinished) {

            RootTools.log(Constants.TAG, shell.getCommandQueuePositionString(cmd))
            RootTools.log(Constants.TAG, "Processed " + cmd.totalOutputProcessed + " of " + cmd.totalOutput + " output from command.")

            synchronized(cmd) {
                try {
                    if (!cmd.isFinished) {
                        cmd.p_wait(2000)
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }

            if (!cmd.isExecuting && !cmd.isFinished) {
                if (!shell.isExecuting && !shell.isReading) {
                    Log.e(Constants.TAG, "Waiting for a command to be executed in a shell that is not executing and not reading! \n\n Command: " + cmd.command)
                    val e = Exception()
                    e.stackTrace = Thread.currentThread().stackTrace
                    e.printStackTrace()
                } else if (shell.isExecuting && !shell.isReading) {
                    Log.e(Constants.TAG, "Waiting for a command to be executed in a shell that is executing but not reading! \n\n Command: " + cmd.command)
                    val e = Exception()
                    e.stackTrace = Thread.currentThread().stackTrace
                    e.printStackTrace()
                } else {
                    Log.e(Constants.TAG, "Waiting for a command to be executed in a shell that is not reading! \n\n Command: " + cmd.command)
                    val e = Exception()
                    e.stackTrace = Thread.currentThread().stackTrace
                    e.printStackTrace()
                }
            }

        }
    }

    companion object {

        fun getInstance() {
            //this will allow RootTools to be the only one to get an instance of this class.
            RootTools.setRim(RootToolsInternalMethods())
        }
    }
}
