package jp.kotmw

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.SVGPath
import javafx.stage.Modality
import javafx.stage.Stage
import jp.kotmw.pixiv.Pixiv
import jp.kotmw.pixiv.json.illust.Illust
import jp.kotmw.pixiv.json.illust.IllustType
import java.util.concurrent.Executors
import kotlin.system.exitProcess

class Controller {

    @FXML
    lateinit var root: AnchorPane
    @FXML
    lateinit var imageLists: FlowPane
    @FXML
    lateinit var loading: VBox
    @FXML
    lateinit var loadingDescription: Label

    private val pixiv = Pixiv()
    private val executor = Executors.newSingleThreadExecutor()

    fun loginCheck() {
        if (!pixiv.hasRefreshToken()) loginDialog()
        else asyncLogin()
    }

    fun shutdown() {
        executor.shutdown()
    }

    fun onBookmarks(actionEvent: ActionEvent) {
        imageLists.children.clear()
        val bookmarkData = pixiv.userBookmarks(restrict = "private")

        bookmarkData.forEach { addImage(it) }

        setLoadButton()
    }

    fun onRankings(actionEvent: ActionEvent) {
        imageLists.children.clear()
        val rankingData = pixiv.rankings()

        for (illust in rankingData) {
            addImage(illust)
        }
    }
//          val type = imageUrl.split(".").last()
//
//          ImageIO.write(ImageIO.read(ByteArrayInputStream(stream)), type, File("C:\\Image\\${imageUrl.split("/").last()}"))

    private fun setLoadButton() {
        if (!pixiv.hasNextList()) return
        val button = Button("Load More (+30)")
        button.styleClass.add("loadMore")
        AnchorPane.setRightAnchor(button, 50.0)
        AnchorPane.setLeftAnchor(button, 50.0)
        val anchorPane = AnchorPane(button)
        anchorPane.prefWidthProperty().bind((imageLists.parent as Region).widthProperty().subtract(35.0))
        imageLists.children.add(anchorPane)
        button.setOnAction {
            imageLists.children.remove(anchorPane)
            for (illust in pixiv.loadNextList()) addImage(illust)
            setLoadButton()
        }
    }

    private fun addImage(illust: Illust) {
        val imageView = ImageView()
        imageView.isPreserveRatio = true
        imageView.fitHeight = 150.0
        imageView.fitWidth = 150.0
        imageView.image = Image(pixiv.getImageStream(illust.image_urls.small))
        val vBox = VBox(imageView)
        vBox.styleClass.add("imageBox")
        vBox.setPrefSize(150.0, 150.0)
        vBox.alignment = Pos.CENTER
        val pane = Pane(vBox)
        if (illust.type == IllustType.Ugoira) {
            pane.children.add(ugoiraSign())
        }
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

    private fun loginDialog(errorDescription: String = "") {
        val stage = Stage()
        stage.isResizable = false
        stage.sizeToScene()
        val error = Label(errorDescription)
        error.setPrefSize(240.0, 40.0)
        error.textFill = Color.RED
        error.isWrapText = true
        val textField = TextField()
        textField.promptText = "ID or mail address"
        textField.setPrefSize(240.0, 30.0)
        val passwordField = PasswordField()
        passwordField.promptText = "password"
        passwordField.setPrefSize(240.0, 30.0)
        val login = Button("Login")
        login.setPrefSize(240.0, 40.0)
        login.setOnAction {
            stage.close()
            asyncLogin(textField.text, passwordField.text)
        }
        val vBox = VBox(error, textField, passwordField, login)
        vBox.setPrefSize(300.0, 230.0)
        vBox.padding = Insets(10.0, 30.0, 10.0, 30.0)
        vBox.spacing = 10.0
        stage.scene = Scene(vBox)
        stage.title = "Pixiv Login"
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.setOnCloseRequest { exitProcess(0) }
        stage.showAndWait()
    }

    private fun asyncLogin(userName: String = "", password: String = "") {
        val task = object : Task<Unit>() {
            override fun call() {
                try {
                pixiv.login(userName, password)
                } catch (e: Exception) {
                    Platform.runLater { loginDialog(e.message ?: "認証に失敗しました。") }
                }
            }
        }
        loadingDescription.text = "Login Now..."
        task.setOnRunning { loading.isVisible = true }
        task.setOnSucceeded { loading.isVisible = false }

        executor.submit(task)
    }
}
