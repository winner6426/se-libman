package com.app.librarymanager.controllers;

import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.StageManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

public class ForgotPasswordController {

  @FXML
  private TextField emailField;
  @FXML
  private VBox loadingOverlay;
  @FXML
  private ProgressIndicator loadingSpinner;

  @FXML
  private void handleSendEmail() {
    String email = emailField.getText();
    if (email.isEmpty()) {
      AlertDialog.showAlert("error", "Invalid email", "Please enter a valid email.", null);
      return;
    }
    showLoading(true);
    Task<JSONObject> sendEmailTask = new Task<JSONObject>() {
      @Override
      protected JSONObject call() throws Exception {
        return AuthController.sendPasswordResetEmail(email);
      }
    };
    sendEmailTask.setOnSucceeded(e -> {
      showLoading(false);
      //  System.out.println(sendEmailTask.getValue());
      JSONObject response = sendEmailTask.getValue();
      if (response == null) {
        AlertDialog.showAlert("error", "Error",
            "Failed to send password reset email. Please try again later.", null);
        return;
      }
      if (response.has("error")) {
        JSONObject error = response.getJSONObject("error");
        if (error.has("message")) {
          AuthController.onSendPasswordEmailFailure(error.getString("message"));
        }
      } else {
        AlertDialog.showAlert("info", "Email Sent",
            "Password reset email sent. If the email exists, you will receive a link to reset the password, please check your inbox (or spam folder).",
            event -> {
              StageManager.showLoginWindow();
            });
      }
    });
    sendEmailTask.setOnFailed(e -> {
      showLoading(false);
    });

    new Thread(sendEmailTask).start();
  }

  @FXML
  private void handleOpenLogin() {
    StageManager.showLoginWindow();
  }

  @FXML
  private void handleOpenRegister() {
    StageManager.showRegisterWindow();
  }

  private void showLoading(boolean show) {
    loadingOverlay.setVisible(show);
    loadingSpinner.setVisible(show);
  }
}
