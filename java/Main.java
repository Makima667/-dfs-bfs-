import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UI.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 680);
        primaryStage.setTitle("PathLab - JavaFX 算法可视化");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
