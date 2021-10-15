import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.*

internal class Test1 {
    fun testOneGetTicks(l: Float, r: Float) {
        val x = getTicks(l, r)
        assert(x.size in 5..10) { "|getTicks($l, $r)| = ${x.size}, not in 5..10" }
        x.forEach{
            assert(it in l..r)
        }
    }
    @Test
    fun testGetTicks() {
        for (i in 1..10) {
            testOneGetTicks(0f, i.toFloat())
        }
        repeat(100) {
            val a = Random.nextFloat()
            val b = Random.nextFloat()
            testOneGetTicks(min(a, b), max(a, b))
        }
        repeat(100) {
            val a = 1f / Random.nextFloat()
            val b = 1f / Random.nextFloat()
            testOneGetTicks(min(a, b), max(a, b))
        }
    }
    @Test
    fun testInterpolate() {
        assertEquals(listOf(1f, 2f, 10f), interpolate(listOf(1f, 2f, 10f), 0, 0))
        assertEquals(listOf(1f, 1f, 1.5f, 2f, 6f, 10f, 10f), interpolate(listOf(1f, 2f, 10f), 1, 0))
        assertEquals(listOf(1f, 1.25f, 1.625f, 3.8125f, 6.90625f, 8.453125f, 10f), interpolate(listOf(1f, 2f, 10f), 1, 1))
    }
    fun testOneReadmePlot(args: List<String>, filename: String) {
        main(args.toTypedArray())
        val process = Runtime.getRuntime().exec("diff output.png ${filename}")
        val result = process.inputStream.reader().readText()
        println("result = ${result}")
    }
}
