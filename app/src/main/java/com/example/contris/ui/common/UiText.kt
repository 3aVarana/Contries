package com.example.contris.ui.common

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Text that can be resolved lazily so ViewModels never touch Android resources. */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    class Resource(@StringRes val resId: Int, vararg val args: Any) : UiText {
        override fun equals(other: Any?): Boolean =
            other is Resource && other.resId == resId && other.args.contentEquals(args)
        override fun hashCode(): Int = 31 * resId + args.contentHashCode()
    }

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(resId, *args)
    }

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(resId, *args)
    }
}
