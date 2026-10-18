package fr.mandarine.environmentquality.ui.navigation

import androidx.navigation3.runtime.NavKey

/** Thin wrapper over the back stack so screens only see intents, not the list. */
class Navigator(private val backStack: MutableList<NavKey>) {

    val currentTab: NavKey get() = backStack.firstOrNull() ?: MapKey

    fun open(key: NavKey) {
        backStack.add(key)
    }

    fun back() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** Re-selecting a tab returns to its root; the stack is never left empty. */
    fun selectTab(root: NavKey) {
        backStack.add(root)
        repeat(backStack.size - 1) { backStack.removeAt(0) }
    }

    private var focusSearch = false

    /** Opens the search tab ready to type, for the buttons that ask for a commune. */
    fun startSearch() {
        focusSearch = true
        selectTab(SearchKey)
    }

    /** True once after [startSearch], so the search field takes focus only when asked to. */
    fun consumeSearchFocus(): Boolean = focusSearch.also { focusSearch = false }
}
