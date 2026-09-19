package com.solappan.agent.tools

import org.junit.Assert.*
import org.junit.Test

class AppMatchingTest {
    private val apps = listOf("Maps" to "installed.maps", "Maps Beta" to "installed.beta", "Music" to "installed.music")
    @Test fun exactNamesTakePriority() = assertEquals(listOf("installed.maps"), matchingAppPackages(apps, " MAPS "))
    @Test fun ambiguousNamesAreNotGuessed() = assertEquals(2, matchingAppPackages(apps, "map").size)
    @Test fun unknownNamesHaveNoHardcodedFallback() = assertTrue(matchingAppPackages(apps, "Spotify").isEmpty())
    @Test fun emptyQueryCannotOpenFirstApp() = assertTrue(matchingAppPackages(apps, "").isEmpty())
}
