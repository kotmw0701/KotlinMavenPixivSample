package jp.kotmw.pixiv.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val mapper = jacksonObjectMapper()

inline fun <reified T> String.parseJson(): T {
    return mapper.readValue(this)
}

inline fun <reified T> String.parseJson(rootName: String): T {
    return mapper.readerFor(T::class.java).withRootName(rootName).readValue(this)
}
