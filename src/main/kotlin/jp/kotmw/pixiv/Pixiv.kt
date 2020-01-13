package jp.kotmw.pixiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jp.kotmw.parseJson
import jp.kotmw.parsed.response.AuthResponse
import jp.kotmw.parsed.response.Response
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

class Pixiv {

    private lateinit var configuration: Configuration
    private val host = "https://app-api.pixiv.net"
    private val authHost = "https://oauth.secure.pixiv.net"
    private val clientId = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    private val clientSecret = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    private val hash = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    var accessToken: String = ""
    var userId = ""

    init {
        val config = File(".config")
        if (config.exists())
            BufferedReader(FileReader(".config")).use { configuration = it.readText().parseJson() }
        else {
            configuration = Configuration(null)
            updateConfig()
        }

    }

    fun login(userName: String, password: String) {
        auth(userName, password)
    }

    fun enableAuth(accessToken: String, refreshToken: String = "") {
        this.accessToken = accessToken
        this.configuration.refreshToken = refreshToken
        updateConfig()
    }

    private fun auth(userName: String = "", password: String = "", refreshToken: String = "") {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'")
        format.timeZone = TimeZone.getTimeZone("UTC")
        val localTime = format.format(Date())
        val url = "$authHost/auth/token"
        val headers = mutableMapOf(
            "host" to "oauth.secure.pixiv.net",
            "user-agent" to "PixivAndroidApp/5.0.64 (Android 6.0)",
            "x-client-time" to localTime,
            "x-client-hash" to DigestUtils.md5Hex(localTime + this.hash),
            "content-type" to "application/x-www-form-urlencoded"
        )
        val data = mutableMapOf(
            "get_secure_url" to "1",
            "client_id" to this.clientId,
            "client_secret" to this.clientSecret
        )

        if (refreshToken.isNotEmpty() || !this.configuration.refreshToken.isNullOrEmpty()) {
            data["grant_type"] = "refresh_token"
            data["refresh_token"] = if (refreshToken.isNotEmpty()) refreshToken else this.configuration.refreshToken.toString()
        } else if (userName.isNotEmpty() && password.isNotEmpty()) {
            data["grant_type"] = "password"
            data["username"] = userName
            data["password"] = password
        } else throw IllegalArgumentException("password or refreshToken is not set.")

        val response = request(Connection.Method.POST, url, headers, data)
        if (!listOf(200, 301, 302).contains(response.statusCode())) {
            if (data["grant_type"] == "password")
                throw IllegalArgumentException("Authentication Failed. Check your username and password.\n" +
                        "HTTP ${response.statusCode()} : ${response.body()}")
            else throw IllegalArgumentException("Authentication Failed. Check your Refresh Token.\n" +
                    "HTTP ${response.statusCode()} : ${response.body()}")
        }

        println("Status : ${response.statusCode()}")
        println("Cookies : ")
        response.cookies().forEach { (t, u) -> println("\t$t : $u")}
        println("\nHeaders : ")
        response.headers().forEach { (t, u) -> println("\t$t : $u")}
        println("\nBody : ")
        println(response.body())

        val authResponse: Response = response.body().parseJson<AuthResponse>().response
        this.accessToken = authResponse.access_token
        this.userId = authResponse.user.id
        this.configuration.refreshToken = authResponse.refresh_token
        updateConfig()
    }

    private fun request(
        method: Connection.Method = Connection.Method.GET,
        url: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        data: Map<String, String> = mapOf()
    ): Connection.Response {
        return Jsoup.connect(url)
            .method(method)
            .headers(headers)
            .data(data)
            .ignoreContentType(true)
            .execute()
    }

    private fun updateConfig() {
        PrintWriter(FileWriter(".config")).use {
            it.print(jacksonObjectMapper().writeValueAsString(configuration))
        }
//        Files.setAttribute(Paths.get(".config"), "dos:hidden", true)
    }
}

data class Configuration(
    var refreshToken: String? = null
)
