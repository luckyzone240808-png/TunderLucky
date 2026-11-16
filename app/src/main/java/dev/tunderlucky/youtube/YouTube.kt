package dev.tunderlucky.youtube

import android.content.Context
import dev.tunderlucky.automation.Launch
import java.net.URLEncoder

object YouTube {
    const val PKG = "com.google.android.youtube"
    fun open(ctx: Context): Boolean = Launch.app(ctx, "YouTube") != null || Launch.viewUrl(ctx, "https://www.youtube.com", null)
    fun search(ctx: Context, q: String): Boolean =
        Launch.viewUrl(ctx, "https://www.youtube.com/results?search_query=" + URLEncoder.encode(q, "UTF-8"), PKG)
}
