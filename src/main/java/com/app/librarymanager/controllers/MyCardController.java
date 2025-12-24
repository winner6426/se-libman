package com.app.librarymanager.controllers;

import com.app.librarymanager.models.LibraryCard;
import com.app.librarymanager.models.LibraryCard.Status;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.StageManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.text.Text;
import org.bson.Document;

public class MyCardController extends ControllerWithLoader {

  @FXML
  private Text statusText;
  @FXML
  private Text cardIdText;
  @FXML
  private Text registerDateText;
  @FXML
  private Text expireDateText;
  @FXML
  private Button requestCardButton;

  @FXML
  private void initialize() {
    showCancel(false);
    AuthController.requireLogin();
    if (!AuthController.getInstance().isAuthenticated()) {
      requestCardButton.setDisable(true);
      return;
    }
    loadCurrentCard();
  }

  private void loadCurrentCard() {
    User currentUser = AuthController.getInstance().getCurrentUser();
    if (currentUser == null) {
      return;
    }

    setLoadingText("Loading library card...");
    Task<LibraryCard> task = new Task<>() {
      @Override
      protected LibraryCard call() {
        return LibraryCardController.getCardOfUser(currentUser.getUid());
      }
    };

    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      updateUIWithCard(task.getValue());
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to load library card", null);
    });

    new Thread(task).start();
  }

  private void updateUIWithCard(LibraryCard card) {
    if (card == null) {
      statusText.setText("No card yet");
      cardIdText.setText("-");
      registerDateText.setText("-");
      expireDateText.setText("-");
      requestCardButton.setDisable(false);
      requestCardButton.setText("Card registration");
      return;
    }

    statusText.setText(card.getStatus().name());
    cardIdText.setText(card.get_id() != null ? card.get_id().toString() : "-");
    registerDateText.setText(
        card.getRegisterDate() != null ? DateUtil.dateToString(card.getRegisterDate()) : "-");
    expireDateText.setText(
        card.getExpireDate() != null ? DateUtil.dateToString(card.getExpireDate()) : "-");

    if (Status.PENDING.equals(card.getStatus())) {
      requestCardButton.setDisable(true);
      requestCardButton.setText("Pending approval");
    } else if (Status.APPROVED.equals(card.getStatus())) {
      requestCardButton.setDisable(true);
      requestCardButton.setText("Card issued");
    } else {
      requestCardButton.setDisable(false);
      requestCardButton.setText("Re-register");
    }
  }

  @FXML
  private void handleRequestCard() {
    User currentUser = AuthController.getInstance().getCurrentUser();
    if (currentUser == null) {
      AlertDialog.showAlert("error", "Error", "Please log in to register for a card.", null);
      return;
    }

    openCardRegistrationDialog(currentUser);
  }

  private void openCardRegistrationDialog(User currentUser) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/card-registration-modal.fxml"));
      Parent parent = loader.load();

      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle("Library Card Registration");
      dialog.initOwner(requestCardButton.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);
      dialog.getDialogPane().getStylesheets().add(
          Objects.requireNonNull(StageManager.class.getResource("/styles/global.css")).toExternalForm());

      CardRegistrationModalController controller = loader.getController();
      controller.setConfirmCallback(months -> {
        // Close the dialog
        dialog.close();

        // Process card registration with selected months
        processCardRegistration(currentUser, months);
      });

      ButtonType confirmButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButtonType = ButtonType.CANCEL;
      dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, cancelButtonType);

      Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
      confirmButton.getStyleClass().addAll("btn", "btn-primary");
      confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
        controller.onSubmit();
        event.consume();
      });

      Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
      cancelButton.getStyleClass().addAll("btn", "btn-text");
      cancelButton.addEventFilter(ActionEvent.ACTION, event -> dialog.close());

      dialog.setResultConverter(dialogButton -> null);

      dialog.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to open registration dialog.", null);
    }
  }

  private void processCardRegistration(User currentUser, int months) {
    setLoadingText("Pending approval");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        return LibraryCardController.requestCard(currentUser.getUid(),
            currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()
                ? currentUser.getDisplayName()
                : currentUser.getEmail(),
            months);
      }
    };

    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      if (task.getValue() == null) {
        AlertDialog.showAlert("error", "Error", "Library card registration failed.", null);
        return;
      }
      AlertDialog.showAlert("success", "Success",
          "Registration submitted. Please wait for approval.", null);
      LibraryCard card = new LibraryCard(task.getValue());
      updateUIWithCard(card);
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Library card registration failed.", null);
    });

    new Thread(task).start();
  }
}



