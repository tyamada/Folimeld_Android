package com.tyamada.folimeld.domain.model

import android.graphics.Bitmap

data class PdfPage(
    val index: Int,
    val thumbnail: Bitmap? = null,
    val rotation: Int = 0,
    val width: Float = 0f,
    val height: Float = 0f
)
