package com.pine.launcher

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    var isHidden: Boolean = false
)
