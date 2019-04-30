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

package com.stericson.rootools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log

import com.stericson.rootools.containers.Mount
import com.stericson.rootools.containers.Permissions
import com.stericson.rootools.containers.Symlink
import com.stericson.rootools.internal.Remounter
import com.stericson.rootools.internal.RootToolsInternalMethods
import com.stericson.rootools.internal.Runner
import com.stericson.rootshell.RootShell
import com.stericson.rootshell.exceptions.RootDeniedException
import com.stericson.rootshell.execution.Command
import com.stericson.rootshell.execution.Shell

import java.io.IOException
import java.util.ArrayList
import java.util.Arrays
import java.util.concurrent.TimeoutException

object RootTools {

    /**
     * This class is the gateway to every functionality within the RootTools library.The developer
     * should only have access to this class and this class only.This means that this class should
     * be the only one to be public.The rest of the classes within this library must not have the
     * public modifier.
     *
     *
     * All methods and Variables that the developer may need to have access to should be here.
     *
     *
     * If a method, or a specific functionality, requires a fair amount of code, or work to be done,
     * then that functionality should probably be moved to its own class and the call to it done
     * here.For examples of this being done, look at the remount functionality.
     */

    private var rim: RootToolsInternalMethods? = null

    private val internals: RootToolsInternalMethods?
        get() {
            if (rim == null) {
                RootToolsInternalMethods.getInstance()
                return rim
            } else {
                return rim
            }
        }

    // --------------------
    // # Public Variables #
    // --------------------

    var debugMode = false
    var utilPath: String? = null

    /**
     * Setting this to false will disable the handler that is used
     * by default for the 3 callback methods for Command.
     *
     *
     * By disabling this all callbacks will be called from a thread other than
     * the main UI thread.
     */
    var handlerEnabled = true


    /**
     * Setting this will change the default command timeout.
     *
     *
     * The default is 20000ms
     */
    var default_Command_Timeout = 20000

    /**
     * @return BusyBox version is found, "" if not found.
     */
    val busyBoxVersion: String
        get() = RootTools.getBusyBoxVersion("")

    /**
     * This will return an List of Strings. Each string represents an applet available from BusyBox.
     *
     *
     *
     * @return `null` If we cannot return the list of applets.
     */
    val busyBoxApplets: List<String>
        @Throws(Exception::class)
        get() = RootTools.getBusyBoxApplets("")

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
    val mounts: ArrayList<Mount>
        @Throws(Exception::class)
        get() = internals!!.mounts

    /**
     * This will return the environment variable PATH
     *
     * @return `List<String></String>` A List of Strings representing the environment variable $PATH
     */
    val path: List<String>
        get() = Arrays.asList(*System.getenv("PATH")!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())

    /**
     * This will return to you a string to be used in your shell commands which will represent the
     * valid working toolbox with correct permissions. For instance, if Busybox is available it will
     * return "busybox", if busybox is not available but toolbox is then it will return "toolbox"
     *
     * @return String that indicates the available toolbox to use for accessing applets.
     */
    val workingToolbox: String
        get() = internals!!.workingToolbox

    /**
     * @return `true` if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    val isAccessGiven: Boolean
        get() = RootShell.isAccessGiven

    /**
     * @return `true` if BusyBox was found.
     */
    val isBusyboxAvailable: Boolean
        get() = RootShell.isBusyboxAvailable

    /**
     * @return `true` if su was found.
     */
    val isRootAvailable: Boolean
        get() = RootShell.isRootAvailable

    fun setRim(rim: RootToolsInternalMethods) {
        RootTools.rim = rim
    }


    // ---------------------------
    // # Public Variable Getters #
    // ---------------------------

    // ------------------
    // # Public Methods #
    // ------------------

    /**
     * This will check a given binary, determine if it exists and determine that it has either the
     * permissions 755, 775, or 777.
     *
     * @param util Name of the utility to check.
     * @return boolean to indicate whether the binary is installed and has appropriate permissions.
     */
    fun checkUtil(util: String): Boolean {

        return internals!!.checkUtil(util)
    }

    /**
     * This will close all open shells.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun closeAllShells() {
        RootShell.closeAllShells()
    }

    /**
     * This will close the custom shell that you opened.
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    fun closeCustomShell() {
        RootShell.closeCustomShell()
    }

    /**
     * This will close either the root shell or the standard shell depending on what you specify.
     *
     * @param root a `boolean` to specify whether to close the root shell or the standard shell.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun closeShell(root: Boolean) {
        RootShell.closeShell(root)
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
                 preserveFileAttributes: Boolean): Boolean {
        return internals!!.copyFile(source, destination, remountAsRw, preserveFileAttributes, 644)
    }

    /**
     * Deletes a file or directory
     *
     * @param target      example: /data/data/org.adaway/files/hosts
     * @param remountAsRw remounts the destination as read/write before writing to it
     * @return true if it was successfully deleted
     */
    fun deleteFileOrDirectory(target: String, remountAsRw: Boolean): Boolean {
        return internals!!.deleteFileOrDirectory(target, remountAsRw)
    }

    fun mkdir(path: String, remountAsRW: Boolean, mode: Int): Boolean {
        return internals!!.mkdir(path, remountAsRW, mode)
    }

    /**
     * Use this to check whether or not a file OR directory exists on the filesystem.
     *
     * @param file  String that represent the file OR the directory, including the full path to the
     * file and its name.
     * @param isDir boolean that represent whether or not we are looking for a directory
     * @return a boolean that will indicate whether or not the file exists.
     */
    @JvmOverloads
    fun exists(file: String, isDir: Boolean = false): Boolean {
        return RootShell.exists(file, isDir)
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
    fun fixUtil(util: String, utilPath: String) {
        internals!!.fixUtil(util, utilPath)
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
        return internals!!.fixUtils(utils)
    }

    /**
     * @param binaryName String that represent the binary to find.
     * @param singlePath boolean that represents whether to return a single path or multiple.
     * @return `List<String></String>` containing the paths the binary was found at.
     */
    fun findBinary(binaryName: String, singlePath: Boolean): List<String> {
        return RootShell.findBinary(binaryName, singlePath)
    }

    /**
     * @param path String that represents the path to the Busybox binary you want to retrieve the version of.
     * @return BusyBox version is found, "" if not found.
     */
    fun getBusyBoxVersion(path: String): String {
        return internals!!.getBusyBoxVersion(path)
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
    fun getBusyBoxApplets(path: String): List<String> {
        return internals!!.getBusyBoxApplets(path)
    }

    /**
     * This will open or return, if one is already open, a custom shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param shellPath a `String` to Indicate the path to the shell that you want to open.
     * @param timeout   an `int` to Indicate the length of time before giving up on opening a shell.
     * @throws TimeoutException
     * @throws com.stericson.rootshell.exceptions.RootDeniedException
     * @throws IOException
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getCustomShell(shellPath: String, timeout: Int): Shell {
        return RootShell.getCustomShell(shellPath, timeout)
    }

    /**
     * This will open or return, if one is already open, a custom shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param shellPath a `String` to Indicate the path to the shell that you want to open.
     * @throws TimeoutException
     * @throws com.stericson.rootshell.exceptions.RootDeniedException
     * @throws IOException
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getCustomShell(shellPath: String): Shell {
        return RootTools.getCustomShell(shellPath, 10000)
    }

    /**
     * @param file String that represent the file, including the full path to the file and its name.
     * @return An instance of the class permissions from which you can get the permissions of the
     * file or if the file could not be found or permissions couldn't be determined then
     * permissions will be null.
     */
    fun getFilePermissionsSymlinks(file: String): Permissions? {
        return internals!!.getFilePermissionsSymlinks(file)
    }

    /**
     * This method will return the inode number of a file. This method is dependent on having a version of
     * ls that supports the -i parameter.
     *
     * @param file path to the file that you wish to return the inode number
     * @return String The inode number for this file or "" if the inode number could not be found.
     */
    fun getInode(file: String): String {
        return internals!!.getInode(file)
    }

    /**
     * This will tell you how the specified mount is mounted. rw, ro, etc...
     *
     *
     *
     * @param path The mount you want to check
     * @return `String` What the mount is mounted as.
     * @throws Exception if we cannot determine how the mount is mounted.
     */
    @Throws(Exception::class)
    fun getMountedAs(path: String): String {
        return internals!!.getMountedAs(path)
    }

    /**
     * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param root         a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
     * @param timeout      an `int` to Indicate the length of time to wait before giving up on opening a shell.
     * @param shellContext the context to execute the shell with
     * @param retry        a `int` to indicate how many times the ROOT shell should try to open with root priviliges...
     * @throws TimeoutException
     * @throws com.stericson.rootshell.exceptions.RootDeniedException
     * @throws IOException
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    @JvmOverloads
    fun getShell(root: Boolean, timeout: Int, shellContext: Shell.ShellContext = Shell.defaultContext, retry: Int = 3): Shell {
        return RootShell.getShell(root, timeout, shellContext, retry)
    }

    /**
     * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param root         a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
     * @param shellContext the context to execute the shell with
     * @throws TimeoutException
     * @throws com.stericson.rootshell.exceptions.RootDeniedException
     * @throws IOException
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getShell(root: Boolean, shellContext: Shell.ShellContext): Shell {
        return getShell(root, 0, shellContext, 3)
    }

    /**
     * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
     * and for closing the shell when you are done using it.
     *
     * @param root a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
     * @throws TimeoutException
     * @throws com.stericson.rootshell.exceptions.RootDeniedException
     * @throws IOException
     */
    @Throws(IOException::class, TimeoutException::class, RootDeniedException::class)
    fun getShell(root: Boolean): Shell {
        return RootTools.getShell(root, 0)
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
        return internals!!.getSpace(path)
    }

    /**
     * This will return a String that represent the symlink for a specified file.
     *
     *
     *
     * @param file path to the file to get the Symlink for. (must have absolute path)
     * @return `String` a String that represent the symlink for a specified file or an
     * empty string if no symlink exists.
     */
    fun getSymlink(file: String): String {
        return internals!!.getSymlink(file)
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
        return internals!!.getSymlinks(path)
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
        return internals!!.hasEnoughSpaceOnSdCard(updateSize)
    }

    /**
     * Checks whether the toolbox or busybox binary contains a specific util
     *
     * @param util
     * @param box  Should contain "toolbox" or "busybox"
     * @return true if it contains this util
     */
    fun hasUtil(util: String, box: String): Boolean {
        //TODO Convert this to use the new shell.
        return internals!!.hasUtil(util, box)
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
        return internals!!.installBinary(context, sourceId, destName, mode)
    }

    /**
     * This method can be used to unpack a binary from the raw resources folder and store it in
     * /data/data/app.package/files/ This is typically useful if you provide your own C- or
     * C++-based binary. This binary can then be executed using sendShell() and its full path.
     *
     * @param context    the current activity's `Context`
     * @param sourceId   resource id; typically `R.raw.id`
     * @param binaryName destination file name; appended to /data/data/app.package/files/
     * @return a `boolean` which indicates whether or not we were able to create the new
     * file.
     */
    fun installBinary(context: Context, sourceId: Int, binaryName: String): Boolean {
        return installBinary(context, sourceId, binaryName, "700")
    }

    /**
     * This method checks whether a binary is installed.
     *
     * @param context    the current activity's `Context`
     * @param binaryName binary file name; appended to /data/data/app.package/files/
     * @return a `boolean` which indicates whether or not
     * the binary already exists.
     */
    fun hasBinary(context: Context, binaryName: String): Boolean {
        return internals!!.isBinaryAvailable(context, binaryName)
    }

    /**
     * This will let you know if an applet is available from BusyBox
     *
     *
     *
     * @param applet The applet to check for.
     * @param path   Path to the busybox binary that you want to check. (do not include binary name)
     * @return `true` if applet is available, false otherwise.
     */
    fun isAppletAvailable(applet: String, path: String): Boolean {
        return internals!!.isAppletAvailable(applet, path)
    }

    /**
     * This will let you know if an applet is available from BusyBox
     *
     *
     *
     * @param applet The applet to check for.
     * @return `true` if applet is available, false otherwise.
     */
    fun isAppletAvailable(applet: String): Boolean {
        return RootTools.isAppletAvailable(applet, "")
    }

    /**
     * Control how many time of retries should request
     *
     * @param timeout The timeout
     * @param retries The number of retries
     * @return `true` if your app has been given root access.
     * @throws TimeoutException if this operation times out. (cannot determine if access is given)
     */
    fun isAccessGiven(timeout: Int, retries: Int): Boolean {
        return RootShell.isAccessGiven/*timeout, retries FIXME*/
    }

    fun isNativeToolsReady(nativeToolsId: Int, context: Context): Boolean {
        return internals!!.isNativeToolsReady(nativeToolsId, context)
    }

    /**
     * This method can be used to to check if a process is running
     *
     * @param processName name of process to check
     * @return `true` if process was found
     * @throws TimeoutException (Could not determine if the process is running)
     */
    fun isProcessRunning(processName: String): Boolean {
        //TODO convert to new shell
        return internals!!.isProcessRunning(processName)
    }

    /**
     * This method can be used to kill a running process
     *
     * @param processName name of process to kill
     * @return `true` if process was found and killed successfully
     */
    fun killProcess(processName: String): Boolean {
        //TODO convert to new shell
        return internals!!.killProcess(processName)
    }

    /**
     * This will launch the Android market looking for BusyBox
     *
     * @param activity pass in your Activity
     */
    fun offerBusyBox(activity: Activity) {
        internals!!.offerBusyBox(activity)
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
        return internals!!.offerBusyBox(activity, requestCode)
    }

    /**
     * This will launch the Android market looking for SuperUser
     *
     * @param activity pass in your Activity
     */
    fun offerSuperUser(activity: Activity) {
        internals!!.offerSuperUser(activity)
    }

    /**
     * This will launch the Android market looking for SuperUser, but will return the intent fired
     * and starts the activity with startActivityForResult
     *
     * @param activity    pass in your Activity
     * @param requestCode pass in the request code
     * @return intent fired
     */
    fun offerSuperUser(activity: Activity, requestCode: Int): Intent {
        return internals!!.offerSuperUser(activity, requestCode)
    }

    /**
     * This will take a path, which can contain the file name as well, and attempt to remount the
     * underlying partition.
     *
     *
     * For example, passing in the following string:
     * "/system/bin/some/directory/that/really/would/never/exist" will result in /system ultimately
     * being remounted. However, keep in mind that the longer the path you supply, the more work
     * this has to do, and the slower it will run.
     *
     * @param file      file path
     * @param mountType mount type: pass in RO (Read only) or RW (Read Write)
     * @return a `boolean` which indicates whether or not the partition has been
     * remounted as specified.
     */
    fun remount(file: String, mountType: String): Boolean {
        // Recieved a request, get an instance of Remounter
        val remounter = Remounter()
        // send the request.
        return remounter.remount(file, mountType)
    }

    /**
     * This restarts only Android OS without rebooting the whole device. This does NOT work on all
     * devices. This is done by killing the main init process named zygote. Zygote is restarted
     * automatically by Android after killing it.
     *
     * @throws TimeoutException
     */
    fun restartAndroid() {
        RootTools.log("Restart Android")
        killProcess("zygote")
    }

    /**
     * Executes binary in a separated process. Before using this method, the binary has to be
     * installed in /data/data/app.package/files/ using the installBinary method.
     *
     * @param context    the current activity's `Context`
     * @param binaryName name of installed binary
     * @param parameter  parameter to append to binary like "-vxf"
     */
    fun runBinary(context: Context, binaryName: String, parameter: String) {
        val runner = Runner(context, binaryName, parameter)
        runner.start()
    }

    /**
     * Executes a given command with root access or without depending on the value of the boolean passed.
     * This will also start a root shell or a standard shell without you having to open it specifically.
     *
     *
     * You will still need to close the shell after you are done using the shell.
     *
     * @param shell   The shell to execute the command on, this can be a root shell or a standard shell.
     * @param command The command to execute in the shell
     * @throws IOException
     */
    @Throws(IOException::class)
    fun runShellCommand(shell: Shell, command: Command) {
        shell.add(command)
    }

    /**
     * This method allows you to output debug messages only when debugging is on. This will allow
     * you to add a debug option to your app, which by default can be left off for performance.
     * However, when you need debugging information, a simple switch can enable it and provide you
     * with detailed logging.
     *
     *
     * This method handles whether or not to log the information you pass it depending whether or
     * not RootTools.debugMode is on. So you can use this and not have to worry about handling it
     * yourself.
     *
     * @param msg The message to output.
     */
    fun log(msg: String?) {
        log(null, msg?:"", 3, null)
    }

    /**
     * This method allows you to output debug messages only when debugging is on. This will allow
     * you to add a debug option to your app, which by default can be left off for performance.
     * However, when you need debugging information, a simple switch can enable it and provide you
     * with detailed logging.
     *
     *
     * This method handles whether or not to log the information you pass it depending whether or
     * not RootTools.debugMode is on. So you can use this and not have to worry about handling it
     * yourself.
     *
     * @param msg  The message to output.
     * @param type The type of log, 1 for verbose, 2 for error, 3 for debug
     * @param e    The exception that was thrown (Needed for errors)
     */
    fun log(msg: String?, type: Int, e: Exception) {
        log(null, msg?:"", type, e)
    }

    /**
     * This method allows you to check whether logging is enabled.
     * Yes, it has a goofy name, but that's to keep it as short as possible.
     * After all writing logging calls should be painless.
     * This method exists to save Android going through the various Java layers
     * that are traversed any time a string is created (i.e. what you are logging)
     *
     *
     * Example usage:
     * if(islog) {
     * StrinbBuilder sb = new StringBuilder();
     * // ...
     * // build string
     * // ...
     * log(sb.toString());
     * }
     *
     * @return true if logging is enabled
     */
    fun islog(): Boolean {
        return debugMode
    }

    /**
     * This method allows you to output debug messages only when debugging is on. This will allow
     * you to add a debug option to your app, which by default can be left off for performance.
     * However, when you need debugging information, a simple switch can enable it and provide you
     * with detailed logging.
     *
     *
     * This method handles whether or not to log the information you pass it depending whether or
     * not RootTools.debugMode is on. So you can use this and not have to worry about handling it
     * yourself.
     *
     * @param TAG  Optional parameter to define the tag that the Log will use.
     * @param msg  The message to output.
     * @param type The type of log, 1 for verbose, 2 for error, 3 for debug
     * @param e    The exception that was thrown (Needed for errors)
     */
    @JvmOverloads
    fun log(TAG: String?, msg: String?, type: Int = 3, e: Exception? = null) {
        var TAG = TAG
        if (msg != null && msg != "") {
            if (debugMode) {
                if (TAG == null) {
                    TAG = Constants.TAG
                }

                when (type) {
                    1 -> Log.v(TAG, msg)
                    2 -> Log.e(TAG, msg, e)
                    3 -> Log.d(TAG, msg)
                }
            }
        }
    }
}
/**
 * Use this to check whether or not a file exists on the filesystem.
 *
 * @param file String that represent the file, including the full path to the
 * file and its name.
 * @return a boolean that will indicate whether or not the file exists.
 */
/**
 * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
 * and for closing the shell when you are done using it.
 *
 * @param root         a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
 * @param timeout      an `int` to Indicate the length of time to wait before giving up on opening a shell.
 * @param shellContext the context to execute the shell with
 * @throws TimeoutException
 * @throws com.stericson.rootshell.exceptions.RootDeniedException
 * @throws IOException
 */
/**
 * This will open or return, if one is already open, a shell, you are responsible for managing the shell, reading the output
 * and for closing the shell when you are done using it.
 *
 * @param root    a `boolean` to Indicate whether or not you want to open a root shell or a standard shell
 * @param timeout an `int` to Indicate the length of time to wait before giving up on opening a shell.
 * @throws TimeoutException
 * @throws com.stericson.rootshell.exceptions.RootDeniedException
 * @throws IOException
 */
/**
 * This method allows you to output debug messages only when debugging is on. This will allow
 * you to add a debug option to your app, which by default can be left off for performance.
 * However, when you need debugging information, a simple switch can enable it and provide you
 * with detailed logging.
 *
 *
 * This method handles whether or not to log the information you pass it depending whether or
 * not RootTools.debugMode is on. So you can use this and not have to worry about handling it
 * yourself.
 *
 * @param TAG Optional parameter to define the tag that the Log will use.
 * @param msg The message to output.
 */
