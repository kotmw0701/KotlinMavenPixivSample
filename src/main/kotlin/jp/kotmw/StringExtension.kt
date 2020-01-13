package jp.kotmw

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.commons.text.StringEscapeUtils

fun String.decode(): String {
    return StringEscapeUtils.unescapeJson(this)
}

inline fun <reified T> String.parseJson(): T {
    return jacksonObjectMapper().readValue(this)
}
