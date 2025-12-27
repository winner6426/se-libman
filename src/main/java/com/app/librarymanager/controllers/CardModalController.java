package com.app.librarymanager.controllers;

import com.app.librarymanager.models.LibraryCard;
import com.app.librarymanager.models.LibraryCard.Status;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import java.time.ZoneId;
import java.util.Date;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Setter;
import org.bson.types.ObjectId;

public class CardModalController extends ControllerWithLoader {

  @FunctionalInterface
  public interface SaveCallback {

    void onSave(LibraryCard card);
  }

  @FXML
  private TextField userIdField;
  @FXML
  private TextField cardIdField;
  @FXML
  private TextField userNameField;
  @FXML
  private DatePicker registerDateField;
  @FXML
  private DatePicker expireDateField;
  @FXML
  private ComboBox<Status> statusField;

  private LibraryCard card;
  @Setter
  private SaveCallback saveCallback;
  public void setSaveCallback(SaveCallback saveCallback) { this.saveCallback = saveCallback; }
  private boolean isEditMode = false;

  @FXML
  private void initialize() {
    showCancel(false);
    DatePickerUtil.setDatePickerFormat(registerDateField);
    DatePickerUtil.setDatePickerFormat(expireDateField);
    DatePickerUtil.disableEditor(registerDateField);
    DatePickerUtil.disableEditor(expireDateField);

    statusField.getItems().addAll(Status.PENDING, Status.APPROVED, Status.REJECTED);
  }

  public void setCard(LibraryCard card) {
    this.card = card;
    if (card != null) {
      isEditMode = true;
      userIdField.setText(card.getUserId());
      userIdField.setDisable(true);
      cardIdField.setText(card.get_id() != null ? card.get_id().toString() : "");
      cardIdField.setDisable(true);
      userNameField.setText(card.getUserName());
      userNameField.setDisable(true);
      if (card.getRegisterDate() != null) {
        registerDateField.setValue(DateUtil.dateToLocalDate(card.getRegisterDate()));
      }
      if (card.getExpireDate() != null) {
        expireDateField.setValue(DateUtil.dateToLocalDate(card.getExpireDate()));
      }
      statusField.setValue(card.getStatus() != null ? card.getStatus() : Status.PENDING);
    } else {
      isEditMode = false;
      userIdField.setDisable(false);
      cardIdField.setDisable(false);
    }
  }

  @FXML
  void onSubmit() {
    if (card == null) {
      AlertDialog.showAlert("error", "Error", "Card is null", null);
      return;
    }

    // Update card fields (userName is not editable, so we don't update it)

    if (registerDateField.getValue() != null) {
      card.setRegisterDate(
          Date.from(registerDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    } else {
      card.setRegisterDate(null);
    }

    if (expireDateField.getValue() != null) {
      card.setExpireDate(
          Date.from(expireDateField.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
    } else {
      card.setExpireDate(null);
    }

    card.setStatus(statusField.getValue() != null ? statusField.getValue() : Status.PENDING);

    Task<Boolean> task = new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        return LibraryCardController.updateCard(card);
      }
    };

    task.setOnRunning(e -> showLoading(true));

    task.setOnSucceeded(e -> {
      showLoading(false);
      Boolean success = task.getValue();
      Stage stage = (Stage) userNameField.getScene().getWindow();
      if (success != null && success) {
        AlertDialog.showAlert("success", "Success", "Card updated successfully", null);
        stage.close();
        if (saveCallback != null) {
          saveCallback.onSave(card);
        }
      } else {
        AlertDialog.showAlert("error", "Error", "Failed to update card", null);
      }
    });

    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "An error occurred while saving the card.", null);
    });

    new Thread(task).start();
  }

  @FXML
  private void onCancel() {
    Stage stage = (Stage) userNameField.getScene().getWindow();
    stage.close();
  }
}

