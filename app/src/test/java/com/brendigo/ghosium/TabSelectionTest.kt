package com.brendigo.ghosium

import org.junit.Assert.assertEquals
import org.junit.Test

class TabSelectionTest {
    @Test fun closesActiveFirst()=assertEquals(0,TabSelection.afterClose(0,0,3))
    @Test fun closesActiveLast()=assertEquals(1,TabSelection.afterClose(2,2,2))
    @Test fun closesBeforeActive()=assertEquals(1,TabSelection.afterClose(2,0,2))
    @Test fun closesAfterActive()=assertEquals(0,TabSelection.afterClose(0,2,2))
    @Test fun lastTabGone()=assertEquals(-1,TabSelection.afterClose(0,0,0))
    @Test fun normalizesMissingSelection()=assertEquals(0,TabSelection.afterClose(-1,2,2))
}
