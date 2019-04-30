/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nick.tiles.tile

import android.content.res.Resources
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.widget.TextView

import java.util.ArrayList

class Category {
    var id = CAT_ID_UNDEFINED
    /**
     * Resource ID of title of the category that is shown to the user.
     */
    var titleRes: Int = 0
    /**
     * Title of the category that is shown to the user.
     */
    var title: CharSequence?=null
    var summary: CharSequence? = null
    var summaryRes: Int = 0
    /**
     * List of the category's children
     */
    var tiles: MutableList<Tile> = ArrayList()

    val tilesCount: Int
        get() = tiles.size

    constructor() {
        // Empty
    }

    internal constructor(`in`: Parcel) {
        readFromParcel(`in`)
    }

    fun addTile(tile: Tile) {
        tiles.add(tile)
    }

    fun addTile(n: Int, tile: Tile) {
        tiles.add(n, tile)
    }

    fun removeTile(tile: Tile) {
        tiles.remove(tile)
    }

    fun removeTile(n: Int) {
        tiles.removeAt(n)
    }

    fun getTile(n: Int): Tile {
        return tiles[n]
    }

    /**
     * Return the currently set title.  If [.titleRes] is set,
     * this resource is loaded from <var>res</var> and returned.  Otherwise
     * [.title] is returned.
     */
    fun getTitle(res: Resources): CharSequence {
        return if (titleRes != 0) {
            res.getText(titleRes)
        } else title?:""
    }

    fun getSummary(res: Resources): CharSequence? {
        return if (summaryRes != 0) {
            res.getText(summaryRes)
        } else summary
    }

    fun onSummarySet(view: TextView) {

    }

    fun describeContents(): Int {
        return 0
    }

    fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(titleRes)
        TextUtils.writeToParcel(title, dest, flags)

        val count = tiles.size
        dest.writeInt(count)

        for (n in 0 until count) {
            val tile = tiles[n]
            tile.writeToParcel(dest, flags)
        }
    }

    fun readFromParcel(`in`: Parcel) {
        titleRes = `in`.readInt()
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)

        val count = `in`.readInt()

        for (n in 0 until count) {
            val tile = Tile.CREATOR.createFromParcel(`in`)
            tiles.add(tile)
        }
    }

    companion object {

        val CAT_ID_UNDEFINED: Long = -1
        val CREATOR: Parcelable.Creator<Category> = object : Parcelable.Creator<Category> {
            override fun createFromParcel(source: Parcel): Category {
                return Category(source)
            }

            override fun newArray(size: Int): Array<Category?> {
                return arrayOfNulls(size)
            }
        }
    }
}
