package com.harissk.androidpdfpreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.harissk.androidpdfpreview.presentation.ui.screen.PDFPreviewScreen
import com.harissk.androidpdfpreview.ui.theme.PDFPreviewTheme

class MainActivity : ComponentActivity() {

    private var windowInsetsController: WindowInsetsControllerCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PDFPreviewTheme {
                PDFPreviewScreen(
                    onFullScreen = { fullScreen ->
                        if (fullScreen) showFullScreen() else hideFullScreen()
                    }
                )
            }
        }
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    }

    private fun showFullScreen() {
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun hideFullScreen() {
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }
}