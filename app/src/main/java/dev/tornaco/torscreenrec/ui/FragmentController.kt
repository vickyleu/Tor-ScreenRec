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

package dev.tornaco.torscreenrec.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

import org.newstand.logger.Logger

import dev.tornaco.torscreenrec.R
import dev.tornaco.torscreenrec.common.Collections

class FragmentController(private val mFragmentManager: FragmentManager, private val mPages: List<Fragment>, id: Int) {

    private var mCurrent: Fragment? = null

    private var mDefIndex = 0

    private var containerId = R.id.container

    val current: Fragment?
        get() = if (mCurrent == null) mPages[mDefIndex] else mCurrent

    init {
        this.containerId = id
        init()
    }

    private fun init() {
        val fragmentManager = mFragmentManager
        val transaction = fragmentManager.beginTransaction()

        val old = fragmentManager.fragments

        if (!Collections.isNullOrEmpty(old)) {
            for (fragment in old) {
                transaction.remove(fragment)
                Logger.v("Removed %s", fragment)
            }
        }

        for (fragment in mPages) {
            transaction.add(containerId, fragment, fragment.javaClass.simpleName)
            transaction.hide(fragment)
        }

        transaction.commitAllowingStateLoss()
    }

    fun setDefaultIndex(index: Int) {
        mDefIndex = index
    }

    fun setCurrent(index: Int) {
        val fragmentManager = mFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.hide(current!!)
        val current = mPages[index]
        transaction.show(current)
        transaction.commitAllowingStateLoss()
        mCurrent = current
    }
}
