package com.pine.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object AppLoader {
    fun loadApps(ctx: Context, includeHidden: Boolean = false): List<AppInfo> {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val hidden = PinePrefs.getHiddenApps(ctx)
        return pm.queryIntentActivities(intent, 0)
            .map { ri ->
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    icon = ri.loadIcon(pm),
                    isHidden = hidden.contains(ri.activityInfo.packageName)
                )
            }
            .filter { includeHidden || !it.isHidden }
            .filter { it.packageName != ctx.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
