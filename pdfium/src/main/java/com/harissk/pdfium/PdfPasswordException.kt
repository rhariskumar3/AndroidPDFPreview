package com.harissk.pdfium

/**
 * Created by Harishkumar on 26/11/23.
 */

class PdfPasswordException : RuntimeException {
    constructor(): super()
    constructor(message: String?) : super(message)
}
