package com.aryan.reader

import android.content.Context
import com.aryan.reader.data.RecentFileItem

internal enum class CoverBinding(val widthFraction: Float, val tintOpacity: Float) {
    PAPER(0.032f, 0.34f),
    CLOTH(0.074f, 0.82f),
    WRAPPED(0.052f, 0.58f),
}

internal enum class LibraryBookCategory(val labelRes: Int, val paletteIndex: Int, val artwork: CoverBinding) {
    FICTION(R.string.wenku_category_fiction, 3, CoverBinding.PAPER),
    HISTORY(R.string.wenku_category_history, 5, CoverBinding.CLOTH),
    BUSINESS(R.string.wenku_category_business, 6, CoverBinding.WRAPPED),
    STUDY(R.string.wenku_category_study, 4, CoverBinding.WRAPPED),
    EBOOKS(R.string.wenku_category_ebooks, 0, CoverBinding.PAPER),
    DOCUMENTS(R.string.wenku_category_documents, 1, CoverBinding.CLOTH),
    COMICS(R.string.wenku_category_comics, 2, CoverBinding.WRAPPED),
    OTHER(R.string.wenku_category_other, 7, CoverBinding.CLOTH);

    companion object {
        fun fromId(id: String?): LibraryBookCategory? = entries.firstOrNull { it.name == id }
    }
}

internal data class LibraryBookGroup(
    val category: LibraryBookCategory,
    val tagLabel: String? = null,
    val fileType: FileType? = null,
) {
    val id: String
        get() {
            val categoryId = tagLabel?.let { "tag:$it" } ?: category.name
            return fileType?.let { "format:${it.name}:$categoryId" } ?: categoryId
        }

    fun label(context: Context): String {
        val categoryLabel = tagLabel ?: context.getString(category.labelRes)
        return fileType?.let { "$categoryLabel · ${it.name}" } ?: categoryLabel
    }
}

internal fun orderLibraryGroups(groups: Collection<LibraryBookGroup>, savedOrder: List<String>): List<LibraryBookGroup> {
    val positions = savedOrder.withIndex().associate { (index, id) -> id to index }
    return groups.sortedWith(compareBy(
        { positions[it.id] ?: Int.MAX_VALUE },
        { it.category.ordinal },
        { it.tagLabel.orEmpty() },
        { it.fileType?.name.orEmpty() },
    ))
}

internal fun RecentFileItem.libraryBookGroup(): LibraryBookGroup {
    val separateFileType = type.takeIf { it == FileType.TXT || it == FileType.EPUB }
    LibraryBookCategory.fromId(libraryCategoryId)?.let { return LibraryBookGroup(it, fileType = separateFileType) }
    val formatCategory = when (type) {
        FileType.CBZ, FileType.CBR, FileType.CB7, FileType.CBT -> LibraryBookCategory.COMICS
        FileType.PDF, FileType.DOC, FileType.DOCX, FileType.ODT, FileType.FODT,
        FileType.PPTX, FileType.MD, FileType.HTML -> LibraryBookCategory.DOCUMENTS
        FileType.TXT, FileType.EPUB, FileType.MOBI, FileType.FB2 -> LibraryBookCategory.EBOOKS
        else -> LibraryBookCategory.OTHER
    }
    val tag = tags.mapNotNull { it.name.trim().takeIf(String::isNotEmpty) }.minOrNull()
        ?: return LibraryBookGroup(formatCategory, fileType = separateFileType)
    val categoryStyle = when {
        listOf("小说", "文学", "玄幻", "故事", "fiction").any { tag.contains(it, ignoreCase = true) } -> LibraryBookCategory.FICTION
        listOf("历史", "传记", "history").any { tag.contains(it, ignoreCase = true) } -> LibraryBookCategory.HISTORY
        listOf("经管", "经济", "社科", "商业", "business").any { tag.contains(it, ignoreCase = true) } -> LibraryBookCategory.BUSINESS
        listOf("学习", "教材", "教程", "study").any { tag.contains(it, ignoreCase = true) } -> LibraryBookCategory.STUDY
        // A tag uses a consistent category style across file formats.
        else -> LibraryBookCategory.OTHER
    }
    return LibraryBookGroup(categoryStyle, tag, separateFileType)
}
