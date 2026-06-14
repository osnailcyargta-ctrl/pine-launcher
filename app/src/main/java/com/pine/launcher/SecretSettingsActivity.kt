package com.pine.launcher

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SecretSettingsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secret_settings)

        supportActionBar?.hide()
        window.statusBarColor = android.graphics.Color.parseColor("#0A0A0C")

        listView = findViewById(R.id.hidden_apps_list)
        loadApps()

        findViewById<View>(R.id.btn_back_secret).setOnClickListener { finish() }
    }

    private fun loadApps() {
        allApps.clear()
        allApps.addAll(AppLoader.loadApps(this, includeHidden = true))

        val adapter = object : ArrayAdapter<AppInfo>(this, R.layout.item_hidden_toggle, allApps) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(context)
                    .inflate(R.layout.item_hidden_toggle, parent, false)
                val app = allApps[position]
                view.findViewById<ImageView>(R.id.hidden_icon).setImageDrawable(app.icon)
                view.findViewById<TextView>(R.id.hidden_label).text = app.label
                val toggle = view.findViewById<Switch>(R.id.hidden_switch)
                toggle.setOnCheckedChangeListener(null)
                toggle.isChecked = PinePrefs.isHidden(context, app.packageName)
                toggle.setOnCheckedChangeListener { _, checked ->
                    if (checked) PinePrefs.hideApp(context, app.packageName)
                    else PinePrefs.unhideApp(context, app.packageName)
                }
                return view
            }
        }
        listView.adapter = adapter
    }
}
