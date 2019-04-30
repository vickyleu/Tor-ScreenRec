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

import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils

/**
 * Description of a single dashboard tile that the user can select.
 */
open class Tile {
    /**
     * Identifier for this tile, to correlate with a new list when
     * it is updated.  The default value is
     * [Tile.TILE_ID_UNDEFINED], meaning no id.
     *
     * @attr ref android.R.styleable#PreferenceHeader_id
     */
    var id = TILE_ID_UNDEFINED
    /**
     * Resource ID of title of the tile that is shown to the user.
     *
     * @attr ref android.R.styleable#PreferenceHeader_title
     */
    var titleRes: Int = 0
    /**
     * Title of the tile that is shown to the user.
     *
     * @attr ref android.R.styleable#PreferenceHeader_title
     */
    var title: CharSequence = ""
    /**
     * Resource ID of optional summary describing what this tile controls.
     *
     * @attr ref android.R.styleable#PreferenceHeader_summary
     */
    var summaryRes: Int = 0
    /**
     * Optional summary describing what this tile controls.
     *
     * @attr ref android.R.styleable#PreferenceHeader_summary
     */
    var summary: CharSequence = ""
    /**
     * Optional icon resource to show for this tile.
     *
     * @attr ref android.R.styleable#PreferenceHeader_icon
     */
    var iconRes: Int = 0

    var iconDrawable: Drawable? = null

    /**
     * Full class name of the fragment to display when this tile is
     * selected.
     *
     * @attr ref android.R.styleable#PreferenceHeader_fragment
     */
    var fragment: String? = null
    /**
     * Optional arguments to supply to the fragment when it is
     * instantiated.
     */
    var fragmentArguments: Bundle? = null
    /**
     * Intent to launch when the preference is selected.
     */
    var intent: Intent? = null
    /**
     * Optional additional data for use by subclasses of the activity
     */
    var extras: Bundle? = null
    var tileView: TileView? = null

    constructor() {
        // Empty
    }

    internal constructor(`in`: Parcel) {
        readFromParcel(`in`)
    }

    /**
     * Return the currently set title.  If [.titleRes] is set,
     * this resource is loaded from <var>res</var> and returned.  Otherwise
     * [.title] is returned.
     */
    fun getTitle(res: Resources): CharSequence {
        return if (titleRes != 0) {
            res.getText(titleRes)
        } else title
    }

    /**
     * Return the currently set summary.  If [.summaryRes] is set,
     * this resource is loaded from <var>res</var> and returned.  Otherwise
     * [.summary] is returned.
     */
    fun getSummary(res: Resources): CharSequence {
        return if (summaryRes != 0) {
            res.getText(summaryRes)
        } else summary
    }

    fun describeContents(): Int {
        return 0
    }

    fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeInt(titleRes)
        TextUtils.writeToParcel(title, dest, flags)
        dest.writeInt(summaryRes)
        TextUtils.writeToParcel(summary, dest, flags)
        dest.writeInt(iconRes)
        dest.writeString(fragment)
        dest.writeBundle(fragmentArguments)
        if (intent != null) {
            dest.writeInt(1)
            intent!!.writeToParcel(dest, flags)
        } else {
            dest.writeInt(0)
        }
        dest.writeBundle(extras)
    }

    fun readFromParcel(`in`: Parcel) {
        id = `in`.readInt()
        titleRes = `in`.readInt()
        title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        summaryRes = `in`.readInt()
        summary = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(`in`)
        iconRes = `in`.readInt()
        fragment = `in`.readString()
        fragmentArguments = `in`.readBundle()
        if (`in`.readInt() != 0) {
            intent = Intent.CREATOR.createFromParcel(`in`)
        }
        extras = `in`.readBundle()
    }

    companion object {
        /**
         * Default value for [DashboardTile.id][Tile.id]
         * indicating that no identifier value is set.  All other values (including those below -1)
         * are valid.
         */
        val TILE_ID_UNDEFINED = -1
        val CREATOR: Parcelable.Creator<Tile> = object : Parcelable.Creator<Tile> {
            override fun createFromParcel(source: Parcel): Tile {
                return Tile(source)
            }

            override fun newArray(size: Int): Array<Tile?> {
                return arrayOfNulls(size)
            }
        }
    }
}
