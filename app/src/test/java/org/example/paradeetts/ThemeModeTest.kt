package org.example.paradeetts

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test fun systemModeFollowsDeviceAppearance() {
        assertFalse(useDarkTheme("system", false))
        assertTrue(useDarkTheme("system", true))
        assertTrue(useDarkTheme(null, true))
    }

    @Test fun explicitModeOverridesDeviceAppearance() {
        assertTrue(useDarkTheme("dark", false))
        assertTrue(useDarkTheme("dark", true))
        assertFalse(useDarkTheme("light", false))
        assertFalse(useDarkTheme("light", true))
    }
}
