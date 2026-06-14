package com.pine.launcher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.AudioManager
import android.os.*
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.*

class MainActivity : AppCompatActivity() {

    // Widget
    private lateinit var widgetHost: PineWidgetHost
    private lateinit var widgetManager: AppWidgetManager
    private val WIDGET_REQUEST = 1001
    private val WIDGET_CONFIG_REQUEST = 1002
    private var pendingWidgetId = -1

    // Views
    private lateinit var wallpaperView: ImageView
    private lateinit var gridView: RecyclerView
    private lateinit var dockBar: LinearLayout
    private lateinit var gridAdapter: HomeGridAdapter

    // Data
    private val homeItems = mutableListOf<HomeItem>() // apps + folders + widgets
    private val dockPkgs = mutableListOf<String>()

    // Secret setting: volume check
    private lateinit var audioManager: AudioManager

    // App install/remove receiver
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            refreshGrid()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        widgetHost = PineWidgetHost(applicationContext)
        widgetManager = AppWidgetManager.getInstance(this)

        wallpaperView = findViewById(R.id.wallpaper)
        gridView = findViewById(R.id.home_grid)
        dockBar = findViewById(R.id.dock_bar)

        setWallpaper()
        setupGrid()
        setupDock()
        setupLongPressAddWidget()

        widgetHost.startListening()

        // Register package changes
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        registerReceiver(packageReceiver, filter)
    }

    // ── WALLPAPER: use pine tree pixel art ──
    private fun setWallpaper() {
        try {
            val wm = getSystemService(WALLPAPER_SERVICE) as android.app.WallpaperManager
            val bmp = wm.drawable
            if (bmp != null) wallpaperView.setImageDrawable(bmp)
            else wallpaperView.setImageResource(R.drawable.wallpaper_default)
        } catch (e: Exception) {
            wallpaperView.setImageResource(R.drawable.wallpaper_default)
        }
    }

    // ── GRID ──
    private fun setupGrid() {
        refreshGrid()
        gridView.layoutManager = GridLayoutManager(this, 4)
        gridAdapter = HomeGridAdapter(
            homeItems,
            onAppClick = { app -> launchApp(app.packageName) },
            onAppLongClick = { app, view -> showAppPopup(app, view) },
            onFolderClick = { folder -> openFolder(folder) },
            onDropToDock = { pkg -> addToDock(pkg) }
        )
        gridView.adapter = gridAdapter
    }

    private fun refreshGrid() {
        homeItems.clear()
        val hidden = PinePrefs.getHiddenApps(this)
        val folders = PinePrefs.getFolders(this)
        val folderPkgs = folders.values.flatten().toSet()
        val dockSet = PinePrefs.getDockApps(this).toSet()

        AppLoader.loadApps(this).forEach { app ->
            if (!folderPkgs.contains(app.packageName) && !dockSet.contains(app.packageName)) {
                homeItems.add(HomeItem.App(app))
            }
        }
        folders.forEach { (name, pkgs) ->
            val apps = pkgs.mapNotNull { pkg ->
                AppLoader.loadApps(this, true).find { it.packageName == pkg }
            }
            if (apps.isNotEmpty()) homeItems.add(HomeItem.Folder(name, apps))
        }
        if (::gridAdapter.isInitialized) gridAdapter.notifyDataSetChanged()
    }

    // ── DOCK ──
    private fun setupDock() {
        dockPkgs.clear()
        dockPkgs.addAll(PinePrefs.getDockApps(this))
        renderDock()
    }

    private fun renderDock() {
        dockBar.removeAllViews()
        val pm = packageManager
        dockPkgs.forEach { pkg ->
            try {
                val info = pm.getApplicationInfo(pkg, 0)
                val icon = pm.getApplicationIcon(info)
                val label = pm.getApplicationLabel(info).toString()
                val iconView = ImageView(this).apply {
                    setImageDrawable(icon)
                    val size = resources.getDimensionPixelSize(R.dimen.dock_icon_size)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginStart = 10; marginEnd = 10
                    }
                    setOnClickListener { launchApp(pkg) }
                    setOnLongClickListener {
                        val app = AppInfo(label, pkg, icon)
                        showAppPopup(app, this)
                        true
                    }
                    // Drag to remove from dock
                    setOnTouchListener { v, e ->
                        if (e.action == MotionEvent.ACTION_DOWN) {
                            val shadow = View.DragShadowBuilder(v)
                            v.startDragAndDrop(ClipData.newPlainText("dock_remove", pkg), shadow, pkg, 0)
                        }
                        false
                    }
                }
                dockBar.addView(iconView)
            } catch (e: PackageManager.NameNotFoundException) {}
        }
    }

    private fun addToDock(pkg: String) {
        if (!dockPkgs.contains(pkg) && dockPkgs.size < 5) {
            dockPkgs.add(pkg)
            PinePrefs.saveDockApps(this, dockPkgs)
            renderDock()
            refreshGrid()
        }
    }

    // ── LONG PRESS -> ADD WIDGET ──
    private fun setupLongPressAddWidget() {
        gridView.setOnLongClickListener {
            showAddWidgetPicker()
            true
        }
    }

    private fun showAddWidgetPicker() {
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetHost.allocateAppWidgetId())
        }
        pendingWidgetId = widgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        }
        startActivityForResult(pickIntent, WIDGET_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            if (pendingWidgetId != -1) {
                widgetHost.deleteAppWidgetId(pendingWidgetId)
                pendingWidgetId = -1
            }
            return
        }
        when (requestCode) {
            WIDGET_REQUEST -> {
                val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (widgetId == -1) return
                val info = widgetManager.getAppWidgetInfo(widgetId)
                if (info?.configure != null) {
                    val configIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                        component = info.configure
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    }
                    startActivityForResult(configIntent, WIDGET_CONFIG_REQUEST)
                } else {
                    addWidgetToHome(widgetId)
                }
            }
            WIDGET_CONFIG_REQUEST -> {
                val widgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (widgetId != -1) addWidgetToHome(widgetId)
            }
        }
    }

    private fun addWidgetToHome(widgetId: Int) {
        val info = widgetManager.getAppWidgetInfo(widgetId) ?: return
        val hostView = widgetHost.createView(applicationContext, widgetId, info)
        hostView.setAppWidget(widgetId, info)

        val widgetContainer = findViewById<LinearLayout>(R.id.widget_container)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(R.dimen.widget_height)
        ).apply { bottomMargin = 12 }
        widgetContainer.addView(hostView, params)
    }

    // ── LAUNCH APP ──
    private fun launchApp(pkg: String) {
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.scale_in, android.R.anim.fade_out)
    }

    // ── POPUP MENU ──
    private fun showAppPopup(app: AppInfo, anchor: View) {
        AppPopupMenu(this, app, anchor) { }.show()
    }

    // ── FOLDER ──
    private fun openFolder(folder: HomeItem.Folder) {
        FolderDialog(this, folder.name, folder.apps) { app ->
            launchApp(app.packageName)
        }.show()
    }

    // ── SECRET SETTINGS ──
    // Access: open Pine Launcher Settings then set volume to max
    fun checkSecretAccess() {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (cur >= max) {
            startActivity(Intent(this, SecretSettingsActivity::class.java))
        } else {
            Toast.makeText(this, "Pine Settings", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshGrid()
        setupDock()
    }

    override fun onDestroy() {
        super.onDestroy()
        widgetHost.stopListening()
        unregisterReceiver(packageReceiver)
    }

    override fun onBackPressed() {
        // Home button behavior - do nothing (stay on home)
    }
}

// ── HOME ITEMS ──
sealed class HomeItem {
    data class App(val info: AppInfo) : HomeItem()
    data class Folder(val name: String, val apps: List<AppInfo>) : HomeItem()
}
