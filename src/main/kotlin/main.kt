import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.EncodedImageFormat
import org.jetbrains.skija.Surface
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.swing.WindowConstants
import kotlin.io.path.Path
import kotlin.system.exitProcess


/** TODO
 * Docs, readme
 * unit test interpolate, getTicks
 * test every parameter
 */

val help = """
    Usage: plot [OPTIONS]
    
    Mandatory options:
    --type=ENUM             ENUM is one of 'scatter', 'line', 'kde-sum', 'kde-average'
    --data=FILE             read input from FILE
    
    Other options:
    --output=FILE           save plot as picture to FILE
    --blur-size=NUM         more blurring for bigger NUM (only for 'line', 'kde-sum', 'kde-average')
    --middle-points=NUM     number of points to add in interpolation (only for 'line')
    --resolution=NUM        to use NUMxNUM matrix for KDE (only for 'kde-sum' and 'kde-average')
""".trimIndent()

/**
 * Creates a map from given command line arguments. Expects them in form --name=value.
 */
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
    for (mandatory in listOf("--type", "--data")) {
        if (argsMap[mandatory] == null) {
            Log("Missed mandatory option $mandatory, terminating", "in parseArgs", "error")
            println("Missed mandatory option $mandatory, terminating")
            exitProcess(0)
        }
    }
    Log("finishing", "in parseArgs")
    return argsMap
}

var parsedArgs : Map<String, String> = mutableMapOf()

fun main(args: Array<String>) {
    Log("starting", "in main")
    println(help)
    parsedArgs = parseArgs(args)
    createWindow("Your plot")
    Log("finishing", "in main")
}

val W = 1600
val H = 800

fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseMotionListener(MyMouseMotionAdapter)

    window.preferredSize = Dimension(W, H)
    window.minimumSize = Dimension(100,100)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}

/**
 * Selects appropriate plotting function from a map based on command line arguments. Also handles all other graphics.
 */
class Renderer(val layer: SkiaLayer): SkiaRenderer {
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        Log("starting", "in onRender")
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        val w = (width / contentScale).toInt()
        val h = (height / contentScale).toInt()

        val plots = mutableMapOf<String, () -> Unit >()
        val surface = Surface.makeRasterN32Premul(W, H)
        plots["scatter"] = { scatter(canvas, surface.canvas, w, h) }
        plots["kde-sum"] = { kde(canvas, surface.canvas, w, h, KDEAlgorithm.SUM) }
        plots["kde-average"] = { kde(canvas, surface.canvas, w, h, KDEAlgorithm.AVERAGE) }
        plots["line"] = { line(canvas, surface.canvas, w, h) }

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
        val image = surface.makeImageSnapshot()
        val pngData = image.encodeToData(EncodedImageFormat.PNG)
        val pngBytes: ByteBuffer = pngData!!.toByteBuffer()
        try {
            parsedArgs["--output"]?.let{ output ->
                val path = Path(output)
                val channel: ByteChannel = Files.newByteChannel(
                    path,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE
                )
                channel.write(pngBytes)
                channel.close()
            }
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