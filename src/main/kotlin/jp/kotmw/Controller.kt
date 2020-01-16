package jp.kotmw

import javafx.animation.ScaleTransition
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.SVGPath
import javafx.stage.Stage
import javafx.util.Duration
import jp.kotmw.pixiv.Pixiv
import jp.kotmw.pixiv.json.illust.IllustPages
import jp.kotmw.pixiv.json.illust.IllustType
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.URL
import java.util.*
import kotlin.system.exitProcess

class Controller {

    @FXML
    lateinit var root: AnchorPane
    @FXML
    lateinit var imageLists: FlowPane

    private val pixiv = Pixiv()

    fun loginCheck() {
        if (!pixiv.hasRefreshToken()) {
            var username = ""
            var password = ""
            if (File(".client").exists())
                BufferedReader(FileReader(".client")).use {
                    username = it.readLine() ?: ""
                    password = it.readLine() ?: ""
                }
            loginDialog(username, password)
        } else pixiv.login()
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
        val button = Button("Load More (+30)")
        button.styleClass.add("loadMore")
        AnchorPane.setRightAnchor(button, 50.0)
        AnchorPane.setLeftAnchor(button, 50.0)
        val anchorPane = AnchorPane(button)
        anchorPane.prefWidthProperty().bind((imageLists.parent as Region).widthProperty().subtract(35.0))
        imageLists.children.add(anchorPane)
        button.setOnAction {
            imageLists.children.remove(anchorPane)
            val nextBookmarks = pixiv.loadNextPage(beforeData)
            if (nextBookmarks != null) {
                for (illust in nextBookmarks.illusts) addImage(illust.image_urls.small, illust.type)
                loadButton(nextBookmarks)
            }
        }
    }

    private fun addImage(imageUrl: String, illustType: IllustType) {
        val imageView = ImageView()
        imageView.isPreserveRatio = true
        imageView.fitHeight = 150.0
        imageView.fitWidth = 150.0
        imageView.image = Image(pixiv.getImageStream(imageUrl))
        val vBox = VBox(imageView)
        vBox.styleClass.add("imageBox")
        vBox.setPrefSize(150.0, 150.0)
        vBox.alignment = Pos.CENTER
        val pane = Pane(vBox)
        val transition = ScaleTransition(Duration.seconds(0.1), imageView)
        pane.setOnMouseEntered {
            transition.fromX = 1.0
            transition.fromY = 1.0
            transition.toX = 1.1
            transition.toY = 1.1
            transition.play()
        }
        pane.setOnMouseExited {
            transition.fromX = 1.1
            transition.fromY = 1.1
            transition.toX = 1.0
            transition.toY = 1.0
            transition.play()
        }
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
        stage.setOnCloseRequest {
            exitProcess(0)
        }
        stage.showAndWait()
    }
}
