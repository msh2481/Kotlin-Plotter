import org.jetbrains.skija.*
import java.io.File
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

fun getTicks(l: Float, r: Float) : List<Float> {
    fun countTicks(k: Float) = (floor(r / k) - ceil(l / k)).roundToInt()
    var k = 1.0f
    while (countTicks(k) > 10) {
        k *= 10
    }
    while (countTicks(k / 10) <= 10) {
        k /= 10
    }
    while (countTicks(k / 2) <= 10) {
        k /= 2
    }
    val result = mutableListOf<Float>()
    for (i in ceil(l / k).toInt()..floor(r / k).toInt()) {
        result.add(i.toFloat() * k)
    }
    return result
}

fun valueToColor(value: Int) : Int {
    check(value in 0..255) { "expected [0;256] but got $value" }
    return DAWN_COLORMAP[value]
}

data class DataSeries(val name: String, val data: List<Float>)

fun readCSV(filename: String): List<DataSeries>? {
    Log("starting with filename=$filename", "in readCSV")
    val matrix: List<List<String>> = File(filename).readLines().map{ it.split(',') }
    val n = matrix.size
    if (n == 0) {
        Log("The file is empty", "in readCSV", "error")
        println("The file is empty")
        return null
    }
    val m = matrix[0].size
    matrix.forEach {
        if (it.size != m) {
            Log("First row has $m columns, but now found row with ${it.size} columns", "in readCSV", "error")
            println("First row has $m columns, but now found row with ${it.size} columns")
            return null
        }
    }
    Log("read matrix $n x $m", "in readCSV")
    val dataframe = mutableListOf<DataSeries>()
    for (j in 0 until m) {
        val data = mutableListOf<Float>()
        for (i in 1 until n) {
            val cur = matrix[i][j].toFloatOrNull()
            if (cur == null) {
                Log("$i-th element of $j-th data series can't be converted to float: ${matrix[i][j]}", "in readCSV", "error")
                println("$i-th element of $j-th data series can't be converted to float: ${matrix[i][j]}")
                return null
            }
            data.add(cur)
        }
        dataframe.add(DataSeries(matrix[0][j], data))
    }
    return dataframe
}

fun readData(minN: Int, maxN: Int) : List<DataSeries>? {
    Log("starting", "in readData")
    require(minN <= maxN) {"minN <= maxN"}
    val filename = requireNotNull(parsedArgs["--data"]){"--data != null since parseArgs"}
    val df = readCSV(filename)
    if (df == null) {
        Log("Something went wrong when reading csv", "error", "in readData")
        println("Something went wrong when reading csv")
        return null
    }
    if (df.size !in minN..maxN) {
        Log("Need $minN..$maxN data series for this plot but got $df.size", "in readData", "error")
        println("Need 2 or 3 data series for kde plot but got $df.size")
        return null
    }
    if (df[0].data.isEmpty()) {
        Log("Need at least one point but got empty series", "in readData", "error")
        println("Need at least one point but got empty series")
        return null
    }
    return df
}

fun findExtrema(sequence: List<Float>) : List<Float> {
    require(sequence.isNotEmpty()) {"can't find extrema for empty sequence"}
    var min : Float? = null
    var max : Float? = null
    for (elem in sequence) {
        if (min == null || min > elem) {
            min = elem
        }
        if (max == null || max < elem) {
            max = elem
        }
    }
    requireNotNull(min) {"min != null"}
    requireNotNull(max) {"max != null"}
    if (min == max) {
        min -= 1
        max += 1
    }
    return listOf(min, max)
}

fun findXYBounds(df: List<DataSeries>, isXSeries: (Int) -> Boolean) : List<Float> =
    findExtrema(df.filterIndexed{ idx, _ -> isXSeries(idx)}.map{ it.data }.flatten()) +
    findExtrema(df.filterIndexed{ idx, _ -> !isXSeries(idx)}.map{ it.data }.flatten())

fun findZBounds(matrix: Array<FloatArray>) : List<Float> = findExtrema(matrix.map{ it.toList() }.flatten())

class AxisDrawer(
    private val canvas: Canvas,
    private val canvas2: Canvas,
    private val w: Int,
    private val h: Int,
    private val minX: Float,
    private val maxX: Float,
    private val minY: Float,
    private val maxY: Float) {

    private val displayMinX = 0.1f * w
    private val displayMaxX = 0.9f * w
    private val displayMinY = 0.1f * h
    private val displayMaxY = 0.9f * h
    private val fontSize = 0.007f * (w + h)
    private val tickW = 0.002f * (w + h)
    private val xTickCaptionOffset = 0.03f * h
    private val yTickCaptionOffset = 0.04f * w
    private val xAxisCaptionX = (displayMinX + displayMaxX) / 2
    private val xAxisCaptionY = displayMaxY + 2 * xTickCaptionOffset
    private val yAxisCaptionX = displayMinX - 2 * yTickCaptionOffset
    private val yAxisCaptionY = (displayMinY + displayMaxY) / 2

    private fun transformX(x: Float) : Float = displayMinX + (x - minX) / (maxX - minX) * (displayMaxX - displayMinX)
    private fun transformY(y: Float) : Float = displayMaxY + (y - minY) / (maxY - minY) * (displayMinY - displayMaxY)

    private fun drawThinLine(x0: Float, y0: Float, x1: Float, y1: Float) {
        val linePaint = Paint().setARGB(255, 0, 0, 0).setStrokeWidth(1f)
        canvas.drawLine(x0, y0, x1, y1, linePaint)
        canvas2.drawLine(x0, y0, x1, y1, linePaint)
    }
    private fun drawSmallText(x: Float, y: Float, s: String) {
        val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
        val font = Font(typeface, fontSize)
        val textPaint = Paint().setARGB(255, 0, 0, 0).setStrokeWidth(1f)
        canvas.drawString(s, x, y, font, textPaint)
        canvas2.drawString(s, x, y, font, textPaint)
    }

    fun drawAxis(xName: String, yName: String) {
        Log("starting", "in drawAxis")
        drawThinLine(displayMinX, displayMaxY, displayMaxX, displayMaxY)
        drawThinLine(displayMinX, displayMaxY, displayMinX, displayMinY)
        getTicks(minX, maxX).forEach{ tickX ->
            drawThinLine(transformX(tickX), displayMaxY + tickW, transformX(tickX), displayMaxY)
            drawSmallText(transformX(tickX), displayMaxY + xTickCaptionOffset, tickX.toString())
        }
        drawSmallText(xAxisCaptionX, xAxisCaptionY, xName)
        getTicks(minY, maxY).forEach { tickY ->
            drawThinLine(displayMinX, transformY(tickY), displayMinX - tickW, transformY(tickY))
            drawSmallText(displayMinX - yTickCaptionOffset, transformY(tickY), tickY.toString())
        }
        drawSmallText(yAxisCaptionX, yAxisCaptionY, yName)
    }

    fun drawMatrix(matrix: Array<FloatArray>) {
        Log("starting", "in drawMatrix")
        val resolution = matrix.size
        fun cellX(x: Int) : Float = displayMinX + x / resolution.toFloat() * (displayMaxX - displayMinX)
        fun cellY(y: Int) : Float = displayMaxY + y / resolution.toFloat() * (displayMinY - displayMaxY)
        fun drawCell(x: Int, y: Int, sz: Int, value: Int) {
            Log("starting", "in drawKDECell")
            require(value in 0..255) {"0 <= value < 256"}
            val cellColor = valueToColor(value)
            val cellPaint = Paint().setColor(cellColor)
            canvas.drawRect(Rect(cellX(x), cellY(y), cellX(x + sz), cellY(y + sz)), cellPaint)
            canvas2.drawRect(Rect(cellX(x), cellY(y), cellX(x + sz), cellY(y + sz)), cellPaint)
        }
        val (minZ, maxZ) = findZBounds(matrix)
        fun zToValue(z: Float) : Int = ((z - minZ) / (maxZ - minZ) * 255).roundToInt()
        drawCell(0, 0, resolution, 0)
        for (x in 0 until resolution) {
            for (y in 0 until resolution) {
                drawCell(x, y, 1, zToValue(matrix[x][y]))
            }
        }
    }

    fun drawPoint(x: Float, y: Float, color: Int, r: Float = 2f) {
        val pointPaint = Paint().setColor(color)
        canvas.drawCircle(transformX(x), transformY(y), r, pointPaint)
        canvas2.drawCircle(transformX(x), transformY(y), r, pointPaint)
    }

    fun drawLine(p: List<Point>, color: Int, r: Float = 1f) {
        val coords = p.map { pt ->
            val t = Point(transformX(pt.x), transformY(pt.y))
            listOf(t, t)
        }.flatten().drop(1).dropLast(1).toTypedArray()
        val linePaint = Paint().setColor(color).setStrokeWidth(r)
        canvas.drawLines(coords, linePaint)
        canvas2.drawLines(coords, linePaint)
    }
}