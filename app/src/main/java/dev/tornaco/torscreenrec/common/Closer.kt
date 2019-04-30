package dev.tornaco.torscreenrec.common

import org.newstand.logger.Logger

import java.io.Closeable
import java.io.IOException

/**
 * Created by Nick@NewStand.org on 2017/3/9 13:38
 * E-Mail: NewStand@163.com
 * All right reserved.
 */

object Closer {
    fun closeQuietly(closeable: Closeable?) {
        if (closeable == null) return
        try {
            closeable.close()
        } catch (ignore: IOException) {
            Logger.e(ignore, "Fail to close %s with err", closeable)
        }

    }
}
