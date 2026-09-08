package com.example.folimeld_android.domain.model

data class PdfMetadata(
    val title: String = "",
    val author: String = "",
    val subject: String = "",
    val keywords: String = "",
    val format: String = "PDF 1.7",
    val producer: String = "",
    val creator: String = ""
)
