package jp.kotmw

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
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

    private val pixiv = Pixiv()
    private val executor = Executors.newFixedThreadPool(10)

    fun loginCheck() {
        if (!pixiv.hasRefreshToken()) loginDialog()
        else asyncLogin()
    }

    fun shutdown() {
        executor.shutdown()
    }

    fun onBookmarks(actionEvent: ActionEvent) {
        imageLists.children.clear()
        pixiv.userBookmarks(restrict = "private").forEach { addImage(it) }
        setLoadButton()
    }

    fun onRankings(actionEvent: ActionEvent) {
        imageLists.children.clear()
        val rankingData = pixiv.rankings()

        for (illust in rankingData) {
            addImage(illust)
        }

        setLoadButton()
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
        anchorPane.prefWidthProperty().bind((imageLists.parent as Region).widthProperty().subtract(14.0))
        imageLists.children.add(anchorPane)
        button.setOnAction {
            imageLists.children.remove(anchorPane)
            pixiv.loadNextList().forEach { addImage(it) }
            setLoadButton()
        }
    }

    private fun addImage(illust: Illust) {
        val size = 240.0
        val imageView = ImageView()
        imageView.isPreserveRatio = true
        imageView.fitHeight = size
        imageView.fitWidth = size
        imageView.isVisible = false
        val progress = ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS)
        StackPane.setMargin(progress, Insets(70.0))
        val pane = StackPane(imageView, progress)
        pane.setPrefSize(size, size)
        pane.styleClass.add("imageBox")

        if (illust.page_count > 1)
            pane.children.add(Label(illust.page_count.toString()))
        if (illust.type == IllustType.Ugoira)
            pane.children.add(ugoiraSign())
        imageLists.children.add(pane)

        val task = object : Task<Unit>() {
            override fun call() {
                imageView.image = Image(pixiv.getImageStream(illust.image_urls.medium))
            }
        }
        task.setOnSucceeded {
            pane.children.remove(progress)
            imageView.isVisible = true
        }
        executor.submit(task)
    }

    //150 : 55
    //240 : 100
    private fun ugoiraSign(): StackPane {
        val play = SVGPath()
        play.content = "M16 16v17.108a2 2 0 002.992 1.736l14.97-8.554a2 2 0 000-3.473l-14.97-8.553A2 2 0 0016 16z"
        play.fill = Color.WHITE
        play.translateX = 1.0
        return StackPane(Circle(20.0, 20.0, 20.0, Color.web("#00000064")), play)
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
            asyncLogin(textField.text, passwordField.text)
            stage.close()
        }
        val vBox = VBox(10.0, error, textField, passwordField, login)
        vBox.setPrefSize(300.0, 230.0)
        vBox.padding = Insets(10.0, 30.0, 10.0, 30.0)
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
        task.setOnRunning { loading.isVisible = true }
        task.setOnSucceeded { loading.isVisible = false }

        executor.submit(task)
    }
}
