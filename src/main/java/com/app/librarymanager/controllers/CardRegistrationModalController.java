package com.app.librarymanager.controllers;


import javafx.fxml.FXML;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.text.Text;
// Removed Lombok @Setter to avoid duplicate setter generation

public class CardRegistrationModalController extends ControllerWithLoader {

  @FunctionalInterface
  public interface ConfirmCallback {
    void onConfirm(int months);
  }

  @FXML
  private Spinner<Integer> monthsSpinner;
  @FXML
  private Text priceText;

  private ConfirmCallback confirmCallback;

  public void setConfirmCallback(ConfirmCallback confirmCallback) { this.confirmCallback = confirmCallback; }

  private static final int PRICE_PER_MONTH = 50000; // 50k per month

  @FXML
  private void initialize() {
    showCancel(false);
    
    // Set up spinner with values from 1 to 12 months
    SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory = 
        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 12, 1);
    monthsSpinner.setValueFactory(valueFactory);
    
    // Update price when months change
    monthsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
      updatePrice(newValue);
    });
    
    // Initialize price display
    updatePrice(1);
  }

  private void updatePrice(int months) {
    int totalPrice = months * PRICE_PER_MONTH;
    priceText.setText(formatPrice(totalPrice));
  }

  private String formatPrice(int price) {
    return String.format("%,d", price);
  }

  @FXML
  public void onSubmit() {
    if (confirmCallback != null) {
      int months = monthsSpinner.getValue();
      confirmCallback.onConfirm(months);
    }
  }

  @FXML
  private void onCancel() {
    javafx.stage.Stage stage = (javafx.stage.Stage) priceText.getScene().getWindow();
    stage.close();
  }

  public int getSelectedMonths() {
    return monthsSpinner.getValue();
  }
}



