import org.jetbrains.skija.Canvas
import kotlin.random.Random

/**
 *  Just plots given series of points at the right scale.
 *  Supports multiple series, they will be plotted with different colors
 */
fun scatter(canvas: Canvas, canvas2: Canvas, w: Int, h: Int) {
    Log("starting", "in scatter")
    val df = readData(2, 100) ?: return
    val n = df.size
    if (n % 2 == 1) {
        Log("Expected even number of data series for scatter plot but got $n", "in scatter", "error")
        println("Expected even number of data series for scatter plot but got $n")
        return
    }
    val (minX, maxX, minY, maxY) = findXYBounds(df) { it % 2 == 0 }
    val drawer = AxisDrawer(canvas, canvas2, w, h, minX, maxX, minY, maxY)

    drawer.drawAxis("x", "y")
    val seededRandom = Random(2)
    val colors = mutableListOf<Int>()
    val names = mutableListOf<String>()
    df.chunked(2).forEach{ (xSeries, ySeries) ->
        names.add("${xSeries.name}, ${ySeries.name}")
        if (xSeries.data.size != ySeries.data.size) {
            Log("One of given series have different length for x and y, skipping", "in scatter", "warn")
            println("One of given series have different length for x and y, skipping")
        } else {
            val maxColor = 256 * 256 * 256
            val curColor = seededRandom.nextInt(maxColor) - maxColor
            (xSeries.data zip ySeries.data).forEach { (x, y) ->
                drawer.drawPoint(x, y, curColor)
            }
            colors.add(curColor)
        }
    }
    drawer.drawLegend(names, colors)
}