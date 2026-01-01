package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.BookLoanUser;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import java.time.LocalDate;
import java.util.Date;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.bson.Document;

public class LoanRecordModalController extends ControllerWithLoader {

  @FunctionalInterface
  public interface SaveCallback {
    void onSave(BookLoanController.BookLoanUser bookLoanUser);
  }

  @FXML
  private DatePicker borrowDateField;
  @FXML
  private DatePicker dueDateField;
  @FXML
  private ComboBox<String> typeField;
  @FXML
  private TextField copiesField;
  @FXML
  private CheckBox validField;

  @FXML
  private ComboBox<String> statusField;
  @FXML
  private DatePicker requestDateField;
  @FXML
  private TextField processedByField;
  @FXML
  private DatePicker processedAtField;

  @FXML
  private CheckBox returnRequestedField;
  @FXML
  private DatePicker returnRequestedAtField;
  @FXML
  private TextField returnedByField;
  @FXML
  private DatePicker returnedAtField;

  @FXML
  private ComboBox<String> returnConditionField;
  @FXML
  private TextArea returnNotesField;
  @FXML
  private TextArea conditionNotesField;
  @FXML
  private ComboBox<String> borrowConditionField;
  @FXML
  private TextArea borrowConditionNotesField;

  private SaveCallback saveCallback;
  private BookLoanController.BookLoanUser bookLoanUser;

  public void initialize() {
    typeField.getItems().addAll("ONLINE", "OFFLINE");
    statusField.getItems().addAll("PENDING", "AVAILABLE", "REJECTED", "EXPIRED", "RETURNED");
    returnConditionField.getItems().addAll("NORMAL", "LATE", "DAMAGED", "LOST");
    showCancel(false);
    DatePickerUtil.setDatePickerFormat(borrowDateField);
    DatePickerUtil.setDatePickerFormat(dueDateField);
    DatePickerUtil.setDatePickerFormat(requestDateField);
    DatePickerUtil.setDatePickerFormat(processedAtField);
    DatePickerUtil.setDatePickerFormat(returnRequestedAtField);
    DatePickerUtil.setDatePickerFormat(returnedAtField);
    DatePickerUtil.disableEditor(borrowDateField);
    DatePickerUtil.disableEditor(dueDateField);
  }

  public void setBookLoanUser(BookLoanController.BookLoanUser bookLoanUser) {
    this.bookLoanUser = bookLoanUser;
    if (bookLoanUser == null || bookLoanUser.getBookLoan() == null) {
      // defensive: nothing to populate
      System.err.println("LoanRecordModalController.setBookLoanUser: provided bookLoanUser or its BookLoan is null");
      return;
    }
    BookLoan loan = bookLoanUser.getBookLoan();
    borrowDateField.setValue(DateUtil.dateToLocalDate(loan.getBorrowDate()));
    dueDateField.setValue(DateUtil.dateToLocalDate(loan.getDueDate()));
    typeField.setValue(loan.getType() != null ? loan.getType().toString() : null);
    copiesField.setText(String.valueOf(loan.getNumCopies()));
    validField.setSelected(loan.isValid());

    statusField.setValue(loan.getStatus() != null ? loan.getStatus().toString() : "PENDING");
    requestDateField.setValue(DateUtil.dateToLocalDate(loan.getRequestDate()));
    processedByField.setText(loan.getProcessedBy() != null ? loan.getProcessedBy() : "");
    processedAtField.setValue(DateUtil.dateToLocalDate(loan.getProcessedAt()));

    returnRequestedField.setSelected(loan.isReturnRequested());
    returnRequestedAtField.setValue(DateUtil.dateToLocalDate(loan.getReturnRequestedAt()));
    returnedByField.setText(loan.getReturnedBy() != null ? loan.getReturnedBy() : "");
    returnedAtField.setValue(DateUtil.dateToLocalDate(loan.getReturnedAt()));

    returnConditionField.setValue(loan.getReturnCondition());
    returnNotesField.setText(loan.getReturnConditionNotes() != null ? loan.getReturnConditionNotes() : "");
    conditionNotesField.setText(loan.getConditionNotes() != null ? loan.getConditionNotes() : "");
    borrowConditionField.setValue(loan.getBorrowCondition() != null ? loan.getBorrowCondition() : "NORMAL");
    borrowConditionNotesField.setText(loan.getBorrowConditionNotes() != null ? loan.getBorrowConditionNotes() : "");
  }

  public void setSaveCallback(SaveCallback saveCallback) {
    this.saveCallback = saveCallback;
  }

  public void onCancel() {
    Stage stage = (Stage) borrowDateField.getScene().getWindow();
    stage.close();
  }

  public void onSubmit() {
    try {
      if (Integer.parseInt(copiesField.getText()) <= 0) {
        AlertDialog.showAlert("error", "Error", "Number of copies must be greater than 0", null);
        return;
      }
    } catch (Exception e) {
      AlertDialog.showAlert("error", "Error", "Invalid number of copies", null);
      return;
    }

    setLoadingText("Saving...");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        showLoading(true);
        BookLoan updatedBookLoan = bookLoanUser.getBookLoan();
        updatedBookLoan.setBorrowDate(DateUtil.localDateToDate(borrowDateField.getValue()));
        updatedBookLoan.setDueDate(DateUtil.localDateToDate(dueDateField.getValue()));
        updatedBookLoan.setType(BookLoan.Mode.valueOf(typeField.getValue()));
        updatedBookLoan.setNumCopies(Integer.parseInt(copiesField.getText()));
        updatedBookLoan.setValid(validField.isSelected());

        // additional fields
        try {
          updatedBookLoan.setStatus(BookLoan.Status.valueOf(statusField.getValue()));
        } catch (Exception ex) {
          // ignore
        }
        updatedBookLoan.setRequestDate(DateUtil.localDateToDate(requestDateField.getValue()));
        // processed fields
        updatedBookLoan.setProcessedBy(processedByField.getText());
        updatedBookLoan.setProcessedAt(DateUtil.localDateToDate(processedAtField.getValue()));

        updatedBookLoan.setReturnRequested(returnRequestedField.isSelected());
        updatedBookLoan.setReturnRequestedAt(DateUtil.localDateToDate(returnRequestedAtField.getValue()));
        updatedBookLoan.setReturnedBy(returnedByField.getText());
        updatedBookLoan.setReturnedAt(DateUtil.localDateToDate(returnedAtField.getValue()));
        updatedBookLoan.setReturnCondition(returnConditionField.getValue());
        updatedBookLoan.setReturnConditionNotes(returnNotesField.getText());
        updatedBookLoan.setConditionNotes(conditionNotesField.getText());
        updatedBookLoan.setBorrowCondition(borrowConditionField.getValue());
        updatedBookLoan.setBorrowConditionNotes(borrowConditionNotesField.getText());

        return BookLoanController.editLoan(updatedBookLoan);
      }
    };

    task.setOnSucceeded(event -> {
      showLoading(false);
      if (task.getValue() == null) {
        AlertDialog.showAlert("error", "Error", "Failed to update loan", null);
        return;
      } else {
        AlertDialog.showAlert("success", "Success", "Loan updated successfully", null);
        Stage stage = (Stage) borrowDateField.getScene().getWindow();
        // refresh the local object with values
        BookLoan updated = new BookLoan(task.getValue());
        // Update the bookLoan property safely (BookLoanUser exposes an ObjectProperty)
        bookLoanUser.bookLoanProperty().set(updated);
        stage.close();
        if (saveCallback != null) saveCallback.onSave(bookLoanUser);
      }
    });

    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to update loan", null);
    });

    new Thread(task).start();
  }
}
