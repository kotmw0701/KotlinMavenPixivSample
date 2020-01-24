package jp.kotmw

import javafx.application.Platform
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.geometry.Insets
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
import java.io.File
import java.util.concurrent.Executors
import javax.imageio.ImageIO
import kotlin.system.exitProcess

class Controller {

    @FXML
    lateinit var root: AnchorPane
    @FXML
    lateinit var imageLists: FlowPane
    @FXML
    lateinit var loading: VBox
    @FXML
    lateinit var progressBar: ProgressBar
    @FXML
    lateinit var fileName: Label

    private val pixiv = Pixiv()
    private val executor = Executors.newFixedThreadPool(10)

    fun loginCheck() = if (!pixiv.hasRefreshToken()) loginDialog() else asyncLogin()

    fun shutdown() = executor.shutdown()

    fun onBookmarks(actionEvent: ActionEvent) {
        imageLists.children.clear()
        pixiv.userBookmarks(restrict = "private").forEach { addImage(it) }
        setLoadButton()
    }

    fun onRankings(actionEvent: ActionEvent) {
        imageLists.children.clear()
        pixiv.rankings().forEach { addImage(it) }
        setLoadButton()
    }

    fun onDownload(actionEvent: ActionEvent) {
        val bookmarkList = pixiv.userBookmarks(restrict = "private").toMutableList()
        while (pixiv.hasNextList()) bookmarkList.addAll(pixiv.loadNextList())

        File("Image").mkdir()

        executor.submit(object : Task<Unit>() {
            override fun call() {
                bookmarkList.forEachIndexed { illustNum, it ->
                    if (it.page_count <= 1) {
                        val url = it.meta_single_page.original_image_url.toString()
                        val type = url.split(".").last()
                        ImageIO.write(ImageIO.read(pixiv.getImageStream(url)), type, File("Image\\${it.id}.$type"))
                        updateMessage(it.title)
                    } else it.meta_pages.forEachIndexed { index, pages ->
                        val url = pages.image_urls.original.toString()
                        val type = url.split(".").last()
                        ImageIO.write(ImageIO.read(pixiv.getImageStream(url)), type, File("Image\\${it.id}_$index.$type"))
                        updateMessage(it.title +"_"+ index)
                    }
                    updateProgress(illustNum.toLong(), bookmarkList.size.toLong())
                }
            }
        }.apply {
            setOnScheduled {
                progressBar.progressProperty().bind(progressProperty())
                fileName.textProperty().bind(messageProperty())
            }
            setOnSucceeded { println("Complete!") }
        })
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

        val imageView = ImageView().apply {
            isPreserveRatio = true
            fitHeight = size
            fitWidth = size
            isVisible = false
        }

        val progress = ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS)
        progress.setMinSize(100.0, 100.0)
        val pane = StackPane(imageView, progress)
        pane.setPrefSize(size, size)
        pane.styleClass.add("imageBox")

        if (illust.page_count > 1) pane.children.add(Label(illust.page_count.toString()))
        if (illust.type == IllustType.Ugoira) pane.children.add(ugoiraSign())
        imageLists.children.add(pane)
        executor.submit(object : Task<Unit>() {
            override fun call() { imageView.image = Image(pixiv.getImageStream(illust.image_urls.medium)) }
        }.apply { setOnSucceeded {
            pane.children.remove(progress)
            imageView.isVisible = true
        } })
    }

    //150 : 55
    //240 : 100
    private fun ugoiraSign(): StackPane = StackPane(
        Circle(20.0,
            20.0,
            20.0,
            Color.web("#00000064")),
        SVGPath().apply {
            content = "M16 16v17.108a2 2 0 002.992 1.736l14.97-8.554a2 2 0 000-3.473l-14.97-8.553A2 2 0 0016 16z"
            fill = Color.WHITE
            translateX = 1.0
        })

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
        val vBox = VBox(10.0, Label(errorDescription), textField, passwordField, login)
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
