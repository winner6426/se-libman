package com.app.librarymanager.controllers;

import com.app.librarymanager.utils.AlertDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

public class ControllerWithLoader {

  @FXML
  protected VBox loadingOverlay;
  @FXML
  protected ProgressIndicator loadingSpinner;
  @FXML
  protected Text loadingText;
  @FXML
  private Button cancelButton;

  protected void setLoadingText(String text) {
    if (loadingText == null) {
      // Optional control - skip if not present in FXML
      System.err.println("ControllerWithLoader: loadingText not present in FXML; skipping setLoadingText.");
      return;
    }
    // Ensure update happens on FX thread
    javafx.application.Platform.runLater(() -> loadingText.setText(text));
  }

  protected void setCancelLoadingAction(Callback<Void, Void> action) {
    if (cancelButton == null) {
      // No cancel button in this layout; nothing to wire.
      System.err.println("ControllerWithLoader: cancelButton not present in FXML; skipping setCancelLoadingAction.");
      return;
    }
    // Ensure we assign handler on FX thread
    javafx.application.Platform.runLater(() -> cancelButton.setOnAction(e -> action.call(null)));
  }

  protected void showCancel(boolean show) {
    if (cancelButton == null) {
      // Optional control - silently ignore if not present in FXML
      return;
    }
    javafx.application.Platform.runLater(() -> {
      cancelButton.setVisible(show);
      cancelButton.setManaged(show);
    });
  }

  protected void showLoading(boolean show) {
    if (loadingOverlay == null || loadingSpinner == null) {
      // Optional loading overlay not present in this scene; nothing to show.
      System.err.println("ControllerWithLoader: loadingOverlay/loadingSpinner not present in FXML; skipping showLoading.");
      return;
    }
    javafx.application.Platform.runLater(() -> {
      loadingOverlay.setVisible(show);
      loadingSpinner.setVisible(show);
    });
  }
}
