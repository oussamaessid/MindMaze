package app.mindmaze.data.repositoryImp

import app.mindmaze.data.model.PuzzleLevel

/**
 * A small local onboarding puzzle. It is intentionally separate from downloaded level 1,
 * so completing or resetting the guide can never alter the player's real progression.
 */
object GuideLevel {
    val level: PuzzleLevel = mapOf(
        0 to listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1),
        1 to listOf(0 to 3, 1 to 2, 1 to 3, 2 to 3),
        2 to listOf(1 to 0, 2 to 0, 2 to 1, 3 to 0),
        3 to listOf(2 to 2, 3 to 1, 3 to 2, 3 to 3)
    )

    // One per row, column and region; consecutive BOOMS never touch.
    val solution: List<Pair<Int, Int>> = listOf(
        0 to 1,
        1 to 3,
        2 to 0,
        3 to 2
    )

    /** Three adjacent non-solution cells used by the opening swipe lesson. */
    val swipeCells: Set<Pair<Int, Int>> = setOf(2 to 1, 2 to 2, 2 to 3)
}
