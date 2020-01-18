package jp.kotmw

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class Main : Application() {
    override fun start(primaryStage: Stage?) {
        val fxmlLoader = FXMLLoader(ClassLoader.getSystemResource("Main.fxml"))
        primaryStage?.scene = Scene(fxmlLoader.load())
        val controller = (fxmlLoader.getController() as Controller)
        primaryStage?.setOnShown { controller.loginCheck() }
        primaryStage?.setOnCloseRequest { controller.shutdown() }
        primaryStage?.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}
