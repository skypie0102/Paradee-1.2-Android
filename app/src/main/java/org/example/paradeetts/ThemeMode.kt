package org.example.paradeetts

internal val themeModes = listOf("system", "light", "dark")
internal val themeLabels = listOf("System default", "Light", "Dark")

internal fun useDarkTheme(mode: String?, systemDark: Boolean): Boolean = when (mode) {
    "light" -> false
    "dark" -> true
    else -> systemDark
}
