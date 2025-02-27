package no.nav.sokos.spk.mottak.util

import kotlin.math.pow
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CopyBookField(
    val name: String = "",
    val length: Int,
    val decimal: Int = 0,
    val type: CopyBookType,
)

enum class CopyBookType {
    INTEGER,
    STRING,
    DECIMAL,
}

inline fun <reified T : Any> T.toCopyBookFormat(): String {
    val clazz = T::class
    val sb = StringBuilder()
    clazz.memberProperties.forEach { property ->
        val annotation = property.findAnnotation<CopyBookField>()
        val value = property.getter.call(this)?.toString() ?: ""

        if (annotation != null) {
            when (annotation.type) {
                CopyBookType.INTEGER -> sb.append(value.padStart(annotation.length, '0'))
                CopyBookType.STRING -> sb.append(value.padEnd(annotation.length, ' '))
                CopyBookType.DECIMAL -> {
                    val decimalFormat = "%.${annotation.decimal}f".format(value.toDouble())
                    sb.append(decimalFormat.replace(".", "").padStart(annotation.length, '0'))
                }
            }
        }
    }
    return sb.appendLine().toString()
}

inline fun <reified T : Any> String.toDataClass(): T {
    val clazz = T::class
    val constructor = clazz.primaryConstructor!!
    var currentIndex = 0
    val params =
        constructor.parameters.associateWith { param ->
            val property = clazz.memberProperties.find { it.name == param.name }!!
            val annotation = property.findAnnotation<CopyBookField>()!!
            val length = annotation.length
            val endIndex = (currentIndex + length).coerceAtMost(this.length)
            val valueString = this.substring(currentIndex, endIndex).trim()
            currentIndex += length

            when (annotation.type) {
                CopyBookType.INTEGER -> valueString.toIntOrNull()
                CopyBookType.STRING -> valueString
                CopyBookType.DECIMAL ->
                    valueString.toDoubleOrNull()?.let { value ->
                        value / 10.0.pow(annotation.decimal.toDouble())
                    }
            }
        }
    return constructor.callBy(params)
}
