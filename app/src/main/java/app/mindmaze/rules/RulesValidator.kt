package app.mindmaze.rules

import kotlin.math.abs

data class ViolationResult(
    val hasViolation: Boolean,
    val message: String = ""
)

object RulesValidator {

    fun validate(
        boardState: List<List<Int>>,
        matrix: Array<IntArray>,
        boardSize: Int
    ): ViolationResult {
        val queens = boardState.flatMapIndexed { r, row ->
            row.mapIndexedNotNull { c, v -> if (v == 2) r to c else null }
        }

        if (queens.isEmpty()) return ViolationResult(false)

        val rowGroups = queens.groupBy { it.first }
        if (rowGroups.any { it.value.size > 1 })
            return ViolationResult(true, "Two bombs on the same row! ↔️")

        val colGroups = queens.groupBy { it.second }
        if (colGroups.any { it.value.size > 1 })
            return ViolationResult(true, "Two bombs in the same column! ↕️")

        val regionGroups = queens.groupBy { matrix[it.first][it.second] }
        if (regionGroups.any { it.value.size > 1 })
            return ViolationResult(true, "Two bombs in the same color region! 🎨")

        for (i in queens.indices) {
            for (j in i + 1 until queens.size) {
                val (r1, c1) = queens[i]
                val (r2, c2) = queens[j]
                if (abs(r1 - r2) <= 1 && abs(c1 - c2) <= 1)
                    return ViolationResult(true, "Bombs cannot touch each other, not even diagonally! ❌")
            }
        }

        return ViolationResult(false)
    }
}
