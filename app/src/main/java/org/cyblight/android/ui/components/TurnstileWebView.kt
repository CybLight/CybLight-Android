package org.cyblight.android.ui.components

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.cyblight.android.BuildConfig

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TurnstileWebView(
    onToken: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokenState = remember { mutableStateOf("") }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        factory = { context ->
            WebView(context).apply {
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                setBackgroundColor(Color.TRANSPARENT)
                background = null
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onToken(token: String) {
                            tokenState.value = token
                            onToken(token)
                        }
                    },
                    "CybLightTurnstile",
                )

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1.0">
                      <script src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"></script>
                      <style>
                        html, body { margin: 0; background: transparent !important; display: flex; justify-content: center; }
                      </style>
                    </head>
                    <body>
                      <div id="turnstile"></div>
                      <script>
                        turnstile.render('#turnstile', {
                          sitekey: '${BuildConfig.TURNSTILE_SITEKEY}',
                          theme: 'dark',
                          callback: function(token) {
                            CybLightTurnstile.onToken(token);
                          }
                        });
                      </script>
                    </body>
                    </html>
                """.trimIndent()

                loadDataWithBaseURL("https://login.cyblight.org", html, "text/html", "UTF-8", null)
            }
        },
    )
}
