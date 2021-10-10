import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Font
import org.jetbrains.skija.Paint
import org.jetbrains.skija.Typeface
import kotlin.math.roundToInt
import kotlin.random.Random

fun scatter(canvas: Canvas, canvas2: Canvas, w: Int, h: Int) {
    Log("starting", "in scatter")
    val df = readCSV(requireNotNull(parsedArgs["--data"]){"--data should be not null since parseArgs"})
    val n = df.size
    if (n < 2) {
        Log("Need at least 2 data series for scatter plot but got $n", "in scatter", "error")
        println("Need at least to data series for scatter plot but got $n")
        return
    }
    val m = df[0].data.size
    if (m == 0) {
        Log("Need at least one point but got empty series", "in scatter", "error")
        println("Need at least one point but got empty series")
        return
    }
    var minX : Float? = null
    var maxX : Float? = null
    var minY : Float? = null
    var maxY : Float? = null
    for (series in 0..n-1) {
        for (point in 0..m-1) {
            val cur = df[series].data[point].toFloatOrNull()
            if (cur == null) {
                Log("$point-th point of $series-th series isn't float", "in scatter", "error")
                println("$point-th point of $series-th series isn't float")
                return
            }
            if (series == 0) {
                if (minX == null || minX > cur) {
                    minX = cur
                }
                if (maxX == null || maxX < cur) {
                    maxX = cur
                }
            } else {
                if (minY == null || minY > cur) {
                    minY = cur
                }
                if (maxY == null || maxY < cur) {
                    maxY = cur
                }
            }
        }
    }
    requireNotNull(minX) { "minX != null" }
    requireNotNull(maxX) { "maxX != null" }
    requireNotNull(minY) { "minY != null" }
    requireNotNull(maxY) { "maxY != null" }

    if (minX == maxX) {
        minX -= 1
        maxX += 1
    }
    if (minY == maxY) {
        minY -= 1
        maxY += 1
    }
    val displayMinX = 0.1f * w
    val displayMaxX = 0.9f * w
    val displayMinY = 0.1f * h
    val displayMaxY = 0.9f * h

    fun transformX(x: Float) : Float = displayMinX + (x - minX) / (maxX - minX) * (displayMaxX - displayMinX)
    fun transformY(y: Float) : Float = displayMinY + (y - minY) / (maxY - minY) * (displayMaxY - displayMinY)

    fun drawThinLine(x0: Float, y0: Float, x1: Float, y1: Float) {
        val linePaint = Paint().setARGB(255, 0, 0, 0).setStrokeWidth(1f)
        canvas.drawLine(x0, y0, x1, y1, linePaint)
        canvas2.drawLine(x0, y0, x1, y1, linePaint)
    }
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    fun drawSmallText(x: Float, y: Float, s: String) {
        val font = Font(typeface, (h + w) * 0.008f)
        val textPaint = Paint().setARGB(255, 0, 0, 0).setStrokeWidth(1f)
        canvas.drawString(s, x, y, font, textPaint)
    }
    drawThinLine(displayMinX, displayMaxY, displayMaxX, displayMaxY)
    drawThinLine(displayMinX, displayMaxY, displayMinX, displayMinY)
    val tickW = 0.002f * (w + h)
    getTicks(minX, maxX).forEach{ tickX ->
        drawThinLine(transformX(tickX), displayMaxY + tickW, transformX(tickX), displayMaxY)
        drawSmallText(transformX(tickX), displayMaxY + 0.05f * h, tickX.toString())
    }
    drawSmallText(displayMaxX + 0.01f * w, displayMaxY, df.slice(0 until n step 2).map{ it.name }.toString())
    getTicks(minY, maxY).forEach { tickY ->
        drawThinLine(displayMinX, transformY(tickY), displayMinX - tickW, transformY(tickY))
        drawSmallText(displayMinX - 0.05f * w, transformY(tickY), tickY.toString())
    }
    drawSmallText(displayMinX - 0.05f * w, displayMinY, df.slice(1 until n step 2).map{ it.name }.toString())
    val seededRandom = Random(1)

    df.chunked(2).forEach{ (xSeries, ySeries) ->
        val currentPaint = Paint().setARGB(255, seededRandom.nextInt(256), seededRandom.nextInt(256), seededRandom.nextInt(256))
        xSeries.data.zip(ySeries.data).forEach{ (xPoint, yPoint) ->
            val x = transformX(xPoint.toFloat())
            val y = transformY(yPoint.toFloat())
            canvas.drawCircle(x, y, 2f, currentPaint)
            canvas2.drawCircle(x, y, 2f, currentPaint)
        }
    }
}