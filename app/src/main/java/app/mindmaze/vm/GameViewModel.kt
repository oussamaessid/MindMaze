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

    fun toggleCell(row: Int, col: Int) {
        if (boardSize == 0) return
        val index = row * boardSize + col
        if (index in _flatBoard.indices) {
            val current = _flatBoard[index]
            _flatBoard[index] = (current + 1) % 3
        }
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
                _flatBoard[index] = value.coerceIn(0, 2)
            }
        }
    }
}