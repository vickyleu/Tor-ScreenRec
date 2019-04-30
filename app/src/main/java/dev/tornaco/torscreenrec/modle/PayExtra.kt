package dev.tornaco.torscreenrec.modle

import lombok.Builder

/**
 * Created by Tornaco on 2017/7/29.
 * Licensed with Apache.
 */
@Builder
data class PayExtra(val nick: String? = null,
                    val ad: String? = null,
                    val date: String? = null)

