package de.ichmann.android.nonodroid

import android.util.Xml
import org.freenono.model.data.DifficultyLevel
import org.freenono.model.data.Nonogram
import org.freenono.serializer.data.NonogramFormatException
import org.freenono.serializer.data.NonogramSerializer
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class AndroidNonogramSerializer : NonogramSerializer {

    private val FIELD_FREE_CHAR: Char = '_'
    private val FIELD_OCCUPIED_CHAR: Char = 'x'

    // We don't use namespaces
    private val ns: String? = null

    override fun save(f: File?, vararg nonograms: Nonogram?) {

    }

    fun save(ostream: OutputStream?, vararg nonograms: Nonogram?) {

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