package dev.tunderlucky.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores the user's API key encrypted with an Android Keystore AES-GCM key. */
class SecureStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("secure", Context.MODE_PRIVATE)
    private val alias = "tunder_lucky_key"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build()
        )
        return g.generateKey()
    }

    fun putApiKey(value: String) {
        if (value.isBlank()) { prefs.edit().remove("k").apply(); return }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val iv = c.iv
        val out = iv + c.doFinal(value.toByteArray())
        prefs.edit().putString("k", Base64.encodeToString(out, Base64.NO_WRAP)).apply()
    }

    fun getApiKey(): String {
        val s = prefs.getString("k", null) ?: return ""
        return try {
            val b = Base64.decode(s, Base64.NO_WRAP)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b.copyOfRange(0, 12)))
            String(c.doFinal(b.copyOfRange(12, b.size)))
        } catch (e: Exception) { "" }
    }

    fun hasKey(): Boolean = prefs.contains("k")
}
