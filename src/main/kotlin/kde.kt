import org.jetbrains.skija.Canvas
import kotlin.math.roundToInt

enum class KDEAlgorithm {
    SUM,
    AVERAGE
}

fun blurMatrix(matrix0: Array<FloatArray>, size: Int) : Array<FloatArray> {
    val nCells = matrix0.size
    var matrix = matrix0
    fun getOrZero(x: Int, y: Int) : Float = if ((x in matrix.indices) && (y in matrix[x].indices)) matrix[x][y] else 0f
    repeat(size) {
        val buffer = Array(nCells) { FloatArray(nCells) {0f} }
        for (x in matrix.indices) {
            for (y in matrix[x].indices) {
                val sum = getOrZero(x, y) + getOrZero(x, y-1) + getOrZero(x, y+1) + getOrZero(x-1, y) + getOrZero(x+1, y)
                buffer[x][y] = sum / 5f
            }
        }
        matrix = buffer
    }
    return matrix
}

fun divideMatrices(numerator: Array<FloatArray>, denominator: Array<FloatArray>) : Array<FloatArray> {
    Log("starting", "in divideMatrices")
    for (row in numerator.indices) {
        for (column in numerator[row].indices) {
            if (denominator[row][column] != 0f) {
                numerator[row][column] /= denominator[row][column]
            } else {
                Log("zero denominator encountered", "in divideMatrices", "warn")
                numerator[row][column] = 0f
            }
        }
    }
    return numerator
}

fun kde(canvas: Canvas, canvas2: Canvas, w: Int, h: Int, algo: KDEAlgorithm) {
    Log("starting", "in kde")
    val resolution = parsedArgs["resolution"]?.toIntOrNull() ?: 64
    var df = readData(2, 3) ?: return
    if (df.size == 2) {
        df = df + DataSeries("values", List(df[0].data.size){1f})
    }

    val (minX, maxX, minY, maxY) = findXYBounds(df)
    fun xToCell(x: Float) : Int = ((x - minX) / (maxX - minX) * (resolution - 1)).roundToInt()
    fun yToCell(y: Float) : Int = ((y - minY) / (maxY - minY) * (resolution - 1)).roundToInt()

    var sumMatrix = Array(resolution) { FloatArray(resolution) { 0f } }
    var cntMatrix = Array(resolution) { FloatArray(resolution) { 0f } }
    (df[0].data zip df[1].data zip df[2].data).forEach { (xy, z) ->
        val x = xToCell(xy.first)
        val y = yToCell(xy.second)
        sumMatrix[x][y] += z
        cntMatrix[x][y] += 1f
    }
    val blurSize = parsedArgs["--blur-size"]?.toIntOrNull() ?: 32
    sumMatrix = blurMatrix(sumMatrix, blurSize)
    if (algo == KDEAlgorithm.AVERAGE) {
        cntMatrix = blurMatrix(cntMatrix, blurSize)
        sumMatrix = divideMatrices(sumMatrix, cntMatrix)
    }

    val drawer = AxisDrawer(canvas, canvas2, w, h, minX, maxX, minY, maxY)
    drawer.drawAxis(df[0].name, df[1].name)
    drawer.drawMatrix(sumMatrix)
}