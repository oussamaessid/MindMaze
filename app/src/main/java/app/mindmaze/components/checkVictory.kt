package app.mindmaze.components

import kotlin.math.abs

fun checkVictory(board: List<List<Int>>, size: Int, matrix: Array<IntArray>): Boolean {
    val queens = board.flatMapIndexed { r, row -> row.mapIndexedNotNull { c, v -> if (v == 2) r to c else null } }
    if (queens.size != size) return false

    if (queens.groupBy { it.first }.any { it.value.size > 1 }) return false
    if (queens.groupBy { it.second }.any { it.value.size > 1 }) return false
    if (queens.groupBy { matrix[it.first][it.second] }.any { it.value.size > 1 }) return false

    for (i in queens.indices) for (j in i + 1 until queens.size) {
        val (r1, c1) = queens[i]; val (r2, c2) = queens[j]
        if (abs(r1 - r2) <= 1 && abs(c1 - c2) <= 1) return false
    }
    return true
}