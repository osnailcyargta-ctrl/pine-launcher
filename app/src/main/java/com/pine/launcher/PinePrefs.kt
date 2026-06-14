package com.pine.launcher

import android.content.Context
import android.content.SharedPreferences

object PinePrefs {
    private const val NAME = "pine_prefs"
    private const val KEY_HIDDEN = "hidden_apps"
    private const val KEY_DOCK = "dock_apps"
    private const val KEY_FOLDERS = "folders"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getHiddenApps(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun setHiddenApps(ctx: Context, set: Set<String>) =
        prefs(ctx).edit().putStringSet(KEY_HIDDEN, set).apply()

    fun hideApp(ctx: Context, pkg: String) {
        val s = getHiddenApps(ctx).toMutableSet()
        s.add(pkg)
        setHiddenApps(ctx, s)
    }

    fun unhideApp(ctx: Context, pkg: String) {
        val s = getHiddenApps(ctx).toMutableSet()
        s.remove(pkg)
        setHiddenApps(ctx, s)
    }

    fun isHidden(ctx: Context, pkg: String): Boolean =
        getHiddenApps(ctx).contains(pkg)

    fun getDockApps(ctx: Context): List<String> {
        val raw = prefs(ctx).getString(KEY_DOCK, "") ?: ""
        return if (raw.isEmpty()) emptyList() else raw.split(",").filter { it.isNotEmpty() }
    }

    fun saveDockApps(ctx: Context, pkgs: List<String>) =
        prefs(ctx).edit().putString(KEY_DOCK, pkgs.joinToString(",")).apply()

    // Folders: stored as "FolderName|pkg1;pkg2;pkg3"
    fun getFolders(ctx: Context): Map<String, List<String>> {
        val raw = prefs(ctx).getString(KEY_FOLDERS, "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return raw.split("||").associate { entry ->
            val parts = entry.split("|")
            val name = parts.getOrElse(0) { "Folder" }
            val pkgs = parts.getOrElse(1) { "" }.split(";").filter { it.isNotEmpty() }
            name to pkgs
        }
    }

    fun saveFolders(ctx: Context, folders: Map<String, List<String>>) {
        val raw = folders.entries.joinToString("||") { (name, pkgs) ->
            "$name|${pkgs.joinToString(";")}"
        }
        prefs(ctx).edit().putString(KEY_FOLDERS, raw).apply()
    }
}
