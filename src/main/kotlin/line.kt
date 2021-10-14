import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Point
import java.io.File
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

fun saveToFile(points: List<Point>, filename: String) {
    Log("starting", "in saveToFile")
    File(filename).writeText(buildString {
        points.forEach { pt ->
            appendLine("${pt.x}, ${pt.y}")
        }
    })
}

fun plotLine(drawer: AxisDrawer, xs: DataSeries, ys: DataSeries, color: Int) {
    Log("starting", "in plotLine")
    val middlePoints = parsedArgs["--middle-points"]?.toIntOrNull() ?: 0
    val blurSize = parsedArgs["--blur-size"]?.toIntOrNull() ?: 0
    val fixedXs = interpolate(xs.data, middlePoints, blurSize)
    val fixedYs = interpolate(ys.data, middlePoints, blurSize)
    val points = (fixedXs zip fixedYs).map{ (x, y) -> Point(x, y) }
    try {
        saveToFile(points, "preprocessed.csv")
    } catch (e: Exception) {
        Log("Can't save preprocessed data", "in plotLine", "error")
        println("Can't save preprocessed data")
    }
    drawer.drawLine(points, color)
    points.forEach{ pt -> drawer.drawPoint(pt.x, pt.y, color) }
}

fun line(canvas: Canvas, canvas2: Canvas, w: Int, h: Int) {
    Log("starting", "in scatter")
    val df = readData(2, 100) ?: return
    val (minX, maxX, minY, maxY) = findXYBounds(df) { it == 0 }
    val drawer = AxisDrawer(canvas, canvas2, w, h, minX, maxX, minY, maxY)

    drawer.drawAxis("x", "y")
    val seededRandom = Random(2)
    val xSeries = df[0]
    val maxColor = 256 * 256 * 256
    val colors = mutableListOf(-maxColor)
    df.drop(1).forEach{ ySeries ->
        val color = seededRandom.nextInt(maxColor) - maxColor
        plotLine(drawer, xSeries, ySeries, color)
        colors.add(color)
    }
    drawer.drawLegend(df.map{ it.name }, colors)
}