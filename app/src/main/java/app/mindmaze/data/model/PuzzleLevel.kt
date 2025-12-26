package app.mindmaze.data.model

typealias PuzzleLevel = Map<Int, List<Pair<Int, Int>>>

data class ViolationData(
    val violatedCells: Set<Pair<Int, Int>>,
    val violatedQueens: Set<Pair<Int, Int>>,
    val queensPerRegion: Map<Int, Int>
)

enum class CellState { EMPTY, X, QUEEN }