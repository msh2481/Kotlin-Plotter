import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.datetime.Clock
import org.jetbrains.skija.*
import org.jetbrains.skija.Canvas._nReadPixels
import org.jetbrains.skija.Canvas._nWritePixels
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.Optional.of
import javax.swing.WindowConstants
import kotlin.io.path.Path
import kotlin.system.exitProcess


/** TODO
 * Docs, readme
 * remove i, j
 * more parameters to kde
 * kde-average, line, histogram
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
        val surface = Surface.makeRasterN32Premul(800, 600)
        plots["scatter"] = { scatter(canvas, surface.canvas, w, h) }
        plots["kde-sum"] = { kde(canvas, surface.canvas, w, h, KDEAlgorithm.SUM) }
        plots["kde-average"] = { kde(canvas, surface.canvas, w, h, KDEAlgorithm.AVERAGE) }

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
        Log("writing output file", "in onRender")
        val image = surface.makeImageSnapshot();
        val pngData = image.encodeToData(EncodedImageFormat.PNG)
        val pngBytes: ByteBuffer = pngData!!.toByteBuffer()
        try {
            val output = requireNotNull(parsedArgs["--output"]) {"--output should be not null since parseArgs"}
            val path = Path(output)
            val channel: ByteChannel = Files.newByteChannel(
                path,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
            )
            channel.write(pngBytes)
            channel.close()
        } catch (e: IOException) {
            println("Failed to write output file")
            Log("caught $e", "in onRender", "error", "exception")
            exitProcess(0)
        }
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