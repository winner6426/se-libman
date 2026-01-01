package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.BookLoanUser;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.AlertDialog;
import java.util.Date;
import java.util.List;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class ManageLoanRecordsController extends ControllerWithLoader {

  @FXML
  private Pagination pagination;
  @FXML
  private TableView<BookLoanUser> bookLoansTable;
  @FXML
  private TableColumn<BookLoanUser, String> _idColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userIdColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userDisplayNameColumn;
  @FXML
  private TableColumn<BookLoanUser, String> userEmailColumn;
  @FXML
  private TableColumn<BookLoanUser, String> bookIdColumn;
  @FXML
  private TableColumn<BookLoanUser, String> bookTitleColumn;
  @FXML
  private TableColumn<BookLoanUser, String> borrowDateColumn;
  @FXML
  private TableColumn<BookLoanUser, String> statusColumn;
  @FXML
  private TableColumn<BookLoanUser, String> dueDateColumn;
  @FXML
  private TableColumn<BookLoanUser, String> typeColumn;
  @FXML
  private TableColumn<BookLoanUser, String> numOfCopiesColumn;
  @FXML
  private TableColumn<BookLoanUser, String> returnConditionColumn;
  @FXML
  private TableColumn<BookLoanUser, String> returnedByColumn;
  @FXML
  private TableColumn<BookLoanUser, String> returnedAtColumn;
  @FXML
  private TableColumn<BookLoanUser, Boolean> validColumn;

  private int currentPage = 0;
  @FXML
  private ComboBox<String> pageSizeInput;
  private int pageSize = 10;
  private int totalRecords = 0;

  @FXML
  private TextField searchField;

  private ObservableList<BookLoanUser> bookLoansList = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    showCancel(false);

    _idColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBookLoan().get_id().toString()));
    userIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getUser() != null ? cellData.getValue().getUser().getUid() : "[REMOVED USER]"));
    userDisplayNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getUser() != null ? cellData.getValue().getUser().getDisplayName() : "[REMOVED USER]"));
    userEmailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getUser() != null ? cellData.getValue().getUser().getEmail() : "[REMOVED USER]"));
    bookIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBook() != null ? cellData.getValue().getBook().getId() : "[REMOVED BOOK]"));
    bookTitleColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBook() != null ? cellData.getValue().getBook().getTitle() : "[REMOVED BOOK]"));
    borrowDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        DateUtil.dateToString(cellData.getValue().getBookLoan().getBorrowDate())));
    statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        BookLoanController.computeDisplayStatus(cellData.getValue().getBookLoan())));
    dueDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        DateUtil.dateToString(cellData.getValue().getBookLoan().getDueDate())));
    validColumn.setCellValueFactory(cellData -> new SimpleBooleanProperty(
        cellData.getValue().getBookLoan().isValid()));
    typeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBookLoan().getType().toString()));
    numOfCopiesColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        String.valueOf(cellData.getValue().getBookLoan().getNumCopies())));
    returnConditionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBookLoan().getReturnCondition() == null ? "" : cellData.getValue().getBookLoan().getReturnCondition()));
    returnedByColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        cellData.getValue().getBookLoan().getReturnedBy() == null ? "" : cellData.getValue().getBookLoan().getReturnedBy()));
    returnedAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
        DateUtil.dateToString(cellData.getValue().getBookLoan().getReturnedAt())));

    loadBookLoans();

    pagination.setPageFactory(index -> {
      currentPage = index;
      loadBookLoans();
      return new Label();
    });

    pageSizeInput.getItems().addAll("10", "20", "50", "100");
    pageSizeInput.setValue("10");
    pageSizeInput.setOnAction(e -> {
      pageSize = Integer.parseInt(pageSizeInput.getValue());
      loadBookLoans();
    });

    setRowContextMenu();
  }

  private void loadBookLoans() {
    Task<List<BookLoanUser>> task = new Task<>() {
      @Override
      protected List<BookLoanUser> call() {
        totalRecords = (int) BookLoanController.numberOfRecords();
        return BookLoanController.getAllLentBook(currentPage * pageSize, pageSize);
      }
    };

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      bookLoansList = FXCollections.observableArrayList(task.getValue());
      bookLoansTable.setItems(bookLoansList);

      pagination.setPageCount((int) Math.ceil((double) totalRecords / pageSize));
      pagination.setCurrentPageIndex(currentPage);
    });
    task.setOnFailed(e -> showLoading(false));

    new Thread(task).start();
  }

  @FXML
  private void onSearch() {
    String searchText = searchField.getText().toLowerCase();
    ObservableList<BookLoanUser> filteredList = FXCollections.observableArrayList();
    for (BookLoanUser bookLoan : bookLoansList) {
      if ((bookLoan.getUser() != null && (bookLoan.getUser().getDisplayName().toLowerCase().contains(searchText)
          || bookLoan.getUser().getEmail().toLowerCase().contains(searchText)
          || bookLoan.getUser().getUid().toLowerCase().contains(searchText)))
          || (bookLoan.getBook() != null && (bookLoan.getBook().getTitle().toLowerCase().contains(searchText)))) {
        filteredList.add(bookLoan);
      }
    }
    bookLoansTable.setItems(filteredList);
  }

  private void setRowContextMenu() {
    bookLoansTable.setRowFactory(tableView -> {
      final TableRow<BookLoanUser> row = new TableRow<>();
      final ContextMenu contextMenu = new ContextMenu();
      final MenuItem editMenuItem = new MenuItem("Edit Full Loan Record");

      editMenuItem.setOnAction(e -> {
        // Use the row's item instead of table selection to ensure we have the correct item
        BookLoanUser bookLoan = row.getItem();
        if (bookLoan == null) {
          AlertDialog.showAlert("info", "No selection", "No loan record selected.", null);
          return;
        }
        // Ensure the row is selected for visual feedback
        bookLoansTable.getSelectionModel().select(bookLoan);
        openLoanRecordModal(bookLoan);
      });

      contextMenu.getItems().addAll(editMenuItem);
      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
              .otherwise(contextMenu));
      return row;
    });

  }

  private void updateBookInTable(BookLoanUser updatedBookLoanUser) {
    for (int i = 0; i < bookLoansList.size(); i++) {
      if (bookLoansList.get(i).getBookLoan().get_id()
          .equals(updatedBookLoanUser.getBookLoan().get_id())) {
        bookLoansList.set(i, updatedBookLoanUser);
        bookLoansTable.setItems(bookLoansList);
        bookLoansTable.refresh();
        break;
      }
    }
  }

  private void openLoanRecordModal(BookLoanUser bookLoan) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/loan-record-modal.fxml"));
      Parent parent = loader.load();
      LoanRecordModalController controller = loader.getController();
      controller.setBookLoanUser(bookLoan);
      controller.setSaveCallback(updatedBookLoanUser -> updateBookInTable(updatedBookLoanUser));

      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle("Edit Loan Record");
      dialog.initOwner(bookLoansTable.getScene().getWindow());
      dialog.getDialogPane().setContent(parent);

      String okButtonText = "Save & Update";

      ButtonType okButtonType = new ButtonType(okButtonText, ButtonBar.ButtonData.OK_DONE);
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
    }
  }

}
