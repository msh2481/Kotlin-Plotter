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
import javax.swing.WindowConstants

object Log {
    val file = File("log${Clock.System.now().toString().substring(0, 19).replace(':', '-')}.txt")

    var predicate : (Array<out String>) -> Boolean = fun(tags: Array<out String>): Boolean {
        return true
    }
    operator fun invoke(message: String, vararg tags: String) {
        if (predicate(tags)) {
            file.appendText("${Clock.System.now()} | ${tags.toList()} | $message\n")
        }
    }
}

val help = """
    $ plot --type=scatter --data=data.csv --output=plot.png
""".trimIndent()

fun parseArgs(args: Array<String>) : Map<String, String> {
    val argsMap = mutableMapOf<String, String>()
    for (argument in args) {
        if (argument.count{ it == '='} != 1) {
            println("Can't understand argument (there should be exactly one '='): $argument")
            continue
        }
        val (key, value) = argument.split('=')
        Log("$key = $value", "in parseArgs")
    }
    return argsMap
}

fun main(args: Array<String>) {
    Log("starting", "in main")
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
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        val w = (width / contentScale).toInt()
        val h = (height / contentScale).toInt()

        // РИСОВАНИЕ

        layer.needRedraw()
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