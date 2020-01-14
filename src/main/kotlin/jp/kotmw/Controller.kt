package jp.kotmw

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.SVGPath
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import jp.kotmw.pixiv.IllustType
import jp.kotmw.pixiv.Pixiv
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.util.*

class Controller: Initializable {

    @FXML
    lateinit var username: TextField
    @FXML
    lateinit var password: PasswordField
    @FXML
    lateinit var imageLists: FlowPane

    private val host = "https://app-api.pixiv.net"

    private val pixiv = Pixiv()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        if (File(".client").exists())
            BufferedReader(FileReader(".client")).use {
                username.text = it.readLine() ?: ""
                password.text = it.readLine() ?: ""
            }
    }

    fun onButton(actionEvent: ActionEvent) {
        pixiv.login(username.text, password.text)
    }

    fun onBookmarks(actionEvent: ActionEvent) {
        imageLists.children.clear()
        val bookmarkData = pixiv.userBookmarks(restrict = "private")

        for (illust in bookmarkData.illusts) {
            println("\n${illust.title.decode()} : ${illust.user.name.decode()}")
            println("\tType : ${illust.type}")
            addImage(illust.image_urls.small, illust.type)
        }
        val label = Label("[Click]\nLoad next page")
        label.textAlignment = TextAlignment.CENTER
        label.font = Font(20.0)
        val vBox = VBox(label)
        vBox.alignment = Pos.CENTER
        vBox.setOnMouseClicked {
            imageLists.children.remove(vBox)
            val nextBookmarks = pixiv.userNextBookmarks(bookmarkData)
            for (illust in nextBookmarks.illusts) addImage(illust.image_urls.small, illust.type)
        }

        imageLists.children.add(vBox)
    }

    fun onRankings(actionEvent: ActionEvent) {
        imageLists.children.clear()
        val rankingData = pixiv.rankings()

        for (illust in rankingData.illusts) {
            addImage(illust.image_urls.small, illust.type)
        }
    }
//          val type = imageUrl.split(".").last()
//
//          ImageIO.write(ImageIO.read(ByteArrayInputStream(stream)), type, File("C:\\Image\\${imageUrl.split("/").last()}"))

    private fun addImage(imageUrl: String, illustType: IllustType) {
        val imageView = ImageView()
        imageView.isPreserveRatio = true
        imageView.image = Image(pixiv.getImageStream(imageUrl))
//        println("${imageView.image.width} : ${imageView.image.height}")
        val vBox = VBox(imageView)
        vBox.setPrefSize(150.0, 150.0)
        vBox.alignment = Pos.CENTER
        val pane = Pane(vBox)
        if (illustType == IllustType.Ugoira)
            pane.children.add(ugoiraSign())
        imageLists.children.add(pane)
    }

    private fun ugoiraSign(): Pane {
        val play = SVGPath()
        play.content = "M16 16v17.108a2 2 0 002.992 1.736l14.97-8.554a2 2 0 000-3.473l-14.97-8.553A2 2 0 0016 16z"
        play.fill = Color.WHITE
        play.translateX = -4.0
        play.translateY = -4.0
        val pane = Pane(Circle(20.0, 20.0, 20.0, Color.web("#00000064")), play)
        pane.translateX = 55.0
        pane.translateY = 55.0
        return pane
    }
}
