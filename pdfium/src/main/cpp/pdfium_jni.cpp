#include <jni.h>
#include <string>
#include <vector>
#include <cwchar>
#include <memory>
#include <fstream>
#include <map>

extern "C" {
#include <stdlib.h>
}

#include <android/log.h>

#define JNI_FUNC(retType, bindClass, name)  JNIEXPORT retType JNICALL Java_com_harissk_pdfium_##bindClass##_##name
#define JNI_ARGS    JNIEnv *env, jobject thiz

#define LOG_TAG "PDFCORE"
#define LOGI(...)   __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...)   __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...)   __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
#pragma clang diagnostic ignored "-Wunused-command-line-argument"
#pragma clang diagnostic ignored "-Wsign-conversion"
#pragma clang diagnostic ignored "-Wconversion"

extern "C" {
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <string.h>
#include <stdio.h>
#include <Mutex.h>
#include <fpdfview.h>
#include <fpdf_doc.h>
#include <fpdf_edit.h>
#include <fpdf_text.h>
#include <fpdf_annot.h>
#include <cpp/fpdf_scopers.h>
}

#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/bitmap.h>
#include <fpdf_save.h>

using namespace android;

static Mutex sLibraryLock;
static int sLibraryReferenceCount = 0;

static void initLibraryIfNeed() {
    Mutex::Autolock lock(sLibraryLock);
    if (sLibraryReferenceCount == 0) {
        FPDF_InitLibrary();
        LOGI("PDF Library Initialized!");
        // Log the system page size
        long pageSize = sysconf(_SC_PAGESIZE);
        if (pageSize == -1) {
            LOGE("Failed to get page size.");
        } else {
            LOGI("System page size: %ld bytes", pageSize);
        }
    }
    sLibraryReferenceCount++;
}

static void destroyLibraryIfNeed() {
    Mutex::Autolock lock(sLibraryLock);
    sLibraryReferenceCount--;
    if (sLibraryReferenceCount == 0) {
        FPDF_DestroyLibrary();
        LOGI("PDF Instance Destroyed!");
    }
}

struct rgb {
    uint8_t red;
    uint8_t green;
    uint8_t blue;
};

class DocumentFile {

public:
    FPDF_DOCUMENT pdfDocument = NULL;

    DocumentFile() { initLibraryIfNeed(); }

    ~DocumentFile();
};

DocumentFile::~DocumentFile() {
    if (pdfDocument != NULL) {
        FPDF_CloseDocument(pdfDocument);
    }

    destroyLibraryIfNeed();
}

template<class string_type>
inline typename string_type::value_type *WriteInto(string_type *str, size_t length_with_null) {
    str->reserve(length_with_null);
    str->resize(length_with_null - 1);
    return &((*str)[0]);
}

inline long getFileSize(int fd) {
    struct stat file_state;

    if (fstat(fd, &file_state) >= 0) {
        return (long) (file_state.st_size);
    } else {
        LOGE("Error getting file size");
        return 0;
    }
}

int jniThrowException(JNIEnv *env, const char *className, const char *message) {
    jclass exClass = env->FindClass(className);
    if (exClass == NULL) {
        LOGE("Unable to find exception class %s", className);
        return -1;
    }

    if (env->ThrowNew(exClass, message) != JNI_OK) {
        LOGE("Failed throwing '%s' '%s'", className, message);
        return -1;
    }

    return 0;
}

int jniThrowExceptionFmt(JNIEnv *env, const char *className, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    char msgBuf[512];
    vsnprintf(msgBuf, sizeof(msgBuf), fmt, args);
    return jniThrowException(env, className, msgBuf);
    va_end(args);
}

jobject NewLong(JNIEnv *env, jlong value) {
    jclass cls = env->FindClass("java/lang/Long");
    jmethodID methodID = env->GetMethodID(cls, "<init>", "(J)V");
    return env->NewObject(cls, methodID, value);
}

jobject NewInteger(JNIEnv *env, jint value) {
    jclass cls = env->FindClass("java/lang/Integer");
    jmethodID methodID = env->GetMethodID(cls, "<init>", "(I)V");
    return env->NewObject(cls, methodID, value);
}

uint16_t rgb_to_565(unsigned char R8, unsigned char G8, unsigned char B8) {
    unsigned char R5 = (R8 * 249 + 1014) >> 11;
    unsigned char G6 = (G8 * 253 + 505) >> 10;
    unsigned char B5 = (B8 * 249 + 1014) >> 11;
    return (R5 << 11) | (G6 << 5) | (B5);
}

void rgbBitmapTo565(void *source, int sourceStride, void *dest, AndroidBitmapInfo *info) {
    rgb *srcLine;
    uint16_t *dstLine;
    int y, x;
    for (y = 0; y < info->height; y++) {
        srcLine = (rgb *) source;
        dstLine = (uint16_t *) dest;
        for (x = 0; x < info->width; x++) {
            rgb *r = &srcLine[x];
            dstLine[x] = rgb_to_565(r->red, r->green, r->blue);
        }
        source = (char *) source + sourceStride;
        dest = (char *) dest + info->stride;
    }
}

extern "C" {

static constexpr char kContentsKey[] = "Contents";

static int getBlock(void *param, unsigned long position, unsigned char *outBuffer,
                    unsigned long size) {
    const int fd = reinterpret_cast<intptr_t>(param);
    const int readCount = pread(fd, outBuffer, size, position);
    if (readCount < 0) {
        LOGE("Cannot read from file descriptor.");
        return 0;
    }
    return 1;
}

void throwPdfiumException(JNIEnv *env, long errorNum) {
    switch (errorNum) {
        case FPDF_ERR_UNKNOWN:
            jniThrowException(env, "com/harissk/pdfium/exception/UnknownException",
                              "An unexpected error occurred while processing the PDF document");
            break;
        case FPDF_ERR_FILE:
            jniThrowException(env, "com/harissk/pdfium/exception/FileNotFoundException",
                              "Unable to find the specified PDF file");
            break;
        case FPDF_ERR_FORMAT:
            jniThrowException(env, "com/harissk/pdfium/exception/InvalidFormatException",
                              "The provided file is not a valid PDF document");
            break;
        case FPDF_ERR_PASSWORD:
            jniThrowException(env, "com/harissk/pdfium/exception/IncorrectPasswordException",
                              "The provided password is incorrect");
            break;
        case FPDF_ERR_SECURITY:
            jniThrowException(env, "com/harissk/pdfium/exception/UnsupportedSecurityException",
                              "The PDF document uses an unsupported security scheme");
            break;
        case FPDF_ERR_PAGE:
            jniThrowException(env, "com/harissk/pdfium/exception/PageNotFoundException",
                              "The requested page was not found within the PDF document");
            break;
        case 7:
            jniThrowException(env, "com/harissk/pdfium/exception/FileNotFoundException",
                              "File is Empty");
            break;
        default:
            jniThrowException(env, "com/harissk/pdfium/exception/UnknownException", "No Error");
    }
}

void throwPdfiumException1(JNIEnv *env, const char *message) {
    jniThrowException(env, "com/harissk/pdfium/exception/UnknownException", message);
}


JNI_FUNC(jlong, PdfiumCore, nativeOpenDocument)(JNI_ARGS, jint fd, jstring password) {

    size_t fileLength = (size_t) getFileSize(fd);
    if (fileLength <= 0) {
        jniThrowException(env, "java/io/IOException", "Empty PDF file");
        const long errorNum = FPDF_ERR_FILE;
        return errorNum;
    }

    DocumentFile *docFile = new DocumentFile();

    FPDF_FILEACCESS loader;
    loader.m_FileLen = fileLength;
    loader.m_Param = reinterpret_cast<void *>(intptr_t(fd));
    loader.m_GetBlock = &getBlock;

    const char *cpassword = NULL;
    if (password != NULL) {
        cpassword = env->GetStringUTFChars(password, NULL);
    }

    FPDF_DOCUMENT document = FPDF_LoadCustomDocument(&loader, cpassword);

    if (cpassword != NULL) {
        env->ReleaseStringUTFChars(password, cpassword);
    }

    if (!document) {
        delete docFile;

        const long errorNum = FPDF_GetLastError();
        throwPdfiumException(env, errorNum);
        return -1;
    }

    docFile->pdfDocument = document;

    return reinterpret_cast<jlong>(docFile);
}

JNI_FUNC(jlong, PdfiumCore, nativeOpenMemDocument)(JNI_ARGS, jbyteArray data, jstring password) {
    DocumentFile *docFile = new DocumentFile();

    const char *cpassword = NULL;
    if (password != NULL) {
        cpassword = env->GetStringUTFChars(password, NULL);
    }

    jbyte *cData = env->GetByteArrayElements(data, NULL);
    int size = (int) env->GetArrayLength(data);
    jbyte *cDataCopy = new jbyte[size];
    memcpy(cDataCopy, cData, size);
    FPDF_DOCUMENT document = FPDF_LoadMemDocument(reinterpret_cast<const void *>(cDataCopy),
                                                  size, cpassword);
    delete[] cDataCopy; // Release the copied data
    env->ReleaseByteArrayElements(data, cData, JNI_ABORT);

    if (cpassword != NULL) {
        env->ReleaseStringUTFChars(password, cpassword);
    }

    if (!document) {
        delete docFile;
        // cDataCopy is already deleted right after FPDF_LoadMemDocument call
        const long errorNum = FPDF_GetLastError();
        throwPdfiumException(env, errorNum);
        return -1;
    }

    docFile->pdfDocument = document;

    return reinterpret_cast<jlong>(docFile);
}

JNI_FUNC(jint, PdfiumCore, nativeGetPageCount)(JNI_ARGS, jlong documentPtr) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(documentPtr);
    return (jint) FPDF_GetPageCount(doc->pdfDocument);
}

JNI_FUNC(void, PdfiumCore, nativeCloseDocument)(JNI_ARGS, jlong documentPtr) {
    delete reinterpret_cast<DocumentFile *>(documentPtr);
}

static jlong loadPageInternal(JNIEnv *env, DocumentFile *doc, int pageIndex) {
    try {
        if (doc == NULL) throw "Get page document null";

        FPDF_DOCUMENT pdfDoc = doc->pdfDocument;
        if (pdfDoc != NULL) {
            FPDF_PAGE page = FPDF_LoadPage(pdfDoc, pageIndex);
            if (page == NULL) {
                throw "Loaded page is null";
            }
            return reinterpret_cast<jlong>(page);
        } else {
            throw "Get page pdf document null";
        }

    } catch (const char *msg) {
        LOGE("%s", msg);

        throwPdfiumException1(env, "cannot load page");

        return -1;
    }
}

static void closePageInternal(jlong pagePtr) {
    FPDF_ClosePage(reinterpret_cast<FPDF_PAGE>(pagePtr));
}

JNI_FUNC(jlong, PdfiumCore, nativeLoadPage)(JNI_ARGS, jlong docPtr, jint pageIndex) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);

    if (!doc) {
        const long errorNum = FPDF_ERR_FILE;
        throwPdfiumException(env, errorNum);
        return -1;
    }

    jlong pagePtr = loadPageInternal(env, doc, (int) pageIndex);
    if (pagePtr == -1) {
        const long errorNum = FPDF_GetLastError();
        throwPdfiumException(env, errorNum);
        return -1;
    }

    return pagePtr;
}
JNI_FUNC(jlongArray, PdfiumCore, nativeLoadPages)(JNI_ARGS, jlong docPtr, jint fromIndex,
                                                  jint toIndex) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);

    if (toIndex < fromIndex) return NULL;
    jlong pages[toIndex - fromIndex + 1];

    int i;
    for (i = 0; i <= (toIndex - fromIndex); i++) {
        pages[i] = loadPageInternal(env, doc, (i + fromIndex));
    }

    jlongArray javaPages = env->NewLongArray((jsize) (toIndex - fromIndex + 1));
    env->SetLongArrayRegion(javaPages, 0, (jsize) (toIndex - fromIndex + 1), (const jlong *) pages);

    return javaPages;
}

JNI_FUNC(void, PdfiumCore, nativeClosePage)(JNI_ARGS, jlong pagePtr) { closePageInternal(pagePtr); }
JNI_FUNC(void, PdfiumCore, nativeClosePages)(JNI_ARGS, jlongArray pagesPtr) {
    int length = (int) (env->GetArrayLength(pagesPtr));
    jlong *pages = env->GetLongArrayElements(pagesPtr, NULL);

    int i;
    for (i = 0; i < length; i++) { closePageInternal(pages[i]); }
}

JNI_FUNC(jint, PdfiumCore, nativeGetPageWidthPixel)(JNI_ARGS, jlong pagePtr, jint dpi) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    return (jint) (FPDF_GetPageWidth(page) * dpi / 72);
}
JNI_FUNC(jint, PdfiumCore, nativeGetPageHeightPixel)(JNI_ARGS, jlong pagePtr, jint dpi) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    return (jint) (FPDF_GetPageHeight(page) * dpi / 72);
}

JNI_FUNC(jint, PdfiumCore, nativeGetPageWidthPoint)(JNI_ARGS, jlong pagePtr) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    return (jint) FPDF_GetPageWidth(page);
}
JNI_FUNC(jint, PdfiumCore, nativeGetPageHeightPoint)(JNI_ARGS, jlong pagePtr) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    return (jint) FPDF_GetPageHeight(page);
}
JNI_FUNC(jobject, PdfiumCore, nativeGetPageSizeByIndex)(JNI_ARGS, jlong docPtr, jint pageIndex,
                                                        jint dpi) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    if (doc == NULL) {
        LOGE("Document is null");

        throwPdfiumException1(env, "Document is null");
        return NULL;
    }

    double width, height;
    int result = FPDF_GetPageSizeByIndex(doc->pdfDocument, pageIndex, &width, &height);

    if (result == 0) {
        width = 0;
        height = 0;
    }

    jint widthInt = (jint) (width * dpi / 72);
    jint heightInt = (jint) (height * dpi / 72);

    jclass clazz = env->FindClass("com/harissk/pdfium/util/Size");
    jmethodID constructorID = env->GetMethodID(clazz, "<init>", "(II)V");
    return env->NewObject(clazz, constructorID, widthInt, heightInt);
}

static void renderPageInternal(FPDF_PAGE page,
                               ANativeWindow_Buffer *windowBuffer,
                               int startX, int startY,
                               int canvasHorSize, int canvasVerSize,
                               int drawSizeHor, int drawSizeVer,
                               bool renderAnnot) {

    FPDF_BITMAP pdfBitmap = FPDFBitmap_CreateEx(canvasHorSize, canvasVerSize,
                                                FPDFBitmap_BGRA,
                                                windowBuffer->bits,
                                                (int) (windowBuffer->stride) * 4);

    if (drawSizeHor < canvasHorSize || drawSizeVer < canvasVerSize) {
        FPDFBitmap_FillRect(pdfBitmap, 0, 0, canvasHorSize, canvasVerSize,
                            0x848484FF); //Gray
    }

    int flags = FPDF_REVERSE_BYTE_ORDER;

    if (renderAnnot) {
        flags |= FPDF_ANNOT;
    }

    FPDF_RenderPageBitmap(pdfBitmap, page,
                          startX, startY,
                          drawSizeHor, drawSizeVer,
                          0, flags);
}

JNI_FUNC(void, PdfiumCore, nativeRenderPage)(JNI_ARGS, jlong pagePtr, jobject objSurface,
                                             jint startX, jint startY,
                                             jint drawSizeHor, jint drawSizeVer,
                                             jboolean renderAnnot) {
    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, objSurface);
    if (nativeWindow == NULL) {
        LOGE("native window pointer null");
        return;
    }
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);

    if (page == NULL) {
        LOGE("Render page pointers invalid");
        return;
    }

    if (ANativeWindow_getFormat(nativeWindow) != WINDOW_FORMAT_RGBA_8888) {
        LOGD("Set format to RGBA_8888");
        ANativeWindow_setBuffersGeometry(nativeWindow,
                                         ANativeWindow_getWidth(nativeWindow),
                                         ANativeWindow_getHeight(nativeWindow),
                                         WINDOW_FORMAT_RGBA_8888);
    }

    ANativeWindow_Buffer buffer;
    int ret;
    if ((ret = ANativeWindow_lock(nativeWindow, &buffer, NULL)) != 0) {
        LOGE("Locking native window failed: %s", strerror(ret * -1));
        return;
    }

    renderPageInternal(page, &buffer,
                       (int) startX, (int) startY,
                       buffer.width, buffer.height,
                       (int) drawSizeHor, (int) drawSizeVer,
                       (bool) renderAnnot);

    ANativeWindow_unlockAndPost(nativeWindow);
    ANativeWindow_release(nativeWindow);
}

JNI_FUNC(void, PdfiumCore, nativeRenderPageBitmap)(JNI_ARGS, jlong pagePtr, jobject bitmap,
                                                   jint startX, jint startY,
                                                   jint drawSizeHor, jint drawSizeVer,
                                                   jboolean renderAnnot) {
    try {
        FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);

        if (page == NULL || bitmap == NULL) {
            LOGE("Render page pointers invalid");
            return;
        }

        AndroidBitmapInfo info;
        int ret;
        if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
            LOGE("Fetching bitmap info failed: %s", strerror(ret * -1));
            return;
        }

        int canvasHorSize = info.width;
        int canvasVerSize = info.height;

        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 &&
            info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
            LOGE("Bitmap format must be RGBA_8888 or RGB_565");
            return;
        }

        void *addr;
        if ((ret = AndroidBitmap_lockPixels(env, bitmap, &addr)) != 0) {
            LOGE("Locking bitmap failed: %s", strerror(ret * -1));
            return;
        }

        void *tmp;
        int format;
        int sourceStride;
        if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
            tmp = malloc(canvasVerSize * canvasHorSize * sizeof(rgb));
            sourceStride = canvasHorSize * sizeof(rgb);
            format = FPDFBitmap_BGR;
        } else {
            tmp = addr;
            sourceStride = info.stride;
            format = FPDFBitmap_BGRA;
        }

        FPDF_BITMAP pdfBitmap = FPDFBitmap_CreateEx(canvasHorSize, canvasVerSize,
                                                    format, tmp, sourceStride);

        if (drawSizeHor < canvasHorSize || drawSizeVer < canvasVerSize) {
            FPDFBitmap_FillRect(pdfBitmap, 0, 0, canvasHorSize, canvasVerSize,
                                0x848484FF); //Gray
        }

        int baseHorSize = (canvasHorSize < drawSizeHor) ? canvasHorSize : (int) drawSizeHor;
        int baseVerSize = (canvasVerSize < drawSizeVer) ? canvasVerSize : (int) drawSizeVer;
        int baseX = (startX < 0) ? 0 : (int) startX;
        int baseY = (startY < 0) ? 0 : (int) startY;
        int flags = FPDF_REVERSE_BYTE_ORDER;

        if (renderAnnot) {
            flags |= FPDF_ANNOT;
        }

        if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
            FPDFBitmap_FillRect(pdfBitmap, baseX, baseY, baseHorSize, baseVerSize,
                                0xFFFFFFFF); //White
        }

        FPDF_RenderPageBitmap(pdfBitmap, page,
                              startX, startY,
                              (int) drawSizeHor, (int) drawSizeVer,
                              0, flags);

        if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
            rgbBitmapTo565(tmp, sourceStride, addr, &info);
            free(tmp);
        }

        AndroidBitmap_unlockPixels(env, bitmap);
    } catch (const char *msg) {
        LOGE("%s", msg);

        throwPdfiumException1(env, "cannot render page bitmap");
    }
}

JNI_FUNC(jstring, PdfiumCore, nativeGetDocumentMetaText)(JNI_ARGS, jlong docPtr, jstring tag) {
    const char *ctag = env->GetStringUTFChars(tag, NULL);
    if (ctag == NULL) {
        return env->NewStringUTF("");
    }
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);

    size_t bufferLen = FPDF_GetMetaText(doc->pdfDocument, ctag, NULL, 0);
    if (bufferLen <= 2) {
        return env->NewStringUTF("");
    }
    std::wstring text;
    FPDF_GetMetaText(doc->pdfDocument, ctag, WriteInto(&text, bufferLen + 1), bufferLen);
    env->ReleaseStringUTFChars(tag, ctag);
    return env->NewString((jchar *) text.c_str(), bufferLen / 2 - 1);
}

JNI_FUNC(jobject, PdfiumCore, nativeGetFirstChildBookmark)(JNI_ARGS, jlong docPtr,
                                                           jobject bookmarkPtr) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    FPDF_BOOKMARK parent;
    if (bookmarkPtr == NULL) {
        parent = NULL;
    } else {
        jclass longClass = env->GetObjectClass(bookmarkPtr);
        jmethodID longValueMethod = env->GetMethodID(longClass, "longValue", "()J");

        jlong ptr = env->CallLongMethod(bookmarkPtr, longValueMethod);
        parent = reinterpret_cast<FPDF_BOOKMARK>(ptr);
    }
    FPDF_BOOKMARK bookmark = FPDFBookmark_GetFirstChild(doc->pdfDocument, parent);
    if (bookmark == NULL) {
        return NULL;
    }
    return NewLong(env, reinterpret_cast<jlong>(bookmark));
}

JNI_FUNC(jobject, PdfiumCore, nativeGetSiblingBookmark)(JNI_ARGS, jlong docPtr, jlong bookmarkPtr) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    FPDF_BOOKMARK parent = reinterpret_cast<FPDF_BOOKMARK>(bookmarkPtr);
    FPDF_BOOKMARK bookmark = FPDFBookmark_GetNextSibling(doc->pdfDocument, parent);
    if (bookmark == NULL) {
        return NULL;
    }
    return NewLong(env, reinterpret_cast<jlong>(bookmark));
}

JNI_FUNC(jstring, PdfiumCore, nativeGetBookmarkTitle)(JNI_ARGS, jlong bookmarkPtr) {
    FPDF_BOOKMARK bookmark = reinterpret_cast<FPDF_BOOKMARK>(bookmarkPtr);
    size_t bufferLen = FPDFBookmark_GetTitle(bookmark, NULL, 0);
    if (bufferLen <= 2) {
        return env->NewStringUTF("");
    }
    std::wstring title;
    FPDFBookmark_GetTitle(bookmark, WriteInto(&title, bufferLen + 1), bufferLen);
    return env->NewString((jchar *) title.c_str(), bufferLen / 2 - 1);
}

JNI_FUNC(jlong, PdfiumCore, nativeGetBookmarkDestIndex)(JNI_ARGS, jlong docPtr, jlong bookmarkPtr) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    FPDF_BOOKMARK bookmark = reinterpret_cast<FPDF_BOOKMARK>(bookmarkPtr);

    FPDF_DEST dest = FPDFBookmark_GetDest(doc->pdfDocument, bookmark);
    if (dest == NULL) {
        return -1;
    }
    return (jlong) FPDFDest_GetDestPageIndex(doc->pdfDocument, dest);
}

JNI_FUNC(jlongArray, PdfiumCore, nativeGetPageLinks)(JNI_ARGS, jlong pagePtr) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    int pos = 0;
    std::vector<jlong> links;
    FPDF_LINK link;
    while (FPDFLink_Enumerate(page, &pos, &link)) {
        links.push_back(reinterpret_cast<jlong>(link));
    }

    jlongArray result = env->NewLongArray(links.size());
    env->SetLongArrayRegion(result, 0, links.size(), &links[0]);
    return result;
}

JNI_FUNC(jobject, PdfiumCore, nativeGetDestPageIndex)(JNI_ARGS, jlong docPtr, jlong linkPtr) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    FPDF_LINK link = reinterpret_cast<FPDF_LINK>(linkPtr);
    FPDF_DEST dest = FPDFLink_GetDest(doc->pdfDocument, link);
    if (dest == NULL) {
        return NULL;
    }
    unsigned long index = FPDFDest_GetDestPageIndex(doc->pdfDocument, dest);
    return NewInteger(env, (jint) index);
}

JNI_FUNC(jstring, PdfiumCore, nativeGetLinkURI)(JNI_ARGS, jlong docPtr, jlong linkPtr) {
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    FPDF_LINK link = reinterpret_cast<FPDF_LINK>(linkPtr);
    FPDF_ACTION action = FPDFLink_GetAction(link);
    if (action == NULL) {
        return NULL;
    }
    size_t bufferLen = FPDFAction_GetURIPath(doc->pdfDocument, action, NULL, 0);
    if (bufferLen <= 0) {
        return env->NewStringUTF("");
    }
    std::string uri;
    FPDFAction_GetURIPath(doc->pdfDocument, action, WriteInto(&uri, bufferLen), bufferLen);
    return env->NewStringUTF(uri.c_str());
}

JNI_FUNC(jobject, PdfiumCore, nativeGetLinkRect)(JNI_ARGS, jlong linkPtr) {
    FPDF_LINK link = reinterpret_cast<FPDF_LINK>(linkPtr);
    FS_RECTF fsRectF;
    FPDF_BOOL result = FPDFLink_GetAnnotRect(link, &fsRectF);

    if (!result) {
        return NULL;
    }

    jclass clazz = env->FindClass("android/graphics/RectF");
    jmethodID constructorID = env->GetMethodID(clazz, "<init>", "(FFFF)V");
    return env->NewObject(clazz, constructorID, fsRectF.left, fsRectF.top, fsRectF.right,
                          fsRectF.bottom);
}

JNI_FUNC(jobject, PdfiumCore, nativePageCoordinateToDevice)(JNI_ARGS, jlong pagePtr, jint startX,
                                                            jint startY, jint sizeX,
                                                            jint sizeY, jint rotate, jdouble pageX,
                                                            jdouble pageY) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    int deviceX, deviceY;

    FPDF_PageToDevice(page, startX, startY, sizeX, sizeY, rotate, pageX, pageY, &deviceX, &deviceY);

    jclass clazz = env->FindClass("android/graphics/Point");
    jmethodID constructorID = env->GetMethodID(clazz, "<init>", "(II)V");
    return env->NewObject(clazz, constructorID, deviceX, deviceY);
}

JNI_FUNC(jobject, PdfiumCore, nativeDeviceCoordinateToPage)(JNI_ARGS, jlong pagePtr, jint startX,
                                                            jint startY, jint sizeX,
                                                            jint sizeY, jint rotate, jint deviceX,
                                                            jint deviceY) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    double pageX, pageY;

    FPDF_DeviceToPage(page, startX, startY, sizeX, sizeY, rotate, deviceX, deviceY, &pageX, &pageY);

    jclass clazz = env->FindClass("android/graphics/PointF");
    jmethodID constructorID = env->GetMethodID(clazz, "<init>", "(FF)V");
    return env->NewObject(clazz, constructorID, pageX, pageY);
}

JNI_FUNC(jint, PdfiumCore, nativeGetPageRotation)(JNI_ARGS, jlong pagePtr) {
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    return (jint) FPDFPage_GetRotation(page);
}


//////////////////////////////////////////
// Begin PDF TextPage api
//////////////////////////////////////////

static jlong loadTextPageInternal(JNIEnv *env, FPDF_PAGE page) {
    try {
        if (page == NULL) {
            LOGE("Input FPDF_PAGE is null");
            throwPdfiumException1(env, "Input FPDF_PAGE is null to loadTextPageInternal");
            return -1;
        }
        FPDF_TEXTPAGE textPage = FPDFText_LoadPage(page);
        if (textPage == NULL) {
            // Consider using FPDF_GetLastError() if appropriate for FPDFText_LoadPage
            LOGE("FPDFText_LoadPage failed.");
            throwPdfiumException1(env, "Failed to load text page from FPDF_PAGE");
            return -1;
        }
        return reinterpret_cast<jlong>(textPage);
    } catch (const char *msg) { // Catching specific exceptions or rethrowing might be better
        LOGE("%s", msg);
        // Ensure an exception is thrown to Java if not already done
        jniThrowException(env, "java/lang/RuntimeException", msg);
        return -1;
    }
}

static void closeTextPageInternal(jlong textPagePtr) {
    FPDFText_ClosePage(reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr));
}

JNI_FUNC(jlong, PdfiumCore, nativeLoadTextPage)(JNI_ARGS, jlong docPtr, jlong pagePtr) {
    // docPtr is kept for potential context but not directly used by loadTextPageInternal now
    FPDF_PAGE page = reinterpret_cast<FPDF_PAGE>(pagePtr);
    return loadTextPageInternal(env, page);
}

JNI_FUNC(jlongArray, PdfiumCore, nativeLoadTextPages)(JNI_ARGS, jlong docPtr, jlongArray pagePtrs) {
    // docPtr is kept for potential context
    if (pagePtrs == NULL) {
        LOGE("pagePtrs array is null");
        return NULL;
    }

    jsize numPages = env->GetArrayLength(pagePtrs);
    if (numPages <= 0) {
        LOGD("pagePtrs array is empty or invalid length");
        return env->NewLongArray(0); // Return empty array
    }

    jlong *nativePagePtrs = env->GetLongArrayElements(pagePtrs, NULL);
    if (nativePagePtrs == NULL) {
        LOGE("Failed to get elements from pagePtrs array");
        return NULL; // Or throw exception
    }

    std::vector<jlong> textPagesResult(numPages);
    for (jsize i = 0; i < numPages; ++i) {
        FPDF_PAGE currentPage = reinterpret_cast<FPDF_PAGE>(nativePagePtrs[i]);
        textPagesResult[i] = loadTextPageInternal(env, currentPage);
        // Optionally, check for -1 and handle error (e.g., stop and return partially filled array or throw)
    }

    env->ReleaseLongArrayElements(pagePtrs, nativePagePtrs,
                                  JNI_ABORT); // Use JNI_ABORT as we just read

    jlongArray javaTextPages = env->NewLongArray(numPages);
    if (javaTextPages == NULL) {
        LOGE("Failed to allocate new jlongArray for text pages");
        return NULL; // Or throw exception
    }
    env->SetLongArrayRegion(javaTextPages, 0, numPages, textPagesResult.data());

    return javaTextPages;
}

JNI_FUNC(void, PdfiumCore, nativeCloseTextPage)(JNI_ARGS, jlong textPagePtr) {
    closeTextPageInternal(textPagePtr);
}

JNI_FUNC(void, PdfiumCore, nativeCloseTextPages)(JNI_ARGS, jlongArray textPagesPtr) {
    int length = (int) (env->GetArrayLength(textPagesPtr));
    jlong *textPages = env->GetLongArrayElements(textPagesPtr, NULL);

    int i;
    for (i = 0; i < length; i++) { closeTextPageInternal(textPages[i]); }
}

JNI_FUNC(jint, PdfiumCore, nativeTextCountChars)(JNI_ARGS, jlong textPagePtr) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    return (jint) FPDFText_CountChars(textPage);// FPDF_TEXTPAGE
}

JNI_FUNC(jint, PdfiumCore, nativeTextGetUnicode)(JNI_ARGS, jlong textPagePtr, jint index) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    return (jint) FPDFText_GetUnicode(textPage, (int) index);
}

JNI_FUNC(jdoubleArray, PdfiumCore, nativeTextGetCharBox)(JNI_ARGS, jlong textPagePtr, jint index) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    jdoubleArray result = env->NewDoubleArray(4);
    if (result == NULL) {
        return NULL;
    }
    double fill[4];
    FPDFText_GetCharBox(textPage, (int) index, &fill[0], &fill[1], &fill[2], &fill[3]);
    env->SetDoubleArrayRegion(result, 0, 4, (jdouble *) fill);
    return result;
}

JNI_FUNC(jint, PdfiumCore, nativeTextGetCharIndexAtPos)(JNI_ARGS, jlong textPagePtr, jdouble x,
                                                        jdouble y, jdouble xTolerance,
                                                        jdouble yTolerance) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    return (jint) FPDFText_GetCharIndexAtPos(textPage, (double) x, (double) y, (double) xTolerance,
                                             (double) yTolerance);
}

JNI_FUNC(jint, PdfiumCore, nativeTextGetText)(JNI_ARGS, jlong textPagePtr, jint start_index,
                                              jint count, jshortArray result) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    jboolean isCopy = 0;
    unsigned short *arr = (unsigned short *) env->GetShortArrayElements(result, &isCopy);
    jint output = (jint) FPDFText_GetText(textPage, (int) start_index, (int) count, arr);
    if (isCopy) {
        env->SetShortArrayRegion(result, 0, output, (jshort *) arr);
        env->ReleaseShortArrayElements(result, (jshort *) arr, JNI_ABORT);
    }
    return output;
}

JNI_FUNC(jint, PdfiumCore, nativeTextCountRects)(JNI_ARGS, jlong textPagePtr, jint start_index,
                                                 jint count) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    return (jint) FPDFText_CountRects(textPage, (int) start_index, (int) count);
}

JNI_FUNC(jdoubleArray, PdfiumCore, nativeTextGetRect)(JNI_ARGS, jlong textPagePtr,
                                                      jint rect_index) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    jdoubleArray result = env->NewDoubleArray(4);
    if (result == NULL) {
        return NULL;
    }
    double fill[4];
    FPDFText_GetRect(textPage, (int) rect_index, &fill[0], &fill[1], &fill[2], &fill[3]);
    env->SetDoubleArrayRegion(result, 0, 4, (jdouble *) fill);
    return result;
}


JNI_FUNC(jint, PdfiumCore, nativeTextGetBoundedTextLength)(JNI_ARGS, jlong textPagePtr,
                                                           jdouble left,
                                                           jdouble top, jdouble right,
                                                           jdouble bottom) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);

    return (jint) FPDFText_GetBoundedText(textPage, (double) left, (double) top,
                                          (double) right, (double) bottom, NULL, 0);
}

JNI_FUNC(jint, PdfiumCore, nativeTextGetBoundedText)(JNI_ARGS, jlong textPagePtr, jdouble left,
                                                     jdouble top, jdouble right, jdouble bottom,
                                                     jshortArray arr) {
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    jboolean isCopy = 0;
    unsigned short *buffer = NULL;
    int bufLen = 0;
    if (arr != NULL) {
        buffer = (unsigned short *) env->GetShortArrayElements(arr, &isCopy);
        bufLen = env->GetArrayLength(arr);
    }
    jint output = (jint) FPDFText_GetBoundedText(textPage, (double) left, (double) top,
                                                 (double) right, (double) bottom, buffer, bufLen);
    if (isCopy) {
        env->SetShortArrayRegion(arr, 0, output, (jshort *) buffer);
        env->ReleaseShortArrayElements(arr, (jshort *) buffer, JNI_ABORT);
    }
    return output;
}

//////////////////////////////////////////
// Begin PDF SDK Search
//////////////////////////////////////////

unsigned short *convertWideString(JNIEnv *env, jstring query) {

    std::wstring value;
    const jchar *raw = env->GetStringChars(query, 0);
    jsize len = env->GetStringLength(query);
    value.assign(raw, raw + len);
    env->ReleaseStringChars(query, raw);

    size_t length = sizeof(uint16_t) * (value.length() + 1);
    unsigned short *result = static_cast<unsigned short *>(malloc(length));
    char *ptr = reinterpret_cast<char *>(result);
    size_t i = 0;
    for (wchar_t w: value) {
        ptr[i++] = w & 0xff;
        ptr[i++] = (w >> 8) & 0xff;
    }
    ptr[i++] = 0;
    ptr[i] = 0;

    return result;
}

JNI_FUNC(jlong, PdfiumCore, nativeSearchStart)(JNI_ARGS, jlong textPagePtr, jstring query,
                                               jboolean matchCase, jboolean matchWholeWord) {
    // convert jstring to UTF-16LE encoded wide strings
    unsigned short *pQuery = convertWideString(env, query);
    FPDF_TEXTPAGE textPage = reinterpret_cast<FPDF_TEXTPAGE>(textPagePtr);
    FPDF_SCHHANDLE search;
    unsigned long flags = 0;

    if (matchCase) {
        flags = FPDF_MATCHCASE;
    }
    if (matchWholeWord) {
        flags = flags | FPDF_MATCHWHOLEWORD;
    }

    search = FPDFText_FindStart(textPage, pQuery, flags, 0);
    free(pQuery); // Free the allocated wide string
    return reinterpret_cast<jlong>(search);
}

JNI_FUNC(void, PdfiumCore, nativeSearchStop)(JNI_ARGS, jlong searchHandlePtr) {
    FPDF_SCHHANDLE search = reinterpret_cast<FPDF_SCHHANDLE>(searchHandlePtr);
    FPDFText_FindClose(search);
}

JNI_FUNC(jboolean, PdfiumCore, nativeSearchNext)(JNI_ARGS, jlong searchHandlePtr) {
    FPDF_SCHHANDLE search = reinterpret_cast<FPDF_SCHHANDLE>(searchHandlePtr);
    FPDF_BOOL result = FPDFText_FindNext(search);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNI_FUNC(jboolean, PdfiumCore, nativeSearchPrev)(JNI_ARGS, jlong searchHandlePtr) {
    FPDF_SCHHANDLE search = reinterpret_cast<FPDF_SCHHANDLE>(searchHandlePtr);
    FPDF_BOOL result = FPDFText_FindPrev(search);
    return result ? JNI_TRUE : JNI_FALSE;
}

JNI_FUNC(jint, PdfiumCore, nativeGetCharIndexOfSearchResult)(JNI_ARGS, jlong searchHandlePtr) {
    FPDF_SCHHANDLE search = reinterpret_cast<FPDF_SCHHANDLE>(searchHandlePtr);
    return FPDFText_GetSchResultIndex(search);
}

JNI_FUNC(jint, PdfiumCore, nativeCountSearchResult)(JNI_ARGS, jlong searchHandlePtr) {
    FPDF_SCHHANDLE search = reinterpret_cast<FPDF_SCHHANDLE>(searchHandlePtr);
    return FPDFText_GetSchCount(search);
}

//////////////////////////////////////////
// Begin PDF Annotation api
//////////////////////////////////////////
JNI_FUNC(jlong, PdfiumCore, nativeAddTextAnnotation)(JNI_ARGS, jlong docPtr, int page_index,
                                                     jstring text_,
                                                     jintArray color_, jintArray bound_) {

    // This function uses loadPageInternal, not the refactored loadTextPageInternal.
    // If its FPDF_PAGE also needs to come from Java, it would require similar refactoring.
    // For now, assuming its FPDF_PAGE loading mechanism remains unchanged.
    FPDF_PAGE page;
    DocumentFile *doc = reinterpret_cast<DocumentFile *>(docPtr);
    // NOTE: This still uses the old loadPageInternal to get an FPDF_PAGE.
    // This is distinct from the FPDF_TEXTPAGE loading changes above.
    jlong pagePtrAsJlong = loadPageInternal(env, doc, page_index); // Renamed for clarity
    if (pagePtrAsJlong == -1) {
        return -1;
    } else {
        page = reinterpret_cast<FPDF_PAGE>(pagePtrAsJlong);
    }

    // Get the bound array
    jint *bounds = env->GetIntArrayElements(bound_, NULL);
    int boundsLen = (int) (env->GetArrayLength(bound_));
    if (boundsLen != 4) {
        return -1;
    }

    // Set the annotation rectangle.
    FS_RECTF rect;
    rect.left = bounds[0];
    rect.top = bounds[1];
    rect.right = bounds[2];
    rect.bottom = bounds[3];

    // Get the text color
    unsigned int R, G, B, A;
    jint *colors = env->GetIntArrayElements(color_, NULL);
    int colorsLen = (int) (env->GetArrayLength(color_));
    if (colorsLen == 4) {
        R = colors[0];
        G = colors[1];
        B = colors[2];
        A = colors[3];
    } else {
        R = 51u;
        G = 102u;
        B = 153u;
        A = 204u;
    }

    // Add a text annotation to the page.
    FPDF_ANNOTATION annot = FPDFPage_CreateAnnot(page, FPDF_ANNOT_TEXT);

    // set the rectangle of the annotation
    FPDFAnnot_SetRect(annot, &rect);
    env->ReleaseIntArrayElements(bound_, bounds, 0);

    // Set the color of the annotation.
    FPDFAnnot_SetColor(annot, FPDFANNOT_COLORTYPE_Color, R, G, B, A);
    env->ReleaseIntArrayElements(color_, colors, 0);

    // Set the content of the annotation.
    unsigned short *kContents = convertWideString(env, text_);
    FPDFAnnot_SetStringValue(annot, kContentsKey, kContents);
    free(kContents); // Free the allocated wide string

    // save page
    FPDF_DOCUMENT pdfDoc = doc->pdfDocument;
    if (!FPDF_SaveAsCopy(pdfDoc, NULL, FPDF_INCREMENTAL)) {
        return -1;
    }

    // close page
    closePageInternal(
            pagePtrAsJlong); // Use the jlong version for consistency with closePageInternal

    // reload page
    // NOTE: This reloads the FPDF_PAGE using the old mechanism.
    pagePtrAsJlong = loadPageInternal(env, doc, page_index);
    if (pagePtrAsJlong == -1) {
        // Handle error after trying to reload page for annotation
        return -1;
    }

    jclass clazz = env->FindClass("com/harissk/pdfium/PdfiumCore");
    jmethodID callback = env->GetMethodID(clazz, "onAnnotationAdded",
                                          "(Ljava/lang/Integer;Ljava/lang/Long;)V"); // Return type is V (void)
    // The callback likely expects the FPDF_PAGE pointer, not FPDF_ANNOTATION pointer.
    // Ensure the Java callback signature matches what's passed.
    // Assuming it expects the reloaded page pointer.
    env->CallObjectMethod(thiz, callback, NewInteger(env, page_index),
                          NewLong(env, pagePtrAsJlong));

    return reinterpret_cast<jlong>(annot);
}

const char *getPdfiumErrorMessage(int errCode) {
    switch (errCode) {
        case FPDF_ERR_SUCCESS:
            return "No error";
        case FPDF_ERR_UNKNOWN:
            return "Unknown error";
        case FPDF_ERR_FILE:
            return "File not found or could not be opened";
        case FPDF_ERR_FORMAT:
            return "File not in PDF format or corrupted";
        case FPDF_ERR_PASSWORD:
            return "Incorrect password";
        case FPDF_ERR_SECURITY:
            return "Unsupported security scheme";
        case FPDF_ERR_PAGE:
            return "Page not found or content error";
        default:
            return "Unknown PDF error";
    }
}

JNI_FUNC(jint, PdfiumCore, nativeGetLastError)(JNI_ARGS, jlong documentPtr) {

    // Cast to the correct DocumentFile pointer first. This might be incorrect in your existing code.
    DocumentFile *docFile = reinterpret_cast<DocumentFile *>(documentPtr);

    return FPDF_GetLastError();
}


JNI_FUNC(jstring, PdfiumCore, nativeGetErrorMessage)(JNI_ARGS, jint errorCode) {

    const char *errorMsg = getPdfiumErrorMessage(errorCode);
    return env->NewStringUTF(errorMsg);
}

}//extern C

#pragma clang diagnostic pop
