package app.controllers;

import app.db.DBManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginController {

    @FXML private TextField txtUser;
    @FXML private PasswordField txtPass;
    @FXML private Button btnLogin;
    @FXML private Button btnSignup;

    private DBManager db;

    @FXML
    public void initialize() {
        // ✅ Reuse global DB connection instead of creating a new one
        db = DBManager.getInstance();
        System.out.println("✅ LoginController using shared DB connection");
    }

    @FXML
    private void onLogin(ActionEvent e) {
        try {
            String username = txtUser.getText().trim();
            String password = txtPass.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Please enter both username and password.");
                return;
            }

            Optional<Integer> uid = db.login(username, password);
            if (uid.isPresent()) {
                System.out.println("✅ Login successful. User ID: " + uid.get());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                Parent root = loader.load();

                // Pass DB + user ID to DashboardController
                DashboardController ctrl = loader.getController();
                ctrl.init(db, uid.get());

                // Switch scene
                Stage stage = (Stage) btnLogin.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.setTitle("📈 StockFX Dashboard");
                stage.show();

            } else {
                showAlert("Invalid username or password.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error: " + ex.getMessage());
        }
    }

    @FXML
    private void onSignup(ActionEvent e) {
        try {
            String username = txtUser.getText().trim();
            String password = txtPass.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert("Please enter both username and password.");
                return;
            }

            boolean ok = db.signup(username, password);
            if (ok) {
                showAlert("✅ Signup successful! Please log in now.");
            } else {
                showAlert("⚠️ Signup failed — username may already exist.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Error: " + ex.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("StockFX Message");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
