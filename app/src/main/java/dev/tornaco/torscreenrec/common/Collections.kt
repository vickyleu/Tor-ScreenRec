package dev.tornaco.torscreenrec.common


import com.google.common.base.Preconditions

/**
 * Created by Nick@NewStand.org on 2017/3/8 13:49
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

object Collections {

    fun <C> consumeRemaining(collection: Collection<C>, consumer: Consumer<C>) {
        Preconditions.checkNotNull(collection)
        Preconditions.checkNotNull(consumer)
        for (c in collection) {
            consumer.accept(c)
        }
    }

    fun <C> consumeRemaining(dataArr: Array<C>, consumer: Consumer<C>) {
        Preconditions.checkNotNull(dataArr)
        Preconditions.checkNotNull(consumer)
        for (c in dataArr) {
            consumer.accept(c)
        }
    }

    fun <C> consumeRemaining(collection: Iterable<C>, consumer: Consumer<C>) {
        Preconditions.checkNotNull(collection)
        Preconditions.checkNotNull(consumer)
        for (c in collection) {
            consumer.accept(c)
        }
    }

    fun isNullOrEmpty(collection: Collection<*>?): Boolean {
        return collection == null || collection.isEmpty()
    }
}
