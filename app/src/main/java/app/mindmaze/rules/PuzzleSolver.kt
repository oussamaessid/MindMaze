package app.mindmaze.rules

import kotlin.math.abs

/**
 * Finds one valid solution to a region-constrained N-queens-style board (one bomb per
 * row/column/color-region, no two touching including diagonally) via backtracking. Used
 * only to pick guaranteed-correct cells for the first-launch guided walkthrough on the
 * real level 0 — never shown to the player as "the" answer, and cheap enough (board sizes
 * here are small, 5-9) to run synchronously on the main thread.
 */
object PuzzleSolver {

    fun solve(matrix: Array<IntArray>, size: Int): List<Pair<Int, Int>>? {
        val colsUsed = BooleanArray(size)
        val regionsUsed = BooleanArray(64)
        val placed = mutableListOf<Pair<Int, Int>>()

        fun isSafe(row: Int, col: Int): Boolean {
            val region = matrix[row][col]
            if (region < 0 || region >= regionsUsed.size) return false
            if (colsUsed[col] || regionsUsed[region]) return false
            for ((pr, pc) in placed) {
                if (abs(pr - row) <= 1 && abs(pc - col) <= 1) return false
            }
            return true
        }

        fun backtrack(row: Int): Boolean {
            if (row == size) return true
            for (col in 0 until size) {
                if (isSafe(row, col)) {
                    val region = matrix[row][col]
                    placed.add(row to col)
                    colsUsed[col] = true
                    regionsUsed[region] = true
                    if (backtrack(row + 1)) return true
                    placed.removeAt(placed.lastIndex)
                    colsUsed[col] = false
                    regionsUsed[region] = false
                }
            }
            return false
        }

        return if (backtrack(0)) placed.toList() else null
    }
}
