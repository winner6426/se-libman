package com.app.librarymanager.utils;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;

public class AlertDialog {

  public AlertDialog() {
  }

  public static void showAlert(String type, String title, String message,
      EventHandler<ActionEvent> okCallback) {
    final EventHandler<ActionEvent> handler = okCallback != null ? okCallback : event -> {
    };
    // Ensure dialog creation and showing happens on JavaFX Application Thread
    if (Platform.isFxApplicationThread()) {
      Alert alert = getAlert(title, message, type);
      alert.getDialogPane().getStylesheets().add(
          Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
              .toExternalForm());

      alert.getDialogPane().getStyleClass().add("custom-alert");
      alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
      if (alert.getDialogPane().lookup(".header-panel") != null) {
        alert.getDialogPane().lookup(".header-panel").getStyleClass().add("custom-alert-header");
      }
      Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
      okButton.getStyleClass().addAll("btn", "btn-primary");
      okButton.setOnAction(handler);
      alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
      alert.showAndWait();
    } else {
      CountDownLatch latch = new CountDownLatch(1);
      Platform.runLater(() -> {
        try {
          Alert alert = getAlert(title, message, type);
          alert.getDialogPane().getStylesheets().add(
              Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
                  .toExternalForm());
          alert.getDialogPane().getStyleClass().add("custom-alert");
          alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
          if (alert.getDialogPane().lookup(".header-panel") != null) {
            alert.getDialogPane().lookup(".header-panel").getStyleClass().add("custom-alert-header");
          }
          Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
          okButton.getStyleClass().addAll("btn", "btn-primary");
          okButton.setOnAction(handler);
          alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
          alert.showAndWait();
        } finally {
          latch.countDown();
        }
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static Alert getAlert(String title, String message, String type) {
    Alert alert = new Alert(AlertType.INFORMATION);
//    alert.setTitle((!type.isEmpty() ? type.toUpperCase() : "INFORMATION") + " | " + title);
    alert.setTitle(title);
    switch (type.toLowerCase()) {
      case "error":
        alert.setAlertType(AlertType.ERROR);
        break;
      case "warning":
        alert.setAlertType(AlertType.WARNING);
        break;
      case "confirmation":
        alert.setAlertType(AlertType.CONFIRMATION);
        break;
      default:
        alert.setAlertType(AlertType.INFORMATION);
        break;
    }

    alert.setHeaderText(null);
    alert.setContentText(message);
    return alert;
  }

  public static boolean showConfirm(String title, String message) {
    if (Platform.isFxApplicationThread()) {
      Alert alert = getAlert(title, message, "confirmation");
      alert.getDialogPane().getStylesheets().add(
          Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
              .toExternalForm());
      ButtonType yes = new ButtonType("Yes");
      ButtonType no = new ButtonType("No");
      alert.getButtonTypes().setAll(yes, no);

      alert.getDialogPane().getStyleClass().add("custom-alert");
      alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
      if (alert.getDialogPane().lookup(".header-panel") != null) {
        alert.getDialogPane().lookup(".header-panel").getStyleClass().add("custom-alert-header");
      }
      Button yesButton = (Button) alert.getDialogPane().lookupButton(yes);
      yesButton.getStyleClass().addAll("btn", "btn-primary");
      Button noButton = (Button) alert.getDialogPane().lookupButton(no);
      noButton.getStyleClass().addAll("btn", "btn-danger");
      alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
      return alert.showAndWait().filter(yes::equals).isPresent();
    } else {
      final boolean[] result = new boolean[1];
      CountDownLatch latch = new CountDownLatch(1);
      Platform.runLater(() -> {
        try {
          Alert alert = getAlert(title, message, "confirmation");
          alert.getDialogPane().getStylesheets().add(
              Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
                  .toExternalForm());
          ButtonType yes = new ButtonType("Yes");
          ButtonType no = new ButtonType("No");
          alert.getButtonTypes().setAll(yes, no);
          alert.getDialogPane().getStyleClass().add("custom-alert");
          alert.getDialogPane().lookup(".content.label").getStyleClass().add("custom-alert-content");
          if (alert.getDialogPane().lookup(".header-panel") != null) {
            alert.getDialogPane().lookup(".header-panel").getStyleClass().add("custom-alert-header");
          }
          Button yesButton = (Button) alert.getDialogPane().lookupButton(yes);
          yesButton.getStyleClass().addAll("btn", "btn-primary");
          Button noButton = (Button) alert.getDialogPane().lookupButton(no);
          noButton.getStyleClass().addAll("btn", "btn-danger");
          alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
          result[0] = alert.showAndWait().filter(yes::equals).isPresent();
        } finally {
          latch.countDown();
        }
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return result[0];
    }
  }
}
