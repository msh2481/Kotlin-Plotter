import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

fun getTicks(l: Float, r: Float) : List<Float> {
    fun countTicks(k: Float) = (floor(r / k) - ceil(l / k)).roundToInt()
    var k = 1.0f
    while (countTicks(k) > 10) {
        k *= 10
    }
    while (countTicks(k / 10) <= 10) {
        k /= 10
    }
    if (countTicks(k / 2) <= 10) {
        k /= 2
    }
    val result = mutableListOf<Float>()
    for (i in ceil(l / k).toInt()..floor(r / k).toInt()) {
        result.add(i.toFloat() * k)
    }
    return result
}