package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name Meetup from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Meetup", appName)
    }

    @Test
    fun `aes gcm encryption and decryption roundtrip`() {
        val plain = "Persistent Live Location active at CyberHub"
        val chatId = "conv_aarav"
        val encrypted = CryptoManager.encrypt(plain, chatId)
        assertNotEquals(plain, encrypted.cipherTextBase64)
        val decrypted = CryptoManager.decrypt(encrypted.cipherTextBase64, chatId)
        assertEquals(plain, decrypted)
    }
}
