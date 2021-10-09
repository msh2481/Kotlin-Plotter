import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Paint
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
    val displayMinX = 0.1 * w
    val displayMaxX = 0.9 * w
    val displayMinY = 0.1 * h
    val displayMaxY = 0.9 * h

    val seededRandom = Random(1)
    for (ySeries in 1..n-1) {
        val currentPaint = Paint().setARGB(255, seededRandom.nextInt(256), seededRandom.nextInt(256), seededRandom.nextInt(256))
        for (point in 0..m-1) {
            val x0 = df[0].data[point].toFloat()
            val y0 = df[ySeries].data[point].toFloat()
            val x = displayMinX + (x0 - minX) / (maxX - minX) * (displayMaxX - displayMinX)
            val y = displayMinY + (y0 - minY) / (maxY - minY) * (displayMaxY - displayMinY)
            canvas.drawCircle(x.toFloat(), y.toFloat(), 3f, currentPaint)
            canvas2.drawCircle(x.toFloat(), y.toFloat(), 3f, currentPaint)
        }
    }
}