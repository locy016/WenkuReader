package com.aryan.reader.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChineseTxtChaptersTest {
    @Test
    fun recognizesChineseHeadingsAsStandaloneLines() {
        assertEquals("第0001章 朝气蓬勃", ChineseTxtChapters.headingOf("\uFEFF　 第0001章 朝气蓬勃  "))
        assertEquals("第一百零一回：春秋", ChineseTxtChapters.headingOf("第一百零一回：春秋"))
        assertEquals("序章 大荒", ChineseTxtChapters.headingOf("序章 大荒"))
        assertNull(ChineseTxtChapters.headingOf("第四回、第五回……"))
        assertNull(ChineseTxtChapters.headingOf("他正在读第一章 朝气蓬勃"))
    }

    @Test
    fun recognizesChapterEndingsWithLineWrapsAndExportArtifacts() {
        assertTrue(ChineseTxtChapters.endsClassicalChapter("且看下回分解。\\&apos;);", ""))
        assertTrue(ChineseTxtChapters.endsClassicalChapter("且看下回分解。&apos;;", ""))
        assertTrue(ChineseTxtChapters.endsClassicalChapter("解。", "再看下回分"))
        assertFalse(ChineseTxtChapters.endsClassicalChapter("且看下回分解。正文继续。", ""))
        assertEquals("话说宣王自东郊游猎", ChineseTxtChapters.openingExcerpt("　话说宣王自东郊游猎，晚归。"))
    }
}
