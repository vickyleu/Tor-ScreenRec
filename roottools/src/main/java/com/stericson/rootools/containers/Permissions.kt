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

package com.stericson.rootools.containers

class Permissions {
    var type: String?=null
    var userPermissions: String?=null
    var groupPermissions: String?=null
    var otherPermissions: String?=null
    var symlink: String?=null
    var permissions: Int = 0

    fun getUser(): String {
        return userPermissions?:""
    }

    fun setUser(user: String) {
        this.userPermissions = user
    }

    fun getGroup(): String {
        return groupPermissions?:""
    }

    fun setGroup(group: String) {
        this.groupPermissions = group
    }

    fun getOther(): String {
        return otherPermissions?:""
    }

    fun setOther(other: String) {
        this.otherPermissions = other
    }


}
