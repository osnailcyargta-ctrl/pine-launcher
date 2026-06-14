package com.pine.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView

class AppPopupMenu(
    private val ctx: Context,
    private val app: AppInfo,
    private val anchor: View,
    private val onDismiss: () -> Unit
) {
    fun show() {
        val inflater = LayoutInflater.from(ctx)
        val view = inflater.inflate(R.layout.popup_app_menu, null)

        val popup = PopupWindow(view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true)
        popup.elevation = 16f
        popup.setBackgroundDrawable(null)
        popup.isOutsideTouchable = true
        popup.setOnDismissListener { onDismiss() }

        view.findViewById<TextView>(R.id.menu_open).setOnClickListener {
            val intent = ctx.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(intent)
            }
            popup.dismiss()
        }

        view.findViewById<TextView>(R.id.menu_info).setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            popup.dismiss()
        }

        view.findViewById<TextView>(R.id.menu_uninstall).setOnClickListener {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:${app.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
            popup.dismiss()
        }

        view.findViewById<TextView>(R.id.menu_back).setOnClickListener {
            popup.dismiss()
        }

        // Position popup above icon
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY,
            loc[0] - 60,
            loc[1] - (ctx.resources.getDimensionPixelSize(R.dimen.popup_height)))
    }
}
