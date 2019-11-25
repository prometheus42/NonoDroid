package de.ichmann.android.nonodroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import org.freenono.model.GameBoard
import org.freenono.model.GameState
import org.freenono.model.Token
import org.freenono.model.data.Nonogram
import java.io.*
import kotlin.math.floor


class NonogramView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val TAG = "NonogramView"

    private lateinit var currentNonogram: Nonogram

    /********************* Constants *********************/

    private val MIN_CAPTION_HEIGHT = 5
    private val MIN_CAPTION_WIDTH = 5
    private val MAX_TILE_SIZE = 60.0f
    private val MIN_TILE_SIZE = 24.0f
    private val DEFAULT_TILE_SIZE = 100.0f
    // set correction value for centering text in caption tiles
    private val xCorrectionSize: Float = 0.25f
    private val yCorrectionSize: Float = 0.75f

    private var tileSize: Float = DEFAULT_TILE_SIZE
    private var paddingTop: Float = 0f
    private var paddingLeft: Float = 0f

    private lateinit var gameBoard: GameBoard
    private lateinit var gameController: GameController

    /********************* Input handling *********************/

    private val gameBoardGesturesListener =  object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }
    private val detector: GestureDetector = GestureDetector(context, gameBoardGesturesListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameController.gameStatus != GameState.RUNNING) {
            return false
        }
        if (event.action == MotionEvent.ACTION_DOWN) {
            val xTile = floor((event.x - paddingLeft - currentNonogram.lineCaptionWidth * tileSize) / tileSize).toInt()
            val yTile = floor((event.y - paddingTop - currentNonogram.columnCaptionHeight * tileSize) / tileSize).toInt()
            if (0 <= xTile && xTile < currentNonogram.width() && 0 <= yTile && yTile < currentNonogram.height()) {
                Log.i(TAG, "Clicked on tile (" + xTile + ", " + yTile + ").")
                if (gameBoard.canOccupy(xTile, yTile)) {
                    if (!gameBoard.occupy(xTile, yTile)) {
                        gameController.applyPenalty()
                    }
                    invalidate()
                }
                // check for game end
                if (gameController.isSolved()){
                    gameController.stop()
                    Log.i(TAG, "Nonogram was solved, game ended.")
                    Toast.makeText(context, "Nonogram was solved!!!", Toast.LENGTH_LONG).show()
                }
                if (gameController.isLost()) {
                    gameController.stop()
                    Log.i(TAG, "Game was lost.")
                    Toast.makeText(context, "You lost the game!", Toast.LENGTH_LONG).show()
                }
            }
        }
        else if (event.action == MotionEvent.ACTION_HOVER_ENTER) {
            Log.i(TAG, "Hover Event!")
        }
        /*return detector.onTouchEvent(event).let { result ->
            if (!result) {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    System.out.println("xxx")
                    true
                } else false
            } else true
        }*/
        return true
    }


    /********************* Custom drawing *********************/

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val tileCountWidth = currentNonogram.width() + Math.max(MIN_CAPTION_WIDTH, currentNonogram.lineCaptionWidth + 1)
        val tileCountHeight = currentNonogram.height() + Math.max(MIN_CAPTION_HEIGHT, currentNonogram.columnCaptionHeight + 1)

        val tileWidth = w / tileCountWidth * 1.0f
        val tileHeight = h / tileCountHeight * 1.0f
        tileSize = Math.min(tileHeight, tileWidth)

        paddingLeft = (w - (currentNonogram.width() + currentNonogram.lineCaptionWidth) * tileSize) / 2
        paddingTop = (h - (currentNonogram.height() + currentNonogram.columnCaptionHeight) * tileSize) / 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // set style for captions
        val captionPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GRAY
            textSize = 0.75f * tileSize
            typeface = Typeface.MONOSPACE
        }

        // TODO: Account for padding on View.
        // TODO: Move calculations to onSizeChanged() and use only canvas.apply{} here.

        // paint column captions
        for (x in 0 .. currentNonogram.width()) {
            val xCurrentBlock = (x + currentNonogram.lineCaptionWidth) * tileSize + paddingLeft
            canvas.drawLine(xCurrentBlock, paddingTop, xCurrentBlock,
                    currentNonogram.columnCaptionHeight * tileSize + paddingTop, captionPaint)
        }

        // draw column caption labels
        for (x in 0 until currentNonogram.width()) {
            val xCurrentBlock = (x + currentNonogram.lineCaptionWidth + xCorrectionSize) * tileSize + paddingLeft
            val numbers = currentNonogram.getColumnNumbers(x)
            val blockDifference = currentNonogram.columnCaptionHeight - currentNonogram.getColumnNumbersCount(x)
            for ((i,n) in numbers.withIndex()) {
                canvas.drawText(n.toString(), xCurrentBlock, (blockDifference + i + yCorrectionSize) * tileSize + paddingTop, captionPaint)
            }
        }

        // paint row captions
        for (y in 0 .. currentNonogram.height()) {
            val yCurrentBlock = (y + currentNonogram.columnCaptionHeight) * tileSize + paddingTop
            canvas.drawLine(paddingLeft, yCurrentBlock,
                    currentNonogram.lineCaptionWidth * tileSize + paddingLeft, yCurrentBlock, captionPaint)
        }

        // draw row caption labels
        for (y in 0 until currentNonogram.height()) {
            val yCurrentBlock = (y + currentNonogram.columnCaptionHeight + yCorrectionSize) * tileSize + paddingTop
            val numbers = currentNonogram.getLineNumbers(y)
            val blockDifference = currentNonogram.lineCaptionWidth - currentNonogram.getLineNumberCount(y)
            for ((i,n) in numbers.withIndex()) {
                canvas.drawText(n.toString(), (blockDifference + i + xCorrectionSize) * tileSize + paddingLeft, yCurrentBlock, captionPaint)
            }
        }

        // set style for game board
        val p = Paint().apply {
            textSize = 50.0f
        }

        // paint game board
        for (y in 0 until currentNonogram.height()) {
            for (x in 0 until currentNonogram.width()) {
                when (gameBoard.getFieldValue(x, y)) {
                    Token.FREE -> p.style = Paint.Style.STROKE
                    Token.OCCUPIED -> p.style = Paint.Style.FILL
                    Token.MARKED -> p.style = Paint.Style.STROKE
                    else -> throw Exception("Unexpected value for Token found in GameBoard!")
                }
                canvas.drawRect((x+currentNonogram.getLineCaptionWidth())*tileSize + paddingLeft,
                        (y+currentNonogram.getColumnCaptionHeight())*tileSize + paddingTop,
                        (x+currentNonogram.getLineCaptionWidth())*tileSize + tileSize + paddingLeft,
                        (y+currentNonogram.getColumnCaptionHeight())*tileSize + tileSize + paddingTop, p)
            }
        }

    }

    /********************* Game logic methods *********************/

    fun setNonogram(nonogramFile: InputStream) {
        val a = AndroidNonogramSerializer()
        val l = a.load(nonogramFile)
        currentNonogram = l.first()
        gameBoard = GameBoard(currentNonogram)
        gameController = GameController(currentNonogram, gameBoard)
        gameController.start()
        // refresh View
        invalidate()
        requestLayout()
    }

    fun stop() {
        gameController.stop()
    }
}