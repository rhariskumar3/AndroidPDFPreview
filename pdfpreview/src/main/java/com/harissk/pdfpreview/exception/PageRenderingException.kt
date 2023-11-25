package com.harissk.pdfpreview.exception

/**
 * Created by Harishkumar on 25/11/23.
 */
class PageRenderingException(val page: Int, throwable: Throwable) : Exception(throwable)