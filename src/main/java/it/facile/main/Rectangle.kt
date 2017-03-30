package it.facile.main

import java.io.Serializable

typealias Pt = Pair<Int, Int>

data class Rectangle(val p1: Pt, val p2: Pt, val p3: Pt, val p4: Pt): Serializable {
    fun asList() = listOf(p1, p2, p3, p4)
}