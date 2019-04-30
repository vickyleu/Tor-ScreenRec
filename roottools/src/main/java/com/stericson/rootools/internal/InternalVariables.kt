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

import java.util.ArrayList
import java.util.regex.Pattern

import com.stericson.rootools.containers.Mount
import com.stericson.rootools.containers.Permissions
import com.stericson.rootools.containers.Symlink

object InternalVariables {

    // ----------------------
    // # Internal Variables #
    // ----------------------


    internal var nativeToolsReady = false
    internal var found = false
    internal var processRunning = false

    internal var space: Array<String>? = null
    internal var getSpaceFor: String? = null
    internal var busyboxVersion: String? = null
    internal var pid_list = ""
    internal var mounts: ArrayList<Mount>? = null
    internal var symlinks: ArrayList<Symlink>? = null
    internal var inode = ""
    internal var permissions: Permissions? = null

    // regex to get pid out of ps line, example:
    // root 2611 0.0 0.0 19408 2104 pts/2 S 13:41 0:00 bash
    internal val PS_REGEX = "^\\S+\\s+([0-9]+).*$"
    internal var psPattern: Pattern

    init {
        psPattern = Pattern.compile(PS_REGEX)
    }
}
