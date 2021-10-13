import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Point
import kotlin.random.Random

fun line(canvas: Canvas, canvas2: Canvas, w: Int, h: Int) {
    Log("starting", "in scatter")
    val df = readData(2, 100) ?: return
    val (minX, maxX, minY, maxY) = findXYBounds(df) { it == 0 }
    val drawer = AxisDrawer(canvas, canvas2, w, h, minX, maxX, minY, maxY)

    drawer.drawAxis("x", "y")
    val seededRandom = Random(1)
    val xSeries = df[0]
    df.drop(1).forEach{ ySeries ->
        val maxColor = 256 * 256 * 256
        val curColor = seededRandom.nextInt(maxColor) - maxColor
        val points = (xSeries.data zip ySeries.data).map{ (x, y) -> Point(x, y) }
        drawer.drawLine(points, curColor)
    }
}