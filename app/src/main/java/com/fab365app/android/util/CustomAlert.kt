package com.app.uxis.lf.util

//noinspection SuspiciousImport
import android.R
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface


class CustomAlert {
    private var alertBuilder: AlertDialog.Builder? = null

    constructor(
        context: Context?,
        content: String?,
        okName: String?,
        singleListener: DialogInterface.OnClickListener?
    ) {
        if (null == alertBuilder) {
//            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder =
                AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Light_Dialog)
            alertBuilder!!.setTitle("")
            alertBuilder!!.setMessage(content)
            alertBuilder!!.setCancelable(false)
            alertBuilder!!.setPositiveButton(okName, singleListener)
        }
    }

    constructor(
        context: Context?,
        content: String?,
        okName: String?,
        cancelName: String?,
        leftListener: DialogInterface.OnClickListener?,
        rightListener: DialogInterface.OnClickListener?
    ) {
        if (null == alertBuilder) {

//            alertBuilder = new AlertDialog.Builder(context);
            alertBuilder = AlertDialog.Builder(context, R.style.Theme_DeviceDefault_Light_Dialog)
            alertBuilder!!.setTitle("")
            alertBuilder!!.setMessage(content)
            alertBuilder!!.setCancelable(false)
            alertBuilder!!.setPositiveButton(okName, leftListener)
            alertBuilder!!.setNegativeButton(cancelName, rightListener)

        }
    }

    fun show() {
        if (null != alertBuilder) alertBuilder!!.show()
    }
}


