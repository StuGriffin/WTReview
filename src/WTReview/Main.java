package WTReview;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("View.fxml"));
        primaryStage.setTitle("JavaFX Welcome");

        Scene scene = new Scene(root);

        primaryStage.setTitle("TT - WTReview");
        primaryStage.setScene(scene);

        primaryStage.show();
    }
}
