package com.aryan.reader.epub

internal object ChineseTxtChapters {
    private const val MAX_HEADING_LENGTH = 72
    private const val NUMERALS = "0-9０-９零〇一二两兩三四五六七八九十百千万萬亿億"
    private val heading = Regex(
        "^(?:第\\s*[$NUMERALS]+\\s*[章节節回卷话話部篇]|序章|序言|楔子|引子|尾声|尾聲|终章|終章|后记|後記|完结感言|完結感言)" +
            "(?:[\\s　:：.．·—-]+.*)?$"
    )
    private val chapterEnding = Regex(
        "下[一]?回(?:分解|分晓|分曉|便见|便見)[。．！!？?」』”]+" +
            "(?:[\\\\]*(?:&apos;|['’])[);； ]*)?$"
    )

    fun headingOf(line: String): String? {
        var start = 0
        while (start < line.length && line[start].isWhitespace()) start++
        if (line.getOrNull(start) == '\uFEFF') {
            start++
            while (start < line.length && line[start].isWhitespace()) start++
        }
        when (line.getOrNull(start)) {
            '第', '序', '楔', '引', '尾', '终', '終', '后', '後', '完' -> Unit
            else -> return null
        }
        var end = line.length
        while (end > start && line[end - 1].isWhitespace()) end--
        if (end - start > MAX_HEADING_LENGTH) return null
        return line.substring(start, end).takeIf(heading::matches)
    }

    fun endsClassicalChapter(line: String, previousLineTail: String): Boolean {
        if (previousLineTail.isEmpty() && line.indexOf('下', (line.length - 48).coerceAtLeast(0)) < 0) {
            return false
        }
        val tail = line.takeLast(48).trim()
        if (chapterEnding.containsMatchIn(tail)) return true
        return tail.length <= 16 &&
            chapterEnding.containsMatchIn(previousLineTail.trimEnd() + tail)
    }

    fun openingExcerpt(line: String): String? {
        val opening = line.trim()
        if (opening.length <= 28 && opening.startsWith("主编") &&
            opening.trimEnd('!', '！', '。', '：', ':').endsWith("推荐")
        ) {
            return null
        }
        return opening.take(18).substringBefore('，').substringBefore('。')
            .trimEnd { it.isHighSurrogate() }.ifBlank { null }
    }
}
