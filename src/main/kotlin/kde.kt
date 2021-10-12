import org.jetbrains.skija.*
import kotlin.math.roundToInt
import kotlin.random.Random

enum class KDEAlgorithm {
    SUM,
    AVERAGE
}

fun kde(canvas: Canvas, canvas2: Canvas, w: Int, h: Int, algo: KDEAlgorithm) {
    Log("starting", "in kde")
    val nCells = 256
    val displayMinX = 0.1f * w
    val displayMaxX = 0.9f * w
    val displayMinY = 0.1f * h
    val displayMaxY = 0.9f * h
    val fontSize = 0.008f * (w + h)
    val tickW = 0.002f * (w + h)
    val tickCaptionOffsetRatio = 0.05f
    val xAxisCaptionDx = 0.01f * w
    val xAxisCaptionDy = 0f
    val yAxisCaptionDx = -0.01f * w
    val yAxisCaptionDy = -0.03f * h

    fun readData() : List<NamedList>? {
        var df = readCSV(requireNotNull(parsedArgs["--data"]) { "--data should be not null since parseArgs" })
        if (df.size != 2 && df.size != 3) {
            Log("Need 2 or 3 data series for kde plot but got $df.size", "in kde", "error")
            println("Need 2 or 3 data series for kde plot but got $df.size")
            return null
        }
        if (df[0].data.size == 0) {
            Log("Need at least one point but got empty series", "in kde", "error")
            println("Need at least one point but got empty series")
            return null
        }
        if (df.size == 2) {
            df = df + NamedList("z", List(df[0].data.size) { "1" })
        }
        return df
    }
    fun findXYBounds(df: List<NamedList>) : List<Float>? {
        val n = df.size
        val m = df[0].data.size
        var minX : Float? = null
        var maxX : Float? = null
        var minY : Float? = null
        var maxY : Float? = null
        for (series in 0..n-1) {
            for (point in 0..m-1) {
                val cur = df[series].data[point].toFloatOrNull()
                if (cur == null) {
                    Log("$point-th point of $series-th series isn't float", "kde scatter", "error")
                    println("$point-th point of $series-th series isn't float")
                    return null
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
        return listOf(minX, maxX, minY, maxY)
    }

    val df = readData() ?: return
    val (minX, maxX, minY, maxY) = findXYBounds(df) ?: return

    fun blurMatrix(matrix0: Array<FloatArray>, size: Int) : Array<FloatArray> {
        var matrix = matrix0
        fun getOrZero(x: Int, y: Int) : Float = if ((x in 0..nCells-1) && (y in 0..nCells-1)) matrix[x][y] else 0f
        repeat(size) {
            val buffer = Array(nCells) { FloatArray(nCells) {0f} }
            for (x in 0..nCells-1) {
                for (y in 0..nCells-1) {
                    val sum = getOrZero(x, y) + getOrZero(x, y-1) + getOrZero(x, y+1) + getOrZero(x-1, y) + getOrZero(x+1, y)
                    buffer[x][y] = sum / 5f
                }
            }
            matrix = buffer
        }
        return matrix
    }
    fun xToCell(x: Float) : Int = ((x - minX) / (maxX - minX) * (nCells - 1)).roundToInt()
    fun yToCell(y: Float) : Int = ((y - minY) / (maxY - minY) * (nCells - 1)).roundToInt()
    fun transformX(xCell: Int) : Float = displayMinX + xCell.toFloat() / nCells.toFloat() * (displayMaxX - displayMinX)
    fun transformY(yCell: Int) : Float = displayMaxY + yCell.toFloat() / nCells.toFloat() * (displayMinY - displayMaxY)
    fun buildMatrix() : Array<FloatArray> {
        var sumMatrix = Array(nCells) { FloatArray(nCells) { 0f } }
        var cntMatrix = Array(nCells) { FloatArray(nCells) { 0f } }
        (df[0].data zip df[1].data zip df[2].data).forEach { (xy, zPoint) ->
            val x = xToCell(xy.first.toFloat())
            val y = yToCell(xy.second.toFloat())
            val z = zPoint.toFloat()
            sumMatrix[x][y] += z
            cntMatrix[x][y] += 1f
        }
        return when (algo) {
            KDEAlgorithm.AVERAGE -> {
                val size = parsedArgs["--size"]?.toIntOrNull() ?: 32
                sumMatrix = blurMatrix(sumMatrix, size)
                cntMatrix = blurMatrix(cntMatrix, size)
                for (x in 0..nCells - 1) {
                    for (y in 0..nCells - 1) {
                        if (cntMatrix[x][y] != 0f) {
                            sumMatrix[x][y] /= cntMatrix[x][y]
                        } else {
                            Log("zero count encountered", "in kde", "kde-average", "warn")
                            sumMatrix[x][y] = 0f
                        }
                    }
                }
                sumMatrix
            }
            KDEAlgorithm.SUM -> {
                val size = parsedArgs["--size"]?.toIntOrNull() ?: 32
                blurMatrix(sumMatrix, size)
            }
        }
    }
    fun drawThinLine(x0: Float, y0: Float, x1: Float, y1: Float) {
        val linePaint = Paint().setARGB(255, 0, 0, 0).setStrokeWidth(1f)
        canvas.drawLine(x0, y0, x1, y1, linePaint)
        canvas2.drawLine(x0, y0, x1, y1, linePaint)
    }
    fun drawSmallText(x: Float, y: Float, s: String) {
        val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
        val font = Font(typeface, fontSize)
        val textPaint = Paint().setARGB(255, 0, 0, 0).setStrokeWidth(1f)
        canvas.drawString(s, x, y, font, textPaint)
        canvas2.drawString(s, x, y, font, textPaint)
    }
    fun drawCell(x: Int, y: Int, value: Int, sz: Int = 1) {
        val cellColor = valueToColor(value)
        val cellPaint = Paint().setColor(cellColor)
        Log("starting with x, y, color = $x, $y, $cellColor", "in drawCell")
        canvas.drawRect(Rect(transformX(x), transformY(y), transformX(x + sz), transformY(y + sz)), cellPaint)
        canvas2.drawRect(Rect(transformX(x), transformY(y), transformX(x + sz), transformY(y + sz)), cellPaint)
    }
    fun drawAxis() {
        drawThinLine(displayMinX, displayMaxY, displayMaxX, displayMaxY)
        drawThinLine(displayMinX, displayMaxY, displayMinX, displayMinY)
        for (tickX in 0..nCells step nCells / 8) {
            drawThinLine(transformX(tickX), displayMaxY + tickW, transformX(tickX), displayMaxY)
            drawSmallText(transformX(tickX), displayMaxY + tickCaptionOffsetRatio * h, tickX.toString())
        }
        drawSmallText(displayMaxX + xAxisCaptionDx, displayMaxY + xAxisCaptionDy, df[0].name)
        for (tickY in 0..nCells step nCells / 8) {
            drawThinLine(displayMinX, transformY(tickY), displayMinX - tickW, transformY(tickY))
            drawSmallText(displayMinX - tickCaptionOffsetRatio * w, transformY(tickY), tickY.toString())
        }
        drawSmallText(displayMinX + yAxisCaptionDx, displayMinY + yAxisCaptionDy, df[1].name)
        drawCell(0, 0, 0, nCells)
    }
    fun findZBounds(matrix: Array<FloatArray>) : List<Float> {
        var minZ : Float? = null
        var maxZ : Float? = null
        for (row in matrix) {
            for (z in row) {
                if (minZ == null || minZ > z) {
                    minZ = z
                }
                if (maxZ == null || maxZ < z) {
                    maxZ = z
                }
            }
        }
        requireNotNull(minZ) { "minZ != null" }
        requireNotNull(maxZ) { "maxZ != null" }
        if (maxZ == minZ) {
            minZ -= 1
            maxZ += 1
        }
        return listOf(minZ, maxZ)
    }

    val matrix = buildMatrix()
    val (minZ, maxZ) = findZBounds(matrix)
    fun zToValue(z: Float) : Int = ((z - minZ) / (maxZ - minZ) * 255).roundToInt()

    drawAxis()
    for (x in 0..nCells-1) {
        for (y in 0..nCells-1) {
            drawCell(x, y, zToValue(matrix[x][y]))
        }
    }
}