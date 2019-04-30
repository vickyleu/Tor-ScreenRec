package dev.tornaco.torscreenrec.ui.tiles

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.widget.ImageView
import android.widget.Toast

import dev.tornaco.torscreenrec.R

/**
 * Created by Tornaco on 2017/7/29.
 * Licensed with Apache.
 */

internal object PaymentDialog {

    fun show(context: Context, titleRes: Int, codeRes: Int) {
        val imageView = ImageView(context)
        imageView.setImageResource(codeRes)
        AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setCancelable(false)
                .setView(imageView)
                .setPositiveButton(R.string.done, null)
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.save_qr_code) { dialogInterface, i -> Toast.makeText(context, R.string.save_by_screenshot, Toast.LENGTH_LONG).show() }
                .create().show()
    }
}
