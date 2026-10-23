package com.brendigo.ghosium

/** Testable non-Android tab selection rules. */
object TabSelection {
    fun afterClose(selected:Int,removed:Int,remaining:Int):Int {
        if(remaining<=0)return -1
        return when {
            selected>removed -> selected-1
            selected==removed -> removed.coerceAtMost(remaining-1)
            else -> selected.coerceIn(0,remaining-1)
        }
    }
}
