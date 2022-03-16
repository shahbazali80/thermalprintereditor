package com.ebolo.thermalprintereditor.ui.widgets

import android.content.Context
import android.webkit.WebView

class TextEditorWebView(context: Context): WebView(context) {
    override fun onCheckIsTextEditor() = true
}