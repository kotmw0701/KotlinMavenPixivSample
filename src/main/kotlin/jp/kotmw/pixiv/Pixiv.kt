package jp.kotmw.pixiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jp.kotmw.parseJson
import jp.kotmw.pixiv.json.illust.IllustPages
import jp.kotmw.pixiv.json.response.AuthResponse
import jp.kotmw.pixiv.json.response.Response
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class Pixiv {

    private lateinit var configuration: Configuration
    private val host = "https://app-api.pixiv.net"
    private val authHost = "https://oauth.secure.pixiv.net"
    private val clientId = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    private val clientSecret = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    private val hash = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"

    private var accessToken: String = ""
    private var userId = ""

    init {
        val config = File(".config")
        if (config.exists())
            BufferedReader(FileReader(".config")).use { configuration = it.readText().parseJson() }
        else {
            configuration = Configuration(null)
            updateConfig()
        }

    }

    fun login(userName: String, password: String) = auth(userName, password)

    fun login(refreshToken: String = configuration.refreshToken ?: "") = auth(refreshToken = refreshToken)

    fun hasRefreshToken(): Boolean = !configuration.refreshToken.isNullOrEmpty()

    fun rankings(
        mode: String = "day",
        filter: String = "for_ios",
        date: String = "",
        offset: Int = 0
    ): IllustPages {
        val url = "$host/v1/illust/ranking"
        val param = mutableMapOf(
            "mode" to mode,
            "filter" to filter
        )
        if (date.isNotEmpty())
            param["date"] = date
        if (offset > 0)
            param["offset"] = offset.toString()
        return apiRequest(url = url, data = param).parseJson()
    }

    fun userBookmarks(
        userId: String = this.userId,
        restrict: String = "public",
        filter: String = "for_ios",
        maxBookmarkId: Int = 0,
        tag: String = ""
    ): IllustPages {
        val url = "$host/v1/user/bookmarks/illust"
        val param = mutableMapOf(
            "user_id" to userId,
            "restrict" to restrict,
            "filter" to filter
        )
        if (maxBookmarkId > 0)
            param["max_bookmark_id"] = maxBookmarkId.toString()
        if (tag.isNotEmpty())
            param["tag"] = tag
        return apiRequest(url = url, data = param).parseJson()
    }

    fun loadNextPage(beforeData: IllustPages): IllustPages? {
        val url = beforeData.next_url
        if (url != null)
            return apiRequest(url = beforeData.next_url).parseJson()
        return null
    }

    fun getImageStream(imageUrl: String, referer: String = "https://app-api.pixiv.net"): BufferedInputStream {
        val imageResponse =  Jsoup.connect(imageUrl)
            .method(Connection.Method.GET)
            .header("Referer", referer)
            .ignoreContentType(true)
            .maxBodySize(0)
            .execute()
        return imageResponse.bodyStream()
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

        if (userName.isNotEmpty() && password.isNotEmpty()) {
            data["grant_type"] = "password"
            data["username"] = userName
            data["password"] = password
        } else if (refreshToken.isNotEmpty() || this.hasRefreshToken()) {
            data["grant_type"] = "refresh_token"
            data["refresh_token"] = if (refreshToken.isNotEmpty()) refreshToken else this.configuration.refreshToken.toString()
        } else throw IllegalArgumentException("password or refreshToken is not set.")

        val response = request(Connection.Method.POST, url, headers, data)

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
            .ignoreHttpErrors(true)
            .execute()
    }

    private fun apiRequest(
        method: Connection.Method = Connection.Method.GET,
        url: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        data: Map<String, String> = mapOf(),
        authenticated: Boolean = true
    ):String {
        if (headers["user-agent"].isNullOrEmpty() && headers["User-Agent"].isNullOrEmpty()) {
            headers["App-OS"] = "ios"
            headers["App-OS-Version"] = "12.2"
            headers["App-Version"] = "7.6.2"
            headers["User-Agent"] = "PixivIOSApp/7.6.2 (iOS 12.2; iPhone9,1)"
        }
        return if (authenticated) {
            headers["Authorization"] = "Bearer $accessToken"
            request(method, url, headers, data).body()
        } else request(method, url, headers, data).body()
    }

    private fun updateConfig() {
        PrintWriter(FileWriter(".config")).use {
            it.print(jacksonObjectMapper().writeValueAsString(configuration))
        }
    }
}

data class Configuration(
    var refreshToken: String? = null
)
