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
  private TableColumn<ReturnRow, String> borrowDateColumn;
  @FXML
  private TableColumn<ReturnRow, String> dueDateColumn;

  @FXML
  private ComboBox<String> returnCondition;
  @FXML
  private TextArea conditionNotes;
  @FXML
  private TextField penaltyAmount;
  @FXML
  private Button processReturnBtn;
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
    borrowDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? DateUtil.dateToString(cell.getValue().getLoan().getBookLoan().getBorrowDate()) : "N/A"
    ));
    dueDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
        cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? DateUtil.dateToString(cell.getValue().getLoan().getBookLoan().getDueDate()) : "N/A"
    ));

    returnCondition.getItems().addAll("NORMAL", "LATE", "DAMAGED", "LOST");
    returnCondition.setValue("NORMAL");
    processReturnBtn.setDisable(true);
    refreshBtn.setOnAction(e -> loadLoans());
    loansTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
      boolean has = newV != null;
      processReturnBtn.setDisable(!has);
      if (!has) {
        conditionNotes.clear();
        penaltyAmount.clear();
      }
    });

    processReturnBtn.setOnAction(e -> onProcessReturn());
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

  private void onProcessReturn() {
    ReturnRow sel = getSelected();
    if (sel == null) return;
    ObjectId id = sel.getLoan().getBookLoan().get_id();
    String condition = returnCondition.getValue();
    String notes = conditionNotes.getText();
    Double parsedPenalty = null;
    try {
      String p = penaltyAmount.getText();
      if (p != null && !p.isBlank()) {
        parsedPenalty = Double.parseDouble(p);
      }
    } catch (Exception e) {
      parsedPenalty = null;
    }
    final Double penalty = parsedPenalty;

    if ("DAMAGED".equals(condition) && (notes == null || notes.isBlank())) {
      AlertDialog.showAlert("info", "Info", "Please provide damage details for DAMAGED condition.", null);
      return;
    }

    if (!AlertDialog.showConfirm("Process return", "Are you sure you want to process this return?")) return;

    setLoadingText("Processing return...");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        String adminId = AuthController.getInstance().getCurrentUser().getUid();
        return BookLoanController.processReturn(id, adminId, condition, notes, penalty);
      }
    };
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      if (task.getValue() == null) {
        AlertDialog.showAlert("error", "Failed", "Could not process return.", null);
      } else {
        AlertDialog.showAlert("success", "Processed", "Return processed successfully.", null);
        loadLoans();
      }
    });
    task.setOnFailed(e -> { showLoading(false); AlertDialog.showAlert("error", "Error", "Failed to process return.", null); });
    new Thread(task).start();
  }

}

