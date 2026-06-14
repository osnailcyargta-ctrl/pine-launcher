package com.pine.launcher

import android.content.ClipData
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

class HomeGridAdapter(
    private val items: MutableList<HomeItem>,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Unit,
    private val onFolderClick: (HomeItem.Folder) -> Unit,
    private val onDropToDock: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_APP = 0
        const val TYPE_FOLDER = 1
    }

    override fun getItemViewType(position: Int) = when (items[position]) {
        is HomeItem.App -> TYPE_APP
        is HomeItem.Folder -> TYPE_FOLDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FOLDER -> FolderVH(inflater.inflate(R.layout.item_folder, parent, false))
            else -> AppVH(inflater.inflate(R.layout.item_app, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HomeItem.App -> (holder as AppVH).bind(item.info)
            is HomeItem.Folder -> (holder as FolderVH).bind(item)
        }
    }

    override fun getItemCount() = items.size

    inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
        private val icon: ImageView = view.findViewById(R.id.app_icon)
        private val label: TextView = view.findViewById(R.id.app_label)

        fun bind(app: AppInfo) {
            icon.setImageDrawable(app.icon)
            label.text = app.label

            itemView.setOnClickListener { onAppClick(app) }
            itemView.setOnLongClickListener {
                onAppLongClick(app, itemView)
                true
            }

            // Drag to dock
            icon.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val shadow = object : View.DragShadowBuilder(v) {
                        override fun onDrawShadow(canvas: Canvas) {
                            val bmp = viewToBitmap(v)
                            canvas.drawBitmap(bmp, 0f, 0f, null)
                        }
                        override fun onProvideShadowMetrics(size: Point, touch: Point) {
                            size.set(v.width, v.height)
                            touch.set(v.width / 2, v.height / 2)
                        }
                    }
                    val clip = ClipData.newPlainText("app_pkg", app.packageName)
                    v.startDragAndDrop(clip, shadow, app.packageName, 0)
                }
                false
            }
        }

        private fun viewToBitmap(v: View): Bitmap {
            val bmp = Bitmap.createBitmap(v.width.coerceAtLeast(1), v.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            v.draw(canvas)
            return bmp
        }
    }

    inner class FolderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val previewGrid: GridLayout = view.findViewById(R.id.folder_preview)
        private val label: TextView = view.findViewById(R.id.folder_label)

        fun bind(folder: HomeItem.Folder) {
            label.text = folder.name
            previewGrid.removeAllViews()
            folder.apps.take(4).forEach { app ->
                val iv = ImageView(itemView.context).apply {
                    setImageDrawable(app.icon)
                    val s = itemView.context.resources.getDimensionPixelSize(R.dimen.folder_preview_icon)
                    layoutParams = GridLayout.LayoutParams().apply { width = s; height = s; setMargins(2,2,2,2) }
                    scaleType = ImageView.ScaleType.FIT_CENTER
                }
                previewGrid.addView(iv)
            }
            itemView.setOnClickListener { onFolderClick(folder) }
        }
    }
}
