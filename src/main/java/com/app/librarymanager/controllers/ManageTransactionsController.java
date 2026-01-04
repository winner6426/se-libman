package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Transaction;
import com.app.librarymanager.utils.AlertDialog;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

public class ManageTransactionsController extends ControllerWithLoader {

  private static ManageTransactionsController instance;

  @FXML
  private TableView<Transaction> transactionsTable;

  @FXML
  private TableColumn<Transaction, String> idColumn;

  @FXML
  private TableColumn<Transaction, String> typeColumn;

  @FXML
  private TableColumn<Transaction, String> dateColumn;

  @FXML
  private TableColumn<Transaction, String> amountColumn;

  @FXML
  private TableColumn<Transaction, String> currencyCodeColumn;

  @FXML
  private TableColumn<Transaction, String> noteColumn;

  @FXML
  private TextField searchField;

  @FXML
  private Button searchButton;

  @FXML
  private Button prevButton;

  @FXML
  private Button nextButton;

  @FXML
  private Label paginationLabel;

  private List<Transaction> transactionsList;
  private int currentPage = 0;
  private final int length = 20;
  private long totalTransactions = 0;
  private String currentSearchKeyword = "";

  public static ManageTransactionsController getInstance() {
    if (instance == null) {
      instance = new ManageTransactionsController();
    }
    return instance;
  }

  @FXML
  private void initialize() {
    instance = this;

    // Set up table columns
    idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
    typeColumn.setCellValueFactory(cellData -> 
        new SimpleStringProperty(cellData.getValue().getTypeDisplay()));
    
    dateColumn.setCellValueFactory(cellData -> {
      Date date = cellData.getValue().getDate();
      if (date == null) {
        return new SimpleStringProperty("N/A");
      }
      SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
      return new SimpleStringProperty(sdf.format(date));
    });

    amountColumn.setCellValueFactory(cellData -> {
      double amount = cellData.getValue().getAmount();
      DecimalFormat df = new DecimalFormat("#,##0.00");
      String formattedAmount = df.format(Math.abs(amount));
      String sign = amount >= 0 ? "+" : "-";
      return new SimpleStringProperty(sign + formattedAmount);
    });

    currencyCodeColumn.setCellValueFactory(new PropertyValueFactory<>("currencyCode"));
    noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));

    // Set cell factory for amount column to show colors
    amountColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
      @Override
      protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setStyle("");
        } else {
          setText(item);
          Transaction transaction = getTableView().getItems().get(getIndex());
          if (transaction.getAmount() >= 0) {
            setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold;"); // Green for income
          } else {
            setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;"); // Red for expense
          }
        }
      }
    });

    // Set up search
    searchButton.setOnAction(e -> onSearch());
    searchField.setOnAction(e -> onSearch());

    // Set up pagination
    prevButton.setOnAction(e -> prevPage());
    nextButton.setOnAction(e -> nextPage());

    // Set up context menu
    setRowContextMenu();

    // Load initial data
    loadTransactions();
    countTotalTransactions();
  }

  private void loadTransactions() {
    Task<List<Transaction>> task = new Task<List<Transaction>>() {
      @Override
      protected List<Transaction> call() throws Exception {
        if (currentSearchKeyword.isEmpty()) {
          return TransactionController.getAllTransactions(currentPage * length, length);
        } else {
          return TransactionController.searchTransactions(currentSearchKeyword, 
              currentPage * length, length);
        }
      }
    };

    setLoadingText("Loading transactions...");
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      transactionsList = task.getValue();
      Platform.runLater(() -> {
        transactionsTable.getItems().clear();
        transactionsTable.getItems().addAll(transactionsList);
        updatePaginationInfo();
      });
    });
    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", 
          "Failed to load transactions: " + task.getException().getMessage(), null);
    });

    new Thread(task).start();
  }

  private void countTotalTransactions() {
    Task<Long> task = new Task<Long>() {
      @Override
      protected Long call() throws Exception {
        if (currentSearchKeyword.isEmpty()) {
          return TransactionController.countTransactions();
        } else {
          return TransactionController.countTransactionsBySearch(currentSearchKeyword);
        }
      }
    };

    task.setOnSucceeded(e -> {
      totalTransactions = task.getValue();
      updatePaginationInfo();
    });
    task.setOnFailed(e -> {
      System.err.println("Failed to count transactions: " + task.getException().getMessage());
    });

    new Thread(task).start();
  }

  @FXML
  private void onSearch() {
    currentSearchKeyword = searchField.getText().trim();
    currentPage = 0;
    countTotalTransactions();
    loadTransactions();
  }

  private void setRowContextMenu() {
    transactionsTable.setRowFactory(tv -> {
      TableRow<Transaction> row = new TableRow<>();
      row.setOnMouseClicked(event -> {
        if (event.getClickCount() == 2 && !row.isEmpty()) {
          // Double click - open edit dialog
          Transaction transaction = row.getItem();
          if (transaction != null) {
            openTransactionModal(transaction);
          }
        }
      });

      ContextMenu contextMenu = new ContextMenu();

      MenuItem editItem = new MenuItem("Edit Transaction");
      editItem.setOnAction(event -> {
        Transaction transaction = row.getItem();
        if (transaction != null) {
          openTransactionModal(transaction);
        }
      });

      MenuItem deleteItem = new MenuItem("Delete Transaction");
      deleteItem.setOnAction(event -> {
        Transaction transaction = row.getItem();
        if (transaction != null) {
          deleteTransaction(transaction);
        }
      });

      contextMenu.getItems().addAll(editItem, deleteItem);

      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty())
              .then((ContextMenu) null)
              .otherwise(contextMenu)
      );

      return row;
    });
  }

  private void deleteTransaction(Transaction transaction) {
    if (!AlertDialog.showConfirm("Delete Transaction",
        "Are you sure you want to delete this transaction?")) {
      return;
    }

    Task<Boolean> task = new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        return TransactionController.deleteTransaction(transaction.get_id());
      }
    };

    setLoadingText("Deleting transaction...");
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      boolean success = task.getValue();
      if (success) {
        AlertDialog.showAlert("success", "Success", "Transaction deleted successfully", null);
        loadTransactions();
        countTotalTransactions();
      } else {
        AlertDialog.showAlert("error", "Error", "Failed to delete transaction", null);
      }
    });
    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", 
          "Failed to delete transaction: " + task.getException().getMessage(), null);
    });

    new Thread(task).start();
  }

  private void updatePaginationInfo() {
    int totalPages = (int) Math.ceil((double) totalTransactions / length);
    int currentPageDisplay = totalTransactions > 0 ? currentPage + 1 : 0;
    paginationLabel.setText("Page " + currentPageDisplay + " of " + totalPages);
    prevButton.setDisable(currentPage == 0);
    nextButton.setDisable((currentPage + 1) * length >= totalTransactions);
  }

  @FXML
  private void prevPage() {
    if (currentPage > 0) {
      currentPage--;
      loadTransactions();
    }
  }

  @FXML
  private void nextPage() {
    if ((currentPage + 1) * length < totalTransactions) {
      currentPage++;
      loadTransactions();
    }
  }

  private void openTransactionModal(Transaction transaction) {
    try {
      javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
          getClass().getResource("/views/components/transaction-modal.fxml"));
      javafx.scene.Parent parent = loader.load();
      TransactionModalController controller = loader.getController();
      
      controller.setTransaction(transaction);
      controller.setSaveCallback(updatedTransaction -> {
        loadTransactions();
        countTotalTransactions();
      });

      javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
      dialog.setTitle(transaction == null ? "Create Transaction" : "Edit Transaction");
      dialog.initOwner(transactionsTable.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);

      javafx.scene.control.ButtonType okButtonType = new javafx.scene.control.ButtonType(
          transaction != null ? "Save & Update" : "Create", 
          javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
      javafx.scene.control.ButtonType cancelButtonType = javafx.scene.control.ButtonType.CANCEL;
      dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

      javafx.scene.control.Button saveButton = (javafx.scene.control.Button) 
          dialog.getDialogPane().lookupButton(okButtonType);
      saveButton.getStyleClass().addAll("btn", "btn-primary");
      saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
        controller.onSubmit();
        event.consume();
      });

      javafx.scene.control.Button cancelButton = (javafx.scene.control.Button) 
          dialog.getDialogPane().lookupButton(cancelButtonType);
      cancelButton.getStyleClass().addAll("btn", "btn-text");
      cancelButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> dialog.close());

      dialog.setResultConverter(dialogButton -> null);
      dialog.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
      AlertDialog.showAlert("error", "Error", 
          "Failed to open transaction dialog: " + e.getMessage(), null);
    }
  }
}
