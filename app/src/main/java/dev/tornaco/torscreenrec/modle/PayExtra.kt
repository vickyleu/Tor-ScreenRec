package dev.tornaco.torscreenrec.modle

import lombok.Builder
import lombok.Getter

/**
 * Created by Tornaco on 2017/7/29.
 * Licensed with Apache.
 */
@Builder
@Getter
class PayExtra {
    val nick: String? = null
    val ad: String? = null
    val date: String? = null
}
