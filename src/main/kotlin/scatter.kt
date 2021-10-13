import org.jetbrains.skija.Canvas
import kotlin.random.Random

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
    val seededRandom = Random(1)
    df.chunked(2).forEach{ (xSeries, ySeries) ->
        val maxColor = 256 * 256 * 256
        val curColor = seededRandom.nextInt(maxColor) - maxColor
        (xSeries.data zip ySeries.data).forEach{ (x, y) ->
            drawer.drawPoint(x, y, curColor)
        }
    }
}