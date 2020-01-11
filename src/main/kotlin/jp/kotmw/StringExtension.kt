package jp.kotmw

import org.apache.commons.text.StringEscapeUtils

fun String.decode(): String {
    return StringEscapeUtils.unescapeJson(this)
}
