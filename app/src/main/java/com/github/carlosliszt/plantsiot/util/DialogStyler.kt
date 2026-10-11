package com.github.carlosliszt.plantsiot.util

import android.graphics.Typeface
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.github.carlosliszt.plantsiot.R

object DialogStyler {

    fun apply(dialog: AlertDialog) {
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_rounded)
        dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.apply {
            setTextColor(ContextCompat.getColor(context, R.color.green_dark))
            setTypeface(typeface, Typeface.BOLD)
        }
    }
}
