package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.User;
import com.app.librarymanager.models.ReturnRecord;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableRow;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import org.bson.Document;
import org.bson.types.ObjectId;

public class ManageReturnRequestsController extends ControllerWithLoader {

  @FXML
  private TableView<ReturnRow> loansTable;
  @FXML
  private TableColumn<ReturnRow, String> _idColumn;
  @FXML
  private TableColumn<ReturnRow, String> userEmailColumn;
  @FXML
  private TableColumn<ReturnRow, String> userNameColumn;
  @FXML
  private TableColumn<ReturnRow, String> bookTitleColumn;
  @FXML
  private TableColumn<ReturnRow, String> borrowConditionColumn;
  @FXML
  private TableColumn<ReturnRow, String> borrowConditionNotesColumn;
  @FXML
  private TableColumn<ReturnRow, String> returnConditionNotesColumn;
  @FXML
  private TableColumn<ReturnRow, String> borrowDateColumn;
  @FXML
  private TableColumn<ReturnRow, String> dueDateColumn;

  // Processing is handled via popup dialog; side panel controls removed from FXML
  @FXML
  private Button refreshBtn;

  private ObservableList<ReturnRow> loans = FXCollections.observableArrayList();

  @FXML
  public void initialize() {
    showCancel(false);
    _idColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null && cell.getValue().getLoan().getBookLoan().get_id() != null
            ? cell.getValue().getLoan().getBookLoan().get_id().toString() : "N/A"));
    userEmailColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getUser() != null ? cell.getValue().getUser().getEmail() : (cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? cell.getValue().getLoan().getBookLoan().getUserId() : "N/A")
    ));
    userNameColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getUser() != null ? cell.getValue().getUser().getDisplayName() : (cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? cell.getValue().getLoan().getBookLoan().getUserId() : "N/A")
    ));
    bookTitleColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getLoan() != null ? cell.getValue().getLoan().getTitleBook() : "N/A"));
    borrowConditionColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
      cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ?
        (cell.getValue().getLoan().getBookLoan().getBorrowCondition() != null ? cell.getValue().getLoan().getBookLoan().getBorrowCondition() : "") : ""));
    borrowConditionNotesColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
      cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ?
        (cell.getValue().getLoan().getBookLoan().getBorrowConditionNotes() != null ? cell.getValue().getLoan().getBookLoan().getBorrowConditionNotes() : "") : ""));
    returnConditionNotesColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
      cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ?
        (cell.getValue().getLoan().getBookLoan().getReturnConditionNotes() != null ? cell.getValue().getLoan().getBookLoan().getReturnConditionNotes() : "") : ""));
    borrowDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? DateUtil.dateToString(cell.getValue().getLoan().getBookLoan().getBorrowDate()) : "N/A"
    ));
    dueDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? DateUtil.dateToString(cell.getValue().getLoan().getBookLoan().getDueDate()) : "N/A"
    ));

    // refresh button remains
    refreshBtn.setOnAction(e -> loadLoans());
    loansTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
      // no side panel controls to manage; selection only affects context menu availability
    });

      // context menu to process returns via right-click
      loansTable.setRowFactory(tableView -> {
        final TableRow<ReturnRow> row = new TableRow<>();
        final ContextMenu contextMenu = new ContextMenu();
        final MenuItem processItem = new MenuItem("Process Return");
        processItem.setOnAction(e -> {
          ReturnRow rr = row.getItem();
          if (rr != null) openProcessReturnDialog(rr);
        });
        final MenuItem refreshItem = new MenuItem("Refresh");
        refreshItem.setOnAction(e -> loadLoans());
        contextMenu.getItems().addAll(processItem, refreshItem);
        row.contextMenuProperty().bind(
            javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
                .otherwise(contextMenu));
        return row;
      });

    // processReturn now via popup only (context menu or double-click)
    loadLoans();
  }

  private void loadLoans() {
    Task<List<ReturnBookLoan>> task = new Task<>() {
      @Override
      protected List<ReturnBookLoan> call() {
        return BookLoanController.getAcceptedLoans(0, 200);
      }
    };
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      List<ReturnBookLoan> vals = task.getValue();
      loans.clear();
      if (vals == null || vals.isEmpty()) {
        loansTable.setItems(loans);
        return;
      }
      try {
        List<String> ids = vals.stream()
            .map(v -> v.getBookLoan() != null ? v.getBookLoan().getUserId() : null)
            .filter(id -> id != null)
            .toList();
        List<User> users = null;
        try {
          users = UserController.listUsers(ids);
        } catch (Throwable t) {
          System.err.println("ManageReturnRequestsController: UserController.listUsers threw: " + t.getMessage());
          t.printStackTrace();
        }
        Map<String, User> userMap = users != null ? users.stream().collect(
            Collectors.toMap(User::getUid, u -> u, (u1, u2) -> u1)
        ) : new java.util.HashMap<>();
        for (ReturnBookLoan r : vals) {
          User u = null;
          if (r.getBookLoan() != null) u = userMap.get(r.getBookLoan().getUserId());
          loans.add(new ReturnRow(r, u));
        }
      } catch (Throwable ex) {
        System.err.println("ManageReturnRequestsController.loadLoans: unexpected throwable: " + ex.getMessage());
        ex.printStackTrace();
        for (ReturnBookLoan r : vals) {
          try {
            loans.add(new ReturnRow(r, null));
          } catch (Throwable inner) {
            inner.printStackTrace();
          }
        }
      }
      loansTable.setItems(loans);
    });
    task.setOnFailed(e -> { showLoading(false); });
    new Thread(task).start();
  }

  private static class ReturnRow {
    private final ReturnBookLoan loan;
    private final User user;

    public ReturnRow(ReturnBookLoan loan, User user) {
      this.loan = loan;
      this.user = user;
    }

    public ReturnBookLoan getLoan() { return loan; }
    public User getUser() { return user; }
  }

  private ReturnRow getSelected() {
    return loansTable.getSelectionModel().getSelectedItem();
  }

  // Processing now handled by dialog in openProcessReturnDialog

  private void openProcessReturnDialog(ReturnRow row) {
    try {
      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle("Process Return");
      dialog.initOwner(loansTable.getScene().getWindow());

      javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
      content.setPadding(new javafx.geometry.Insets(10));
      javafx.scene.text.Text info = new javafx.scene.text.Text("User: " + (row.getUser() != null ? row.getUser().getDisplayName() + " (" + row.getUser().getEmail() + ")" : row.getLoan().getBookLoan().getUserId()));
      javafx.scene.text.Text book = new javafx.scene.text.Text("Book: " + row.getLoan().getTitleBook());
      javafx.scene.control.Label returnCondLabel = new javafx.scene.control.Label("Return Condition");
      javafx.scene.control.ComboBox<String> returnCond = new javafx.scene.control.ComboBox<>();
      returnCond.getItems().addAll("NORMAL", "LATE", "DAMAGED", "LOST");
      returnCond.setValue("NORMAL");
      javafx.scene.control.Label borrowNoteLabel = new javafx.scene.control.Label("Borrow Note (read-only)");
      javafx.scene.control.TextArea borrowNoteArea = new javafx.scene.control.TextArea();
      borrowNoteArea.setPrefRowCount(2);
      // pre-fill with original borrow note if present and make it read-only
      try {
        String existingBorrowNote = row.getLoan().getBookLoan().getBorrowConditionNotes();
        if (existingBorrowNote != null) borrowNoteArea.setText(existingBorrowNote);
      } catch (Exception ex) { }
      borrowNoteArea.setEditable(false);
      borrowNoteArea.setMouseTransparent(true);
      javafx.scene.control.Label notesLabel = new javafx.scene.control.Label("Condition Notes");
      javafx.scene.control.TextArea notesArea = new javafx.scene.control.TextArea();
      notesArea.setPrefRowCount(3);
      javafx.scene.control.Label penaltyLabel = new javafx.scene.control.Label("Penalty Amount");
      javafx.scene.control.TextField penaltyField = new javafx.scene.control.TextField();

      content.getChildren().addAll(info, book, borrowNoteLabel, borrowNoteArea, returnCondLabel, returnCond, notesLabel, notesArea, penaltyLabel, penaltyField);

      dialog.getDialogPane().setContent(content);
      ButtonType processType = new ButtonType("Process", ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelType = ButtonType.CANCEL;
      dialog.getDialogPane().getButtonTypes().addAll(processType, cancelType);

      javafx.scene.control.Button processBtn = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(processType);
      processBtn.getStyleClass().addAll("btn", "btn-primary");
      processBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
        final String cond = returnCond.getValue();
        final String notes = notesArea.getText();
        // Do not allow modifying borrow note at return time; pass null so existing note is preserved
        final String borrowNote = null;
        String p = penaltyField.getText();
        Double parsedPenalty = null;
        try {
          if (p != null && !p.isBlank()) parsedPenalty = Double.parseDouble(p);
        } catch (Exception ex) {
          parsedPenalty = null;
        }
        final Double penalty = parsedPenalty;
        final ObjectId id = row.getLoan().getBookLoan().get_id();
        if ("DAMAGED".equals(cond) && (notes == null || notes.isBlank())) {
          AlertDialog.showAlert("info", "Info", "Please provide damage details for DAMAGED condition.", null);
          e.consume();
          return;
        }
        setLoadingText("Processing return...");
        Task<Document> task = new Task<>() {
          @Override
          protected Document call() {
            String adminId = AuthController.getInstance().getCurrentUser().getUid();
            return BookLoanController.processReturn(id, adminId, cond, notes, borrowNote, penalty);
          }
        };
        task.setOnRunning(ev -> showLoading(true));
        task.setOnSucceeded(ev -> { showLoading(false); if (task.getValue() == null) { AlertDialog.showAlert("error","Failed","Could not process return.", null); } else { AlertDialog.showAlert("success","Processed","Return processed successfully.", null); loadLoans(); dialog.close(); } });
        task.setOnFailed(ev -> { showLoading(false); AlertDialog.showAlert("error","Error","Failed to process return", null); });
        new Thread(task).start();
        e.consume();
      });

      dialog.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

