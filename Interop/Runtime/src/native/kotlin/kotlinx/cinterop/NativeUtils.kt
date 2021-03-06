package kotlinx.cinterop

import konan.internal.Intrinsic

internal fun decodeFromUtf8(bytes: ByteArray): String = kotlin.text.fromUtf8Array(bytes, 0, bytes.size)

fun encodeToUtf8(str: String): ByteArray {
    val result = ByteArray(str.length)

    for (index in 0 .. str.length - 1) {
        val char = str[index]
        if (char.toInt() >= 128) {
            TODO("non-ASCII char")
        }
        result[index] = char.toByte()
    }
    return result
}

@Intrinsic
external fun bitsToFloat(bits: Int): Float

@Intrinsic
external fun bitsToDouble(bits: Long): Double

@Intrinsic
external fun <R : Number> Number.signExtend(): R

@Intrinsic
external fun <R : Number> Number.narrow(): R

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
internal annotation class JvmName(val name: String)