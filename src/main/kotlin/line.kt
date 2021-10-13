import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Point
import kotlin.random.Random

fun interpolate(arr: List<Float>, middlePoints: Int, blurSize: Int) : List<Float> {
    Log("starting", "in interpolate")
    require(middlePoints >= 0) {"middlePoints >= 0"}
    require(blurSize >= 0) {"blurSize >= 0"}
    require(arr.isNotEmpty()) {"arr not empty"}
    var big = MutableList(middlePoints) {arr.first()}
    arr.zipWithNext().forEach{ (l, r) ->
        for (i in 0..middlePoints) {
            big.add(l + (r - l) * i / (middlePoints + 1))
        }
    }
    big.addAll(List(middlePoints + 1) {arr.last()})
    repeat(blurSize) {
        var tmp = big
        for (i in 1..big.size-2) {
            tmp[i] = (big[i - 1] + big[i + 1]) / 2
        }
        big = tmp
    }
    return big
}

fun plotLine(drawer: AxisDrawer, xs: DataSeries, ys: DataSeries, color: Int) {
    val middlePoints = parsedArgs["--middle-points"]?.toIntOrNull() ?: 0
    val blurSize = parsedArgs["--blur-size"]?.toIntOrNull() ?: 0
    val fixedXs = interpolate(xs.data, middlePoints, blurSize)
    val fixedYs = interpolate(ys.data, middlePoints, blurSize)
    val points = (fixedXs zip fixedYs).map{ (x, y) -> Point(x, y) }
    drawer.drawLine(points, color)
    points.forEach{ pt -> drawer.drawPoint(pt.x, pt.y, color) }
}

fun line(canvas: Canvas, canvas2: Canvas, w: Int, h: Int) {
    Log("starting", "in scatter")
    val df = readData(2, 100) ?: return
    val (minX, maxX, minY, maxY) = findXYBounds(df) { it == 0 }
    val drawer = AxisDrawer(canvas, canvas2, w, h, minX, maxX, minY, maxY)

    drawer.drawAxis("x", "y")
    val seededRandom = Random(1)
    val xSeries = df[0]
    val maxColor = 256 * 256 * 256
    df.drop(1).forEach{ ySeries ->
        plotLine(drawer, xSeries, ySeries, seededRandom.nextInt(maxColor) - maxColor)
    }
}