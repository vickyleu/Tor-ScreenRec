package dev.tornaco.torscreenrec.common

/**
 * Created by Nick@NewStand.org on 2017/3/7 12:25
 * E-Mail: NewStand@163.com
 * All right reserved.
 */
interface Consumer<T> {
    fun accept(t: T)
}
