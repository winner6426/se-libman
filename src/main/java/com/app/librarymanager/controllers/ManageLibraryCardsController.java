package com.app.librarymanager.controllers;

import com.app.librarymanager.models.LibraryCard;
import com.app.librarymanager.models.LibraryCard.Status;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class ManageLibraryCardsController extends ControllerWithLoader {

  @FXML
  private TableView<LibraryCard> cardsTable;
  @FXML
  private TableColumn<LibraryCard, String> userIdColumn;
  @FXML
  private TableColumn<LibraryCard, String> userNameColumn;
  @FXML
  private TableColumn<LibraryCard, String> registerDateColumn;
  @FXML
  private TableColumn<LibraryCard, String> expireDateColumn;
  @FXML
  private TableColumn<LibraryCard, String> statusColumn;
  @FXML
  private TextField searchField;

  private ObservableList<LibraryCard> cards = FXCollections.observableArrayList();

  @FXML
  private void initialize() {
    showCancel(false);
    setLoadingText("Loading library cards...");

    userIdColumn.setCellValueFactory(
        cellData -> new SimpleStringProperty(cellData.getValue().getUserId()));
    userNameColumn.setCellValueFactory(
        cellData -> new SimpleStringProperty(cellData.getValue().getUserName()));
    registerDateColumn.setCellValueFactory(
        cellData -> new SimpleStringProperty(
            cellData.getValue().getRegisterDate() != null ? DateUtil.dateToString(
                cellData.getValue().getRegisterDate()) : "-"));
    expireDateColumn.setCellValueFactory(
        cellData -> new SimpleStringProperty(
            cellData.getValue().getExpireDate() != null ? DateUtil.dateToString(
                cellData.getValue().getExpireDate()) : "-"));
    statusColumn.setCellValueFactory(
        cellData -> new SimpleStringProperty(
            cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name()
                : "PENDING"));

    setRowContextMenu();
    loadCards();

    searchField.textProperty().addListener((observable, oldValue, newValue) -> filterCards());
  }

  private void loadCards() {
    Task<List<LibraryCard>> task = new Task<>() {
      @Override
      protected List<LibraryCard> call() {
        return LibraryCardController.getAllCards();
      }
    };

    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      List<LibraryCard> result = task.getValue();
      cards.setAll(result != null ? result : List.of());
      cardsTable.setItems(cards);
      resizeColumnsToFitContent();
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to load library cards", null);
    });

    new Thread(task).start();
  }

  @FXML
  private void filterCards() {
    String query = searchField.getText().trim().toLowerCase();
    if (query.isEmpty()) {
      cardsTable.setItems(cards);
      return;
    }
    ObservableList<LibraryCard> filtered = FXCollections.observableArrayList();
    for (LibraryCard card : cards) {
      if ((card.getUserId() != null && card.getUserId().toLowerCase().contains(query))
          || (card.getUserName() != null && card.getUserName().toLowerCase().contains(query))
          || (card.getStatus() != null && card.getStatus().name().toLowerCase()
          .contains(query))) {
        filtered.add(card);
      }
    }
    cardsTable.setItems(filtered);
  }

  private void setRowContextMenu() {
    cardsTable.setRowFactory(tableView -> {
      TableRow<LibraryCard> row = new TableRow<>();
      ContextMenu contextMenu = new ContextMenu();

      MenuItem editItem = new MenuItem("Edit Card");
      editItem.setOnAction(event -> {
        LibraryCard card = row.getItem();
        if (card != null) {
          openCardModal(card);
        }
      });

      MenuItem approveItem = new MenuItem("Approve Card");
      approveItem.setOnAction(event -> {
        LibraryCard card = row.getItem();
        if (card != null) {
          approveCard(card);
        }
      });

      MenuItem rejectItem = new MenuItem("Reject Card");
      rejectItem.setOnAction(event -> {
        LibraryCard card = row.getItem();
        if (card != null) {
          rejectCard(card);
        }
      });

      contextMenu.getItems().addAll(editItem, approveItem, rejectItem);

      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
              .otherwise(contextMenu));
      return row;
    });
  }

  private void approveCard(LibraryCard card) {
    if (!AlertDialog.showConfirm("Approve Card",
        "Confirm user card issuance " + card.getUserName() + "?")) {
      return;
    }

    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        LibraryCardController.approveCardWithMonths(card.get_id());
        return null;
      }
    };

    setLoadingText("Approving card...");
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      AlertDialog.showAlert("success", "Success", "Card approved successfully", null);
      loadCards();
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to approve card", null);
    });

    new Thread(task).start();
  }

  private void rejectCard(LibraryCard card) {
    if (!AlertDialog.showConfirm("Reject Card",
        "Reject user card request " + card.getUserName() + "?")) {
      return;
    }

    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        LibraryCardController.rejectCard(card.get_id());
        return null;
      }
    };

    setLoadingText("Rejecting card...");
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> {
      showLoading(false);
      AlertDialog.showAlert("success", "Success", "Card rejected successfully", null);
      loadCards();
    });
    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to reject card", null);
    });

    new Thread(task).start();
  }

  private void openCardModal(LibraryCard card) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/card-modal.fxml"));
      Parent parent = loader.load();

      CardModalController controller = loader.getController();
      controller.setCard(card);
      controller.setSaveCallback(updatedCard -> {
        updateCardInTable(updatedCard);
      });

      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle("Edit Card");
      dialog.initOwner(cardsTable.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);

      ButtonType okButtonType = new ButtonType("Save & Update", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButtonType = ButtonType.CANCEL;
      dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);
      Button saveButton = (Button) dialog.getDialogPane().lookupButton(okButtonType);
      saveButton.getStyleClass().addAll("btn", "btn-primary");
      saveButton.addEventFilter(ActionEvent.ACTION, event -> {
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
      AlertDialog.showAlert("error", "Error", "Failed to open edit card dialog", null);
    }
  }

  private void updateCardInTable(LibraryCard updatedCard) {
    for (int i = 0; i < cards.size(); i++) {
      LibraryCard card = cards.get(i);
      if (card.get_id() != null && updatedCard.get_id() != null
          && card.get_id().equals(updatedCard.get_id())) {
        cards.set(i, updatedCard);
        break;
      }
    }
    cardsTable.refresh();
    resizeColumnsToFitContent();
  }

  private void resizeColumnsToFitContent() {
    javafx.application.Platform.runLater(() -> {
      if (cards.isEmpty()) {
        userIdColumn.setPrefWidth(120);
        userNameColumn.setPrefWidth(150);
        //cardNumberColumn.setPrefWidth(150);
        registerDateColumn.setPrefWidth(180);
        expireDateColumn.setPrefWidth(180);
        statusColumn.setPrefWidth(100);
        return;
      }

      // User ID Column
      double userIdWidth = calculateColumnWidth("User ID", cards, card -> 
          card.getUserId() != null ? card.getUserId() : "");
      userIdColumn.setPrefWidth(Math.max(userIdWidth, 120));

      // User Name Column - có thể dài hơn
      double userNameWidth = calculateColumnWidth("User Name", cards, card -> 
          card.getUserName() != null ? card.getUserName() : "");
      userNameColumn.setPrefWidth(Math.max(userNameWidth, 150));

      // Register Date Column - format "HH:mm:ss dd/MM/yyyy"
      double registerDateWidth = calculateColumnWidth("Register Date", cards, card -> 
          card.getRegisterDate() != null ? DateUtil.dateToString(card.getRegisterDate()) : "-");
      registerDateColumn.setPrefWidth(Math.max(registerDateWidth, 180));

      // Expire Date Column - format "HH:mm:ss dd/MM/yyyy"
      double expireDateWidth = calculateColumnWidth("Expire Date", cards, card -> 
          card.getExpireDate() != null ? DateUtil.dateToString(card.getExpireDate()) : "-");
      expireDateColumn.setPrefWidth(Math.max(expireDateWidth, 180));

      // Status Column - PENDING, APPROVED, REJECTED (dài nhất là APPROVED = 8 ký tự)
      double statusWidth = calculateColumnWidth("Status", cards, card -> 
          card.getStatus() != null ? card.getStatus().name() : "PENDING");
      statusColumn.setPrefWidth(Math.max(statusWidth, 100));
    });
  }

  private double calculateColumnWidth(String headerText, ObservableList<LibraryCard> items, 
      java.util.function.Function<LibraryCard, String> valueExtractor) {
    double headerWidth = headerText.length() * 8 + 30;

    double maxContentWidth = headerWidth;
    for (LibraryCard card : items) {
      String value = valueExtractor.apply(card);
      if (value != null && !value.isEmpty()) {
        double contentWidth = value.length() * 7 + 30;
        maxContentWidth = Math.max(maxContentWidth, contentWidth);
      }
    }

    return maxContentWidth + 10;
  }
}



