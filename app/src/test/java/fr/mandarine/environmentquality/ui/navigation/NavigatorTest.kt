package fr.mandarine.environmentquality.ui.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {

    private val stack = mutableListOf<NavKey>(MapKey)
    private val navigator = Navigator(stack)

    @Test
    fun `should push a screen when opening it`() {
        navigator.open(CommuneKey("27681"))

        assertEquals(listOf(MapKey, CommuneKey("27681")), stack)
    }

    @Test
    fun `should keep the root when going back from it`() {
        navigator.back()

        assertEquals(listOf<NavKey>(MapKey), stack)
    }

    @Test
    fun `should replace the whole stack when selecting another tab`() {
        navigator.open(CommuneKey("27681"))
        navigator.open(HistoryKey("27681"))

        navigator.selectTab(SearchKey)

        assertEquals(listOf<NavKey>(SearchKey), stack)
        assertEquals(SearchKey, navigator.currentTab)
    }
}
