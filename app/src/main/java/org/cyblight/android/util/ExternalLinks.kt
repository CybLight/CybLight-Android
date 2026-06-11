package org.cyblight.android.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import org.cyblight.android.R

object ExternalLinks {
    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val activity = findActivity(context)
        val launchContext = activity ?: context

        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val chooser = Intent.createChooser(intent, null).apply {
            if (activity == null) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        try {
            launchContext.startActivity(chooser)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_open_link, Toast.LENGTH_SHORT).show()
        }
    }

    fun findActivity(context: Context): Activity? = context.findActivityInternal()

    private tailrec fun Context.findActivityInternal(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivityInternal()
        else -> null
    }
}
