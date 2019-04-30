/*
 * Copyright (C) 2013 The Android Open Source Project
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

package dev.nick.tiles.widget

import android.content.Context
import androidx.appcompat.widget.SwitchCompat
import android.util.AttributeSet

class ToggleSwitch : SwitchCompat {

    private var mOnBeforeListener: OnBeforeCheckedChangeListener? = null

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun setOnBeforeCheckedChangeListener(listener: OnBeforeCheckedChangeListener) {
        mOnBeforeListener = listener
    }

    override fun setChecked(checked: Boolean) {
        if (mOnBeforeListener != null && mOnBeforeListener!!.onBeforeCheckedChanged(this, checked)) {
            return
        }
        super.setChecked(checked)
    }

    fun setCheckedInternal(checked: Boolean) {
        super.setChecked(checked)
    }

    interface OnBeforeCheckedChangeListener {
        fun onBeforeCheckedChanged(toggleSwitch: ToggleSwitch, checked: Boolean): Boolean
    }
}
