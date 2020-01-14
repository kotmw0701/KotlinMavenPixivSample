package jp.kotmw

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.SVGPath
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment
import javafx.stage.Stage
import jp.kotmw.pixiv.json.illust.IllustType
import jp.kotmw.pixiv.Pixiv
import jp.kotmw.pixiv.json.illust.IllustPages
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.util.*

class Controller: Initializable {

    @FXML
    lateinit var imageLists: FlowPane

    private val pixiv = Pixiv()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        var username = ""
        var password = ""
        if (File(".client").exists())
            BufferedReader(FileReader(".client")).use {
                username = it.readLine() ?: ""
                password = it.readLine() ?: ""
            }
        loginDialog(username, password)
    }

    fun onBookmarks(actionEvent: ActionEvent) {
        imageLists.children.clear()
        val bookmarkData = pixiv.userBookmarks(restrict = "private")

        bookmarkData.illusts.forEach { addImage(it.image_urls.small, it.type) }

        loadButton(bookmarkData)
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

    private fun loadButton(beforeData: IllustPages) {
        if (beforeData.next_url == null) return
        val label = Label("[Click]\nLoad next page")
        label.textAlignment = TextAlignment.CENTER
        label.font = Font(20.0)
        val vBox = VBox(label)
        vBox.alignment = Pos.CENTER
        vBox.setOnMouseClicked {
            imageLists.children.remove(vBox)
            val nextBookmarks = pixiv.loadNextPage(beforeData)
            if (nextBookmarks != null) {
                for (illust in nextBookmarks.illusts) addImage(illust.image_urls.small, illust.type)
                loadButton(nextBookmarks)
            }
        }
        imageLists.children.add(vBox)
    }

    private fun addImage(imageUrl: String, illustType: IllustType) {
        val imageView = ImageView()
        imageView.isPreserveRatio = true
        imageView.fitHeight = 150.0
        imageView.fitWidth = 150.0
        imageView.image = Image(pixiv.getImageStream(imageUrl))
        val vBox = VBox(imageView)
        vBox.setPrefSize(150.0, 150.0)
        vBox.alignment = Pos.CENTER
        vBox.styleClass.add("imageBox")
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

    private fun loginDialog(userName: String, password: String) {
        val stage = Stage()
        stage.isResizable = false
        stage.sizeToScene()
        val textField = TextField()
        textField.promptText = "ID or mail address"
        textField.text = userName
        textField.setPrefSize(240.0, 30.0)
        val passwordField = PasswordField()
        passwordField.promptText = "password"
        passwordField.text = password
        passwordField.setPrefSize(240.0, 30.0)
        val login = Button("Login")
        login.setPrefSize(240.0, 40.0)
        login.setOnAction {
            pixiv.login(textField.text, passwordField.text)
            stage.close()
        }
        val vBox = VBox(textField, passwordField, login)
        vBox.setPrefSize(300.0, 200.0)
        vBox.padding = Insets(30.0)
        vBox.spacing = 10.0
        stage.scene = Scene(vBox)
        stage.showAndWait()
    }
}
