package de.ichmann.android.nonodroid

import android.os.Handler
import android.os.Looper
import org.freenono.model.GameBoard
import org.freenono.model.GameState
import org.freenono.model.Token
import org.freenono.model.data.Nonogram

class GameController(val currentNonogram: Nonogram, val gameBoard: GameBoard) {

    private var remainingTime = when (currentNonogram.duration) {
        0L -> 30*60
        else -> currentNonogram.duration
    }
    private val penalties: List<Int> = listOf(1, 2, 4, 8);
    private var penaltyCount: Int = 0
    private val mainHandler = Handler(Looper.getMainLooper())
    var gameStatus: GameState = GameState.NONE

    fun start() {
        gameStatus = GameState.RUNNING
        // Source: https://stackoverflow.com/questions/55570990/kotlin-call-a-function-every-second/55571277
        mainHandler.post(object : Runnable {
            override fun run() {
                remainingTime -= 1
                System.out.println(remainingTime)
                if (remainingTime <= 0) {
                    gameStatus = GameState.GAME_OVER
                }
                if (!(gameStatus == GameState.USER_STOP || gameStatus == GameState.GAME_OVER || gameStatus == GameState.SOLVED)) {
                    mainHandler.postDelayed(this, 1000)
                }
            }
        })
    }

    fun stop() {
        gameStatus = GameState.USER_STOP
    }

    fun applyPenalty() {
        remainingTime -= penalties.get(Math.min(penaltyCount, penalties.size - 1)) * 60
        penaltyCount++;
    }

    fun isSolved() : Boolean {
        return isSolvedThroughOccupied()
    }

    fun isLost() : Boolean {
        return remainingTime <= 0
    }

    private fun isSolvedThroughMarked(): Boolean {
        var y: Int
        var x: Int
        var patternValue: Boolean
        var fieldValue: Token

        y = 0
        while (y < currentNonogram.height()) {
            x = 0
            while (x < currentNonogram.width()) {
                patternValue = currentNonogram.getFieldValue(x, y)
                fieldValue = gameBoard.getFieldValue(x, y)

                if (patternValue && fieldValue == Token.MARKED) {
                    return false

                } else if (!patternValue && fieldValue == Token.FREE) {
                    return false
                }
                x++
            }
            y++
        }
        return true
    }

    private fun isSolvedThroughOccupied(): Boolean {
        var y: Int
        var x: Int
        var patternValue: Boolean
        var fieldValue: Token

        y = 0
        while (y < currentNonogram.height()) {
            x = 0
            while (x < currentNonogram.width()) {
                patternValue = currentNonogram.getFieldValue(x, y)
                fieldValue = gameBoard.getFieldValue(x, y)

                if (patternValue && fieldValue != Token.OCCUPIED) {
                    return false
                }
                x++
            }
            y++
        }
        return true
    }

}