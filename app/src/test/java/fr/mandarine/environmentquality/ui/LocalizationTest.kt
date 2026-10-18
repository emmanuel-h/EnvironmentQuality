package fr.mandarine.environmentquality.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.mandarine.environmentquality.R
import fr.mandarine.environmentquality.data.device.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalizationTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    @Config(qualifiers = "fr")
    fun `should show French when the device is in French`() {
        assertEquals("Suivis", context.getString(R.string.tab_follows))
        assertEquals(AppLanguage.FRENCH, AppLanguage.current(context))
    }

    @Test
    @Config(qualifiers = "en")
    fun `should show English when the device is in English`() {
        assertEquals("Following", context.getString(R.string.tab_follows))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.current(context))
    }

    @Test
    @Config(qualifiers = "de")
    fun `should fall back to French when the device language is not translated`() {
        assertEquals("Suivis", context.getString(R.string.tab_follows))
        assertEquals(AppLanguage.FRENCH, AppLanguage.current(context))
    }
}
