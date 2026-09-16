package com.tyamada.folimeld.domain.model

import androidx.annotation.StringRes
import com.tyamada.folimeld.R

enum class ThumbnailSize(val dp: Int, @StringRes val labelRes: Int) {
    ExtraSmall(64, R.string.size_extra_small),
    Small(128, R.string.size_small),
    Medium(192, R.string.size_medium),
    Large(256, R.string.size_large)
}
