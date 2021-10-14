import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.*

internal class Test1 {
    fun testOneGetTicks(l: Float, r: Float) {
        val x = getTicks(l, r)
        assert(x.size in 6..10) { "|getTicks| in 6..10" }
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
}
