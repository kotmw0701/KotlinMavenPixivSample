package jp.kotmw.pixiv

import jp.kotmw.Configuration
import jp.kotmw.parseJson
import java.io.BufferedReader
import java.io.FileReader

class Pixiv {

    lateinit var config: Configuration

    init {
        BufferedReader(FileReader(".config")).use {
            config = it.readLine().parseJson()
        }
    }
}
