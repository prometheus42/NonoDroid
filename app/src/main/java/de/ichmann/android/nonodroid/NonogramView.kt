package de.ichmann.android.nonodroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Xml
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import org.freenono.model.GameBoard
import org.freenono.model.GameState
import org.freenono.model.Token
import org.freenono.model.data.DifficultyLevel
import org.freenono.model.data.Nonogram
import org.freenono.serializer.data.NonogramFormatException
import org.freenono.serializer.data.NonogramSerializer
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.util.*
import kotlin.math.floor


class AndroidNonogramSerializer : NonogramSerializer {

    private val FIELD_FREE_CHAR: Char = '_'
    private val FIELD_OCCUPIED_CHAR: Char = 'x'

    // We don't use namespaces
    private val ns: String? = null

    override fun save(f: File?, vararg nonograms: Nonogram?) {

    }

    fun save(ostream: OutputStream?,  vararg nonograms: Nonogram?) {

    }

    override fun load(f: File): Array<Nonogram> {
        // TODO: Check whether file is not a directory and if it actually exists
        return load(f.inputStream())
    }

    fun load(istream: InputStream) : Array<Nonogram> {
        val listOfNonograms = parse(istream)
        return listOfNonograms.toTypedArray()
    }

    /**
     * All details for reading XML files comes from Android's Developer
     * Documentation under https://developer.android.com/training/basics/network-ops/xml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(istream: InputStream): List<Nonogram> {
        istream.use { inputStream ->
            val parser: XmlPullParser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)
            parser.nextTag()
            return readNonograms(parser)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readNonograms(parser: XmlPullParser): List<Nonogram> {
        val listOfNonograms = mutableListOf<Nonogram>()

        parser.require(XmlPullParser.START_TAG, ns, "FreeNono")
        parser.nextTag()
        parser.require(XmlPullParser.START_TAG, ns, "Nonograms")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            // Starts by looking for the entry tag
            if (parser.name == "Nonogram") {
                listOfNonograms.add(readNonogram(parser))
            } else {
                skip(parser)
            }
        }
        return listOfNonograms
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readNonogram(parser: XmlPullParser): Nonogram {
        parser.require(XmlPullParser.START_TAG, ns, "Nonogram")
        var name: String = ""
        var author: String = ""
        var description: String = ""
        var difficulty: DifficultyLevel = DifficultyLevel.UNDEFINED
        var level: Int = 0
        var duration: Long = 0
        //var height: Int = 0
        //var width: Int = 0
        var lines: List<BooleanArray> = emptyList()

        name.let { name = parser.getAttributeValue(ns, "name") }
        author.let { author = parser.getAttributeValue(ns, "author") }
        description.let { description = parser.getAttributeValue(ns, "desc") }
        val l = parser.getAttributeValue(ns, "level")
        if (l != null){
            level = l.toInt()
        }
        val du = parser.getAttributeValue(ns, "duration")
        if (du != null){
            duration = du.toLong()
        }

        val di = parser.getAttributeValue(ns, "difficulty")
        if (di != null) {
            difficulty = DifficultyLevel.values()[di.toInt()]
        }
        //height.let { height = parser.getAttributeValue(ns, "height").toInt() }
        //width.let { width = parser.getAttributeValue(ns, "width").toInt() }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }
            when (parser.name) {
                "line" -> lines += readLine(parser)
                else -> skip(parser)
            }
        }
        val n: Nonogram = Nonogram(name, difficulty, lines.toTypedArray())
        n.author = author
        n.description = description
        n.level = level
        n.duration = duration
        return n
    }

    // Processes line tags containing the actual Nonogram data.
    @Throws(IOException::class, XmlPullParserException::class, NonogramFormatException::class)
    private fun readLine(parser: XmlPullParser): BooleanArray {
        parser.require(XmlPullParser.START_TAG, ns, "line")
        val line = readText(parser)
        val tokenizer = StringTokenizer(line)
        var values: BooleanArray = BooleanArray(tokenizer.countTokens())
        var i = 0
        for (t in tokenizer) {
            values[i] = when (t.toString()[0].toLowerCase()) {
                FIELD_FREE_CHAR -> false
                FIELD_OCCUPIED_CHAR -> true
                else -> throw NonogramFormatException("Couldn't find value for character!")
            }
            i++
        }
        parser.require(XmlPullParser.END_TAG, ns, "line")
        return values
    }

    // For the tags title and summary, extracts their text values.
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }
}

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

class NonogramView(context: Context, attrs: AttributeSet) : View(context, attrs) {

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
            val xTile = floor((event.x - currentNonogram.lineCaptionWidth * tileSize) / tileSize).toInt()
            val yTile = floor((event.y - currentNonogram.columnCaptionHeight * tileSize) / tileSize).toInt()
            if (0 <= xTile && xTile < currentNonogram.width() && 0 <= yTile && yTile < currentNonogram.height()) {
                System.out.println("Clicked on tile (" + xTile + ", " + yTile + ").")
                if (gameBoard.canOccupy(xTile, yTile)) {
                    if (!gameBoard.occupy(xTile, yTile)) {
                        gameController.applyPenalty()
                    }
                    invalidate()
                }
                // check for game end
                if (gameController.isSolved()){
                    gameController.stop()
                    Toast.makeText(context, "Nonogram was solved!!!", Toast.LENGTH_LONG).show()
                }
                if (gameController.isLost()) {
                    gameController.stop()
                    Toast.makeText(context, "You lost the game!", Toast.LENGTH_LONG).show()
                }
            }
        }
//        return detector.onTouchEvent(event).let { result ->
//            if (!result) {
//                if (event.action == MotionEvent.ACTION_DOWN) {
//                    Toast.makeText(context, "Clicked...", Toast.LENGTH_SHORT).show()
//                    true
//                } else false
//            } else true
//        }
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
            val xCurrentBlock = (x + currentNonogram.lineCaptionWidth) * tileSize
            canvas.drawLine(xCurrentBlock, 0.0f, xCurrentBlock,
                    currentNonogram.columnCaptionHeight * tileSize, captionPaint)
        }

        // draw column caption labels
        for (x in 0 until currentNonogram.width()) {
            val xCurrentBlock = (x + currentNonogram.lineCaptionWidth + xCorrectionSize) * tileSize
            val numbers = currentNonogram.getColumnNumbers(x)
            val blockDifference = currentNonogram.columnCaptionHeight - currentNonogram.getColumnNumbersCount(x)
            for ((i,n) in numbers.withIndex()) {
                canvas.drawText(n.toString(), xCurrentBlock, (blockDifference + i + yCorrectionSize)*tileSize, captionPaint)
            }
        }

        // paint row captions
        for (y in 0 .. currentNonogram.height()) {
            val yCurrentBlock = (y + currentNonogram.columnCaptionHeight) * tileSize
            canvas.drawLine(0.0f, yCurrentBlock,
                    currentNonogram.lineCaptionWidth*tileSize, yCurrentBlock, captionPaint)
        }

        // draw row caption labels
        for (y in 0 until currentNonogram.height()) {
            val yCurrentBlock = (y + currentNonogram.columnCaptionHeight + yCorrectionSize) * tileSize
            val numbers = currentNonogram.getLineNumbers(y)
            val blockDifference = currentNonogram.lineCaptionWidth - currentNonogram.getLineNumberCount(y)
            for ((i,n) in numbers.withIndex()) {
                canvas.drawText(n.toString(), (blockDifference + i + xCorrectionSize)*tileSize, yCurrentBlock, captionPaint)
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
                canvas.drawRect((x+currentNonogram.getLineCaptionWidth())*tileSize,
                        (y+currentNonogram.getColumnCaptionHeight())*tileSize,
                        (x+currentNonogram.getLineCaptionWidth())*tileSize+tileSize,
                        (y+currentNonogram.getColumnCaptionHeight())*tileSize+tileSize, p)
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