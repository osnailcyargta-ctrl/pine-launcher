package com.pine.launcher

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.*

class FolderDialog(
    context: Context,
    private val folderName: String,
    private val apps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : Dialog(context, R.style.PineDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_folder)

        window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
            setBackgroundDrawableResource(android.R.color.transparent)
        }

        findViewById<TextView>(R.id.folder_title).text = folderName

        val grid = findViewById<GridLayout>(R.id.folder_grid)
        apps.forEach { app ->
            val item = LayoutInflater.from(context).inflate(R.layout.item_app_small, null)
            item.findViewById<ImageView>(R.id.app_icon_small).setImageDrawable(app.icon)
            item.findViewById<TextView>(R.id.app_label_small).text = app.label
            item.setOnClickListener {
                onAppClick(app)
                dismiss()
            }
            grid.addView(item)
        }

        findViewById<View>(R.id.folder_backdrop).setOnClickListener { dismiss() }
    }
}
