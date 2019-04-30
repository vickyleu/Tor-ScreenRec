package dev.tornaco.torscreenrec.control

import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/21.
 * Licensed with Apache.
 */

enum class FloatControlTheme private constructor(layoutRes: Int, stringRes: Int) {

    Default(R.layout.float_controls, R.string.theme_def),
    DefaultDark(R.layout.float_controls_dark, R.string.theme_def_dark),
    DefaultCircle(R.layout.float_controls_circle, R.string.theme_def_circle);

    var layoutRes: Int = 0
        internal set
    var stringRes: Int = 0
        internal set

    init {
        this.layoutRes = layoutRes
        this.stringRes = stringRes
    }

    companion object {

        fun from(ord: Int): FloatControlTheme {
            for (t in values()) {
                if (t.ordinal == ord) return t
            }
            return Default
        }
    }
}
