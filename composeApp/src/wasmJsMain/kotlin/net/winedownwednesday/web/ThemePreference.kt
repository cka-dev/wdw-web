package net.winedownwednesday.web

import kotlinx.browser.localStorage

private const val KEY = "wdw_dark_mode"

/** Returns true (dark) by default if no preference has been saved yet. */
fun loadThemePreference(): Boolean =
    localStorage.getItem(KEY)?.toBooleanStrictOrNull() ?: true

fun saveThemePreference(isDark: Boolean) {
    localStorage.setItem(KEY, isDark.toString())
}
