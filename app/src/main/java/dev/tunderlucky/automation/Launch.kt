package dev.tunderlucky.automation

import android.content.Context
import android.content.Intent
import android.net.Uri

object Launch {
    fun start(ctx: Context, i: Intent): Boolean = try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); ctx.startActivity(i); true
    } catch (e: Exception) { false }

    fun viewUrl(ctx: Context, url: String, pkg: String?): Boolean {
        if (pkg != null) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(pkg)
            if (start(ctx, i)) return true
        }
        return start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    /** Opens an installed launcher app by (partial) label. Returns the label opened, or null. */
    fun app(ctx: Context, name: String): String? {
        val pm = ctx.packageManager
        val list = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        val m = list.firstOrNull { it.loadLabel(pm).toString().equals(name, true) }
            ?: list.firstOrNull { it.loadLabel(pm).toString().contains(name, true) }
            ?: return null
        val i = pm.getLaunchIntentForPackage(m.activityInfo.packageName) ?: return null
        return if (start(ctx, i)) m.loadLabel(pm).toString() else null
    }
}
