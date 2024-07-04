package com.harissk.pdfium

/**
 * Represents the metadata of a PDF document.
 *
 * @param title The title of the document.
 * @param author The author of the document.
 * @param subject The subject of the document.
 * @param keywords The keywords associated with the document.
 * @param creator The application that created the document.
 * @param producer The application that produced the document.
 * @param creationDate The date and time when the document was created.
 * @param modDate The date and time when the document was last modified.
 */
data class Meta(
    val title: String,
    val author: String,
    val subject: String,
    val keywords: String,
    val creator: String,
    val producer: String,
    val creationDate: String,
    val modDate: String,
)