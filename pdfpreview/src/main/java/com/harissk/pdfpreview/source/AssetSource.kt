package com.harissk.pdfpreview.source

import android.content.Context
import android.os.ParcelFileDescriptor
import com.harissk.pdfium.PdfiumCore
import java.io.File
import java.io.IOException

/**
 * Copyright [2024] [Haris Kumar R](https://github.com/rhariskumar3)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * */

/**
 * A {@link DocumentSource} implementation that loads a PDF document from the application's assets.
 */
internal class AssetSource(private val assetName: String) : DocumentSource {

    @Throws(IOException::class)
    override fun createDocument(
        context: Context,
        core: PdfiumCore,
        password: String?,
    ): Long {
        val outFile = File(context.cacheDir, "$assetName-pdfview.pdf")
        if (assetName.contains("/")) outFile.parentFile?.mkdirs()
        context.assets.open(assetName).buffered().copyTo(outFile.outputStream().buffered())
        return core.newDocument(
            fd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY),
            password = password
        )
    }
}