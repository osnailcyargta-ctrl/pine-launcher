package com.pine.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context

class PineWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    companion object { const val HOST_ID = 1024 }

    override fun onCreateView(
        context: Context,
        appWidgetId: Int,
        appWidget: android.appwidget.AppWidgetProviderInfo?
    ): AppWidgetHostView = PineWidgetHostView(context)
}

class PineWidgetHostView(context: Context) : AppWidgetHostView(context) {
    init { setPadding(0, 0, 0, 0) }
}
