package dev.tunderlucky.settings

import android.content.Context
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private inner class Str(val k: String, val d: String) : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String = p.getString(k, d) ?: d
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) { p.edit().putString(k, value).apply() }
    }
    private inner class Bool(val k: String, val d: Boolean) : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Boolean = p.getBoolean(k, d)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) { p.edit().putBoolean(k, value).apply() }
    }

    var endpoint by Str("endpoint", "")
    var model by Str("model", "")
    var lang by Str("lang", "en-IN")
    var history by Str("history", "[]")
    var saveHistory by Bool("save_history", true)
    var lowGfx by Bool("low_gfx", false)
    var speak by Bool("speak", true)
}
