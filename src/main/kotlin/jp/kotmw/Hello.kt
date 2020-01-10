package jp.kotmw

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class Main : Application() {
    override fun start(primaryStage: Stage?) {
        val message = Label("Hello, world. This is JavaFX from Kotlin.")
        val root = VBox(message)
        root.alignment = Pos.CENTER
        primaryStage?.scene = Scene(root, 600.0, 400.0)
        primaryStage?.isResizable = false
        primaryStage?.sizeToScene()
        primaryStage?.show()
    }
}

val hash_secret = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

val header = mutableMapOf(
    "host" to "oauth.secure.pixiv.net",
    "user-agent" to "PixivAndroidApp/5.0.156 (Android 9; ONEPLUS A6013)",
    "app-os" to "android",
    "app-os-version" to "5.0.156",
    "x-client-time" to "",
    "x-client-hash" to "",
    "content-type" to "application/x-www-form-urlencoded")

val data = mutableMapOf(
    "client_id" to "MOBrBDS8blbauoSck0ZfDbtuzpyT",
    "client_secret" to "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj",
    "grant_type" to "password",
    "username" to "",
    "password" to "",
    "get_secure_url" to "true",
    "include_policy" to "true")

fun main(args: Array<String>) {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'")
    format.timeZone = TimeZone.getTimeZone("UTC")
    val localTime = format.format(Date())
    header["x-client-time"] = localTime
    header["x-client-hash"] = DigestUtils.md5Hex(localTime + hash_secret)
    data["username"] = "supurasyu0701"
    data["password"] = "motlofgaruda3110"
    val response = Jsoup.connect("https://oauth.secure.pixiv.net/auth/token")
        .method(Connection.Method.POST)
        .headers(header)
        .data(data)
        .ignoreContentType(true)
        .execute()

    println("Status : ${response.statusCode()}")
    println("Cookies : ")
    response.cookies().forEach { (t, u) -> println("\t$t : $u")}
    println("\nHeaders : ")
    response.headers().forEach { (t, u) -> println("\t$t : $u")}
    println("\nBody : ")
    println(response.body())

    Application.launch(Main::class.java, *args)
}
