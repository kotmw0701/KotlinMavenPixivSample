package jp.kotmw

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class Main : Application() {
    override fun start(primaryStage: Stage?) {
        primaryStage?.scene = Scene(FXMLLoader.load<Parent>(ClassLoader.getSystemResource("Main.fxml")))
        primaryStage?.isResizable = false
        primaryStage?.sizeToScene()
        primaryStage?.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}
