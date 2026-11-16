package dev.tunderlucky.browser

import android.content.Context
import dev.tunderlucky.automation.Launch
import java.net.URLEncoder

object Chrome {
    const val PKG = "com.android.chrome"
    fun open(ctx: Context): Boolean = Launch.app(ctx, "Chrome") != null
    fun openUrl(ctx: Context, url: String): Boolean = Launch.viewUrl(ctx, url, PKG)
    fun search(ctx: Context, q: String): Boolean =
        Launch.viewUrl(ctx, "https://www.google.com/search?q=" + URLEncoder.encode(q, "UTF-8"), PKG)
}
