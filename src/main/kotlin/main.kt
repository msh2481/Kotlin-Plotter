import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.util.*
import javax.swing.WindowConstants
import kotlin.system.exitProcess
import kotlin.random.Random


/** TODO
 * Docs, readme
 * Files
 * 2d kernel estimation, scatter, line, diagram
 */

fun prettyTime() = Clock.System.now().toString().substring(0, 19).replace(':', '-')

object Log {
    val file = File("log${prettyTime()}.txt")
    val tagsCounter: MutableMap<SortedSet<String>, Int> = mutableMapOf()
    var predicate : (Array<out String>) -> Boolean = fun(tags: Array<out String>): Boolean {
        val tagSet = tags.toSortedSet()
        val prevCnt = tagsCounter.getOrDefault(tagSet, 0)
        tagsCounter[tagSet] = prevCnt + 1
        if (prevCnt > 10) {
            return false
        }
        return true
    }
    operator fun invoke(message: String, vararg tags: String) {
        if (predicate(tags)) {
            file.appendText("${prettyTime()} | ${tags.toList()} | $message\n")
        }
    }
}

data class NamedList(val name: String, val data: List<String>)

fun readCSV(filename: String): List<NamedList> {
    Log("starting with filename=$filename", "in readCSV")
    val matrix: List<List<String>> = File(filename).readLines().map{ it.split(',') }
    val n = matrix.size
    if (n == 0) {
        Log("The file is empty", "in readCSV", "error")
        println("The file is empty")
        return listOf()
    }
    val m = matrix[0].size
    matrix.forEach {
        if (it.size != m) {
            Log("First row has $m columns, but now found row with ${it.size} columns", "in readCSV", "error")
            println("First row has $m columns, but now found row with ${it.size} columns")
            return listOf()
        }
    }
    Log("read matrix $n x $m", "in readCSV")
    val dataframe = mutableListOf<NamedList>()
    for (j in 0..m-1) {
        val data = mutableListOf<String>()
        for (i in 1..n-1) {
            data.add(matrix[i][j])
        }
        dataframe.add(NamedList(matrix[0][j], data))
    }
    return dataframe
}

val help = """
    $ plot --type=scatter --data=data.csv --output=plot.png
""".trimIndent()

fun parseArgs(args: Array<String>) : Map<String, String> {
    Log("starting", "in parseArgs")
    val argsMap = mutableMapOf<String, String>()
    for (argument in args) {
        if (argument.count{ it == '='} != 1) {
            Log("Can't understand argument (there should be exactly one '='): $argument", "in parseArgs", "warn")
            println("Can't understand argument (there should be exactly one '='): $argument")
            continue
        }
        val (key, value) = argument.split('=')
        argsMap[key] = value
        Log("$key = $value", "in parseArgs")
    }
    for (mandatory in listOf("--type", "--data", "--output")) {
        if (argsMap[mandatory] == null) {
            Log("Missed mandatory argument $mandatory, terminating", "in parseArgs", "error")
            println("Missed mandatory argument $mandatory, terminating")
            exitProcess(0)
        }
    }
    Log("finishing", "in parseArgs")
    return argsMap
}

var parsedArgs : Map<String, String> = mutableMapOf()

fun main(args: Array<String>) {
    Log("starting", "in main")
    parsedArgs = parseArgs(args)
    createWindow("Your plot")
    Log("finishing", "in main")
}

fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseMotionListener(MyMouseMotionAdapter)

    window.preferredSize = Dimension(800, 600)
    window.minimumSize = Dimension(100,100)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}

class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 40f)
    val paint = Paint().apply {
        color = 0xff9BC730L.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        Log("starting", "in onRender")
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        val w = (width / contentScale).toInt()
        val h = (height / contentScale).toInt()

        val plots = mutableMapOf<String, () -> Unit >()
        plots["scatter"] = fun() {
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

            val seededRandom = Random(19)
            for (ySeries in 1..n-1) {
                val currentPaint = Paint().setARGB(255, seededRandom.nextInt(256), seededRandom.nextInt(256), seededRandom.nextInt(256))
                for (point in 0..m-1) {
                    val x0 = df[0].data[point].toFloat()
                    val y0 = df[ySeries].data[point].toFloat()
                    val x = displayMinX + (x0 - minX) / (maxX - minX) * (displayMaxX - displayMinX)
                    val y = displayMinY + (y0 - minY) / (maxY - minY) * (displayMaxY - displayMinY)
                    canvas.drawCircle(x.toFloat(), y.toFloat(), 3f, currentPaint)
                }
            }
        }
        val plotType = requireNotNull(parsedArgs["--type"]) {"--type should be not null since parseArgs"}
        val plotFunc = plots[plotType]
        if (plotFunc == null) {
            Log("No such plot type ($plotType), terminating", "in onRender", "error")
            println("No such plot type ($plotType), terminating")
            exitProcess(0)
        }
        Log("calling plotFunc", "in onRender")
        plotFunc()
        layer.needRedraw()
        Log("starting", "in onRender")
    }
}

object State {
    var mouseX = 0f
    var mouseY = 0f
}

object MyMouseMotionAdapter : MouseMotionAdapter() {
    override fun mouseMoved(event: MouseEvent) {
        State.mouseX = event.x.toFloat()
        State.mouseY = event.y.toFloat()
    }
}