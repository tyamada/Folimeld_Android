package com.tyamada.folimeld.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tyamada.folimeld.domain.model.PageLayout
import com.tyamada.folimeld.domain.repository.PdfDocumentState
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tyamada.folimeld.domain.model.PdfDetails
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class PdfRepositoryTest {

    private lateinit var repository: PdfRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PDFBoxResourceLoader.init(context)
        repository = PdfRepositoryImpl(context)
    }

    private fun getAssetUri(assetName: String): Uri {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val inputStream = testContext.assets.open("testdata/$assetName")
        val tempFile = File(context.cacheDir, "test_$assetName")
        val outputStream = FileOutputStream(tempFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        return Uri.fromFile(tempFile)
    }

    private suspend fun getLoadedDetails(filename: String): PdfDetails {
        val uri = getAssetUri(filename)
        repository.open(uri).getOrThrow()
        val state = repository.documentState.first { it is PdfDocumentState.Loaded } as PdfDocumentState.Loaded
        return state.details
    }

    @Test
    fun testReadingDirection() = runBlocking {
        assertTrue("R2L_Cover should be R2L", getLoadedDetails("R2L_Cover.pdf").isRightToLeft)
        assertFalse("L2R_Cover should be L2R", getLoadedDetails("L2R_Cover.pdf").isRightToLeft)
    }

    @Test
    fun testCoverPage() = runBlocking {
        // "Cover" typically means the first page is on the right (TwoColumnRight or TwoPageRight)
        val l2rCover = getLoadedDetails("L2R_Cover.pdf")
        assertTrue("L2R_Cover should have cover", l2rCover.isCoverPage)
        
        val l2rNoCover = getLoadedDetails("L2R_NoCover.pdf")
        assertFalse("L2R_NoCover should NOT have cover", l2rNoCover.isCoverPage)
        
        val r2lCover = getLoadedDetails("R2L_Cover.pdf")
        assertTrue("R2L_Cover should have cover", r2lCover.isCoverPage)
        
        val r2lNoCover = getLoadedDetails("R2L_NoCover.pdf")
        assertFalse("R2L_NoCover should NOT have cover", r2lNoCover.isCoverPage)
    }

    @Test
    fun testAllLayouts() = runBlocking {
        val cases = listOf(
            "L2R_Single.pdf" to PageLayout.SinglePage,
            "L2R_OneColumn.pdf" to PageLayout.OneColumn,
            "L2R_TwoColumnLeft.pdf" to PageLayout.TwoColumnLeft,
            "L2R_TwoColumnRight.pdf" to PageLayout.TwoColumnRight,
            "L2R_TwoPageLeft.pdf" to PageLayout.TwoPageLeft,
            "L2R_TwoPageRight.pdf" to PageLayout.TwoPageRight,
            "R2L_Single.pdf" to PageLayout.SinglePage,
            "R2L_OneColumn.pdf" to PageLayout.OneColumn,
            "R2L_TwoColumnLeft.pdf" to PageLayout.TwoColumnLeft,
            "R2L_TwoColumnRight.pdf" to PageLayout.TwoColumnRight,
            "R2L_TwoPageLeft.pdf" to PageLayout.TwoPageLeft,
            "R2L_TwoPageRight.pdf" to PageLayout.TwoPageRight
        )

        for ((filename, expected) in cases) {
            val details = getLoadedDetails(filename)
            assertEquals("File $filename should have layout $expected", expected, details.pageLayout)
        }
    }
}
