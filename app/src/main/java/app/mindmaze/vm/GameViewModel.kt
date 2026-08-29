package app.mindmaze.vm

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import app.mindmaze.data.model.PuzzleLevel

class GameViewModel : ViewModel() {

    val currentLevelIndex = mutableStateOf(0)
    val hasWon = mutableStateOf(false)

    private val _flatBoard = mutableStateListOf<Int>()
    private var boardSize: Int = 0

    val boardState: List<List<Int>>
        get() = if (boardSize == 0) emptyList() else _flatBoard.chunked(boardSize)

    val isBoardReady: Boolean
        get() = boardSize > 0 && _flatBoard.size == boardSize * boardSize

    fun initBoard(size: Int, level: PuzzleLevel?) {
        boardSize = size
        _flatBoard.clear()
        repeat(size * size) { _flatBoard.add(0) }
        hasWon.value = false
    }

    /**
     * Places a bomb (tap / double tap). If the cell already holds a bomb, taps clear it
     * instead, giving the player a quick undo. Returns true only when a bomb was newly
     * placed (i.e. not when a placed bomb was cleared), so callers know when to trigger
     * boom feedback and run the placement-validation logic.
     */
    fun placeBomb(row: Int, col: Int): Boolean {
        if (boardSize == 0) return false
        val index = row * boardSize + col
        if (index !in _flatBoard.indices) return false
        return if (_flatBoard[index] == 2) {
            _flatBoard[index] = 0
            false
        } else {
            _flatBoard[index] = 2
            true
        }
    }

    /**
     * Places an X (tap/swipe/drag). A swipe only affects empty cells, so a stroke
     * re-crossing the same cell is a no-op and a placed bomb is never overwritten by an
     * accidental swipe. A single tap on a cell that already holds an X clears it back to
     * empty instead, giving the player a quick way to undo a mark.
     */
    fun placeX(row: Int, col: Int, isSwipe: Boolean = false) {
        if (boardSize == 0) return
        val index = row * boardSize + col
        if (index !in _flatBoard.indices) return
        when {
            _flatBoard[index] == 0 -> _flatBoard[index] = 1
            _flatBoard[index] == 1 && !isSwipe -> _flatBoard[index] = 0
        }
    }

    /**
     * Auto-marks X on every cell a bomb at (row, col) puts off-limits: the rest of its
     * row, the rest of its column, and the 8 touching cells (including diagonals) — the
     * same row/column/adjacency rules RulesValidator enforces. Only fills currently-empty
     * cells, so it never overwrites another bomb or an existing mark. Called right after a
     * bomb is placed, as a placement aid.
     */
    fun autoMarkAroundBomb(row: Int, col: Int) {
        if (boardSize == 0) return
        fun markIfEmpty(r: Int, c: Int) {
            if (r !in 0 until boardSize || c !in 0 until boardSize) return
            val index = r * boardSize + c
            if (_flatBoard[index] == 0) _flatBoard[index] = 1
        }
        for (c in 0 until boardSize) if (c != col) markIfEmpty(row, c)
        for (r in 0 until boardSize) if (r != row) markIfEmpty(r, col)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            markIfEmpty(row + dr, col + dc)
        }
    }

    fun clearCell(row: Int, col: Int) {
        if (boardSize == 0) return
        val index = row * boardSize + col
        if (index in _flatBoard.indices) _flatBoard[index] = 0
    }

    /**
     * Turns a just-invalidated bomb into a red "error X" (state 3), marking the cell as
     * tried-and-wrong instead of leaving it empty (used when a placement violates the
     * rules and a life is lost). Unlike a normal X, this stays red permanently until the
     * cell is retried (long-press/double-tap places a bomb over it again) or the level
     * resets — it's a deliberate, lasting mistake marker, not just a flash.
     */
    fun markInvalidBombAsErrorX(row: Int, col: Int) {
        if (boardSize == 0) return
        val index = row * boardSize + col
        if (index in _flatBoard.indices) _flatBoard[index] = 3
    }

    fun resetBoard() {
        _flatBoard.replaceAll { 0 }
        hasWon.value = false
    }

    fun restoreBoardState(board: List<List<Int>>) {
        if (board.size != boardSize || board.flatten().size != boardSize * boardSize) return
        board.forEachIndexed { row, rowList ->
            rowList.forEachIndexed { col, value ->
                val index = row * boardSize + col
                _flatBoard[index] = value.coerceIn(0, 3)
            }
        }
    }
}