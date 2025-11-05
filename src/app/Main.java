package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import app.db.DBManager;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // ✅ Initialize DB connection globally
        DBManager.getInstance("localhost", 3306, "stockdb", "root", "1234");

        // ✅ Load the login screen
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setScene(scene);
        stage.setTitle("StockFX Login");
        stage.show();

        // ✅ Close DB when app exits
        //stage.setOnCloseRequest(e -> DBManager.getInstance().close());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
