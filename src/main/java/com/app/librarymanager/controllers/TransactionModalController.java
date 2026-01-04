package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Transaction;
import com.app.librarymanager.utils.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Setter;
import org.bson.Document;

public class TransactionModalController extends ControllerWithLoader {

  @FunctionalInterface
  public interface SaveCallback {
    void onSave(Transaction transaction);
  }

  @FXML
  private TextField idField;

  @FXML
  private ComboBox<Transaction.Type> typeField;

  @FXML
  private DatePicker dateField;

  @FXML
  private TextField amountField;

  @FXML
  private TextField currencyCodeField;

  @FXML
  private TextArea noteField;

  private Transaction transaction;
  
  @Setter
  private SaveCallback saveCallback;

  private boolean isEditMode = false;

  @FXML
  private void initialize() {
    // Populate type dropdown
    typeField.getItems().addAll(Transaction.Type.values());
    
    // Set default currency
    currencyCodeField.setText("VND");
    
    // Number validation for amount field
    amountField.textProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue.matches("-?\\d*(\\.\\d*)?")) {
        amountField.setText(oldValue);
      }
    });
  }

  public void setTransaction(Transaction transaction) {
    this.transaction = transaction;
    if (transaction != null) {
      isEditMode = true;
      idField.setText(transaction.getId());
      idField.setDisable(true);
      typeField.setValue(transaction.getType());
      
      // Convert Date to LocalDate for DatePicker
      if (transaction.getDate() != null) {
        dateField.setValue(new java.sql.Date(transaction.getDate().getTime()).toLocalDate());
      }
      
      amountField.setText(String.valueOf(transaction.getAmount()));
      currencyCodeField.setText(transaction.getCurrencyCode());
      noteField.setText(transaction.getNote());
    } else {
      isEditMode = false;
      // Set current date for new transaction
      dateField.setValue(java.time.LocalDate.now());
    }
  }

  @FXML
  public void onSubmit() {
    try {
      // Validate inputs
      if (typeField.getValue() == null) {
        throw new Exception("Please select a transaction type.");
      }
      
      if (dateField.getValue() == null) {
        throw new Exception("Please select a date.");
      }
      
      if (amountField.getText().trim().isEmpty()) {
        throw new Exception("Please enter an amount.");
      }
      
      if (currencyCodeField.getText().trim().isEmpty()) {
        throw new Exception("Please enter a currency code.");
      }

      double amount = Double.parseDouble(amountField.getText().trim());

      if (transaction == null) {
        transaction = new Transaction();
      }

      transaction.setType(typeField.getValue());
      
      // Convert LocalDate to Date
      java.time.LocalDate localDate = dateField.getValue();
      Date date = java.sql.Date.valueOf(localDate);
      transaction.setDate(date);
      
      transaction.setAmount(amount);
      transaction.setCurrencyCode(currencyCodeField.getText().trim());
      transaction.setNote(noteField.getText().trim());

      // If new transaction, generate ID
      if (!isEditMode) {
        transaction.setId("TXN-" + System.currentTimeMillis());
      }

      Task<Document> task = new Task<Document>() {
        @Override
        protected Document call() throws Exception {
          return isEditMode ? 
              TransactionController.updateTransaction(transaction) : 
              TransactionController.createTransaction(transaction);
        }
      };

      setLoadingText(isEditMode ? "Updating transaction..." : "Creating transaction...");
      task.setOnRunning(e -> showLoading(true));
      task.setOnSucceeded(e -> {
        showLoading(false);
        Document resp = task.getValue();
        Stage stage = (Stage) idField.getScene().getWindow();
        
        if (resp == null) {
          AlertDialog.showAlert("error", "Error",
              "Failed to save transaction. Please try again.", null);
        } else {
          // For edit mode, _id already exists in transaction object
          // For create mode, get _id from response
          if (!isEditMode && resp.getObjectId("_id") != null) {
            transaction.set_id(resp.getObjectId("_id"));
          }
          
          AlertDialog.showAlert("success", "Success",
              isEditMode ? "Transaction updated successfully." : "Transaction created successfully.", 
              null);
          stage.close();
          if (saveCallback != null) {
            saveCallback.onSave(transaction);
          }
        }
      });
      task.setOnFailed(e -> {
        showLoading(false);
        AlertDialog.showAlert("error", "Error", 
            e.getSource().getException().getMessage(), null);
      });

      new Thread(task).start();
      
    } catch (Exception e) {
      AlertDialog.showAlert("error", "Error", e.getMessage(), null);
    }
  }

  @FXML
  private void onCancel() {
    Stage stage = (Stage) idField.getScene().getWindow();
    stage.close();
  }
}
