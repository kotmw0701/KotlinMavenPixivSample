package jp.kotmw

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.FlowPane
import javafx.scene.layout.VBox
import jp.kotmw.parsed.illust.Illusts
import jp.kotmw.parsed.response.AuthResponse
import jp.kotmw.parsed.response.Response
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class Controller: Initializable {

    @FXML
    lateinit var username: TextField
    @FXML
    lateinit var password: PasswordField
    @FXML
    lateinit var imageLists: FlowPane

    private val authHost = "https://oauth.secure.pixiv.net"
    private val host = "https://app-api.pixiv.net"
    private val hashSecret = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    private lateinit var response : Response
    private var accessToken = ""
    private var userId = ""
    private var refreshToken = ""

    private val header = mutableMapOf(
        "host" to "oauth.secure.pixiv.net",
        "user-agent" to "PixivAndroidApp/5.0.156 (Android 9; ONEPLUS A6013)",
        "app-os" to "android",
        "app-os-version" to "5.0.156",
        "x-client-time" to "",
        "x-client-hash" to "",
        "content-type" to "application/x-www-form-urlencoded")

    private val data = mutableMapOf(
        "client_id" to "MOBrBDS8blbauoSck0ZfDbtuzpyT",
        "client_secret" to "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj",
        "grant_type" to "password",
        "username" to "",
        "password" to "",
        "get_secure_url" to "true",
        "include_policy" to "true")

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        BufferedReader(FileReader(".client")).use {
            username.text = it.readLine()
            password.text = it.readLine()
        }
    }

    fun onButton(actionEvent: ActionEvent) {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'")
        format.timeZone = TimeZone.getTimeZone("UTC")
        val localTime = format.format(Date())
        header["x-client-time"] = localTime
        header["x-client-hash"] = DigestUtils.md5Hex(localTime + hashSecret)
        data["username"] = username.text
        data["password"] = password.text
        val authResponse = Jsoup.connect("$authHost/auth/token")
            .method(Connection.Method.POST)
            .headers(header)
            .data(data)
            .ignoreContentType(true)
            .execute()

        println("Status : ${authResponse.statusCode()}")
        println("Cookies : ")
        authResponse.cookies().forEach { (t, u) -> println("\t$t : $u")}
        println("\nHeaders : ")
        authResponse.headers().forEach { (t, u) -> println("\t$t : $u")}
        println("\nBody : ")
        println(authResponse.body())

        val body = authResponse.body()
        this.response = body.parseJson<AuthResponse>().response
        this.accessToken = response.access_token
        this.refreshToken = response.refresh_token
        this.userId = response.user.id

        println("------ Illusts ------")

//        val rankingJson = mapper.readValue<Illusts>(_testRanking("day"))
//
//        for (illust in rankingJson.illusts) {
//            println("\n${illust.title.decode()} : ${illust.user.name.decode()}")
//            println("\tType : ${illust.type}")
//            println("\tSanity : ${illust.sanity_level}")
//            if (illust.page_count == 1) {
//                println("\tOriginal : ${illust.meta_single_page.original_image_url}")
//            } else {
//                for (page in illust.meta_pages)
//                    println("\tOriginal : ${page.image_urls.original}")
//            }
//        }
    }

    private fun _testAuthRequest(
        method: Connection.Method = Connection.Method.GET,
        url: String,
        headers: MutableMap<String, String> = mutableMapOf(),
        params: Map<String, String> = mapOf(),
        data: Map<String, String> = mapOf()
    ): String {
        if (headers["user-agent"].isNullOrEmpty() && headers["User-Agent"].isNullOrEmpty()) {
            headers["App-OS"] = "ios"
            headers["App-OS-Version"] = "12.2"
            headers["App-Version"] = "7.6.2"
            headers["User-Agent"] = "PixivIOSApp/7.6.2 (iOS 12.2; iPhone9,1)"
        }
        headers["Authorization"] = "Bearer $accessToken"
        val requestCall = Jsoup.connect(url)
            .method(method)
            .headers(headers)
            .data(params)
            .data(data)
            .ignoreContentType(true)
            .execute()

        return requestCall.body()
    }

    private fun _testRanking(
        mode: String = "day",
        filter: String = "for_ios",
        date: String = "",
        offset: Int = 0
    ): String {
        val url = "$host/v1/illust/ranking"
        val param = mutableMapOf(
            "mode" to mode,
            "filter" to filter
        )
        if (date.isNotEmpty())
            param["date"] = date
        if (offset > 0)
            param["offset"] = offset.toString()
        return _testAuthRequest(Connection.Method.GET, url, params = param)
    }

    private fun _userBookmarks(
        userId: String = this.userId,
        restrict: String = "public",
        filter: String = "for_ios",
        maxBookmarkId: Int = 0,
        tag: String = ""
    ): Illusts {
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
        return _testAuthRequest(Connection.Method.GET, url, params = param).parseJson()
    }

    private fun _userNextBookmarks(beforeData: Illusts): Illusts {
        return _testAuthRequest(Connection.Method.GET, beforeData.next_url).parseJson()
    }

//          val type = imageUrl.split(".").last()
//
//          ImageIO.write(ImageIO.read(ByteArrayInputStream(stream)), type, File("C:\\Image\\${imageUrl.split("/").last()}"))
    private fun _loadIllust(imageUrl: String, referer: String = "https://app-api.pixiv.net"): Image {
        println("Url : $imageUrl")
        val imageResponse =  Jsoup.connect(imageUrl)
            .method(Connection.Method.GET)
            .header("Referer", referer)
            .ignoreContentType(true)
            .maxBodySize(0)
            .execute()
        val byte = imageResponse.bodyStream()
        return Image(byte)
    }

    fun onBookmarks(actionEvent: ActionEvent) {
        val bookmarkData = _userBookmarks(restrict = "private")

        for (illust in bookmarkData.illusts) {
            println("\n${illust.title.decode()} : ${illust.user.name.decode()}")
            println("\tType : ${illust.type}")
            if (illust.page_count == 1) {
                addImage(illust.meta_single_page.original_image_url.toString())
            } else {
                addImage(illust.meta_pages[0].image_urls.original.toString())
            }
        }
        println("Next -> ${bookmarkData.next_url}")
    }

    private fun addImage(imageUrl: String) {
        val imageView = ImageView()
        imageView.isPreserveRatio = true
        imageView.fitHeight = 200.0
        imageView.fitWidth = 200.0
        imageView.image = _loadIllust(imageUrl)
        val vBox = VBox(imageView)
        vBox.alignment = Pos.CENTER
        vBox.setPrefSize(200.0, 200.0)
        imageLists.children.add(vBox)
    }
}
