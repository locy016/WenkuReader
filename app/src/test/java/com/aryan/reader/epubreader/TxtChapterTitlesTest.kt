package com.aryan.reader.epubreader

import com.aryan.reader.epub.EpubTocEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class TxtChapterTitlesTest {
    @Test
    fun carriesLogicalChapterNameAcrossInternalParts() {
        val first = "part_1.html"
        val second = "part_2.html"
        val third = "part_3.html"
        val titles = TxtChapterTitles(
            bookTitle = "阅读样本",
            partPaths = listOf(first, second, third),
            entriesByPath = mapOf(
                first to listOf(
                    EpubTocEntry("正文开头", first, null, 0),
                    EpubTocEntry("第一章 出发", first, "heading-1", 0)
                ),
                third to listOf(EpubTocEntry("第二章 到达", third, "heading-2", 0))
            )
        )

        assertEquals("正文开头", titles.titleAt(first, null))
        assertEquals("第一章 出发", titles.titleAt(first, "heading-1"))
        assertEquals("第一章 出发", titles.titleAt(second, null))
        assertEquals("第一章 出发", titles.titleAt(third, "heading-1"))
        assertEquals("第二章 到达", titles.titleAt(third, "heading-2"))
    }

    @Test
    fun fallsBackToBookTitleForUnstructuredText() {
        val titles = TxtChapterTitles("读书笔记", listOf("part_1.html"), emptyMap())
        assertEquals("读书笔记", titles.titleAt("part_1.html", null))
    }
}
