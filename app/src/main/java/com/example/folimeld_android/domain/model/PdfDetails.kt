package com.example.folimeld_android.domain.model

data class PdfDetails(
    val version: String = "1.7",
    val pageLayout: PageLayout = PageLayout.SinglePage,
    val isCoverPage: Boolean = false,
    val isRightToLeft: Boolean = false
)

enum class PageLayout {
    SinglePage, OneColumn, TwoColumnLeft, TwoColumnRight, TwoPageLeft, TwoPageRight
}
