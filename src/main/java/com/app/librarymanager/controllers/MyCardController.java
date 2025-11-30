package com.app.librarymanager.controllers;

import com.app.librarymanager.models.LibraryCard;
import com.app.librarymanager.models.LibraryCard.Status;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.time.LocalDate;
import java.time.ZoneId;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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

    setLoadingText("Sending card request...");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        return LibraryCardController.requestCard(currentUser.getUid(),
            currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()
                ? currentUser.getDisplayName()
                : currentUser.getEmail());
      }
    };

//    task.setOnRunning(event -> showLoading(true));
//    task.setOnSucceeded(event -> {
//      showLoading(false);
//      if (task.getValue() == null) {
//        AlertDialog.showAlert("error", "Error", "Đăng ký thẻ thất bại.", null);
//        return;
//      }
//      AlertDialog.showAlert("success", "Thành công",
//          "Yêu cầu đăng ký thẻ đã được gửi. Vui lòng chờ admin phê duyệt.", null);
//      LibraryCard card = new LibraryCard(task.getValue());
//      updateUIWithCard(card);
//    });
//    task.setOnFailed(event -> {
//      showLoading(false);
//      AlertDialog.showAlert("error", "Error", "Đăng ký thẻ thất bại.", null);
//    });
      task.setOnRunning(event -> showLoading(true));
      task.setOnSucceeded(event -> {
          showLoading(false);
          if (task.getValue() == null) {
              AlertDialog.showAlert("error", "Error", "Card registration failed.", null);
              return;
          }
          AlertDialog.showAlert("success", "Success",
                  "Your card registration request has been submitted. Please wait for admin approval.", null);
          LibraryCard card = new LibraryCard(task.getValue());
          updateUIWithCard(card);
      });
      task.setOnFailed(event -> {
          showLoading(false);
          AlertDialog.showAlert("error", "Error", "Card registration failed.", null);
      });


      new Thread(task).start();
  }
}



