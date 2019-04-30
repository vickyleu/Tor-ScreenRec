/*
 * Copyright (c) 2016 Nick Guo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.tornaco.torscreenrec.util

import java.text.DecimalFormat

object MiscUtils {

    fun formatFileSize(size: Long): String {
        val df = DecimalFormat("#.00")
        val fileSizeString: String
        if (size < 1024) {
            fileSizeString = df.format(size.toDouble()) + "B"
        } else if (size < 1048576) {
            fileSizeString = df.format(size.toDouble() / 1024) + "K"
        } else if (size < 1073741824) {
            fileSizeString = df.format(size.toDouble() / 1048576) + "M"
        } else {
            fileSizeString = df.format(size.toDouble() / 1073741824) + "G"
        }
        return fileSizeString
    }
}
