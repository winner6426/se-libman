package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.User;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import org.bson.Document;
import org.bson.types.ObjectId;

public class ManageLoanRequestsController extends ControllerWithLoader {

  @FXML
  private TableView<RequestRow> requestsTable;
  @FXML
  private TableColumn<RequestRow, String> _idColumn;
  @FXML
  private TableColumn<RequestRow, String> userEmailColumn;
  @FXML
  private TableColumn<RequestRow, String> userNameColumn;
  @FXML
  private TableColumn<RequestRow, String> bookTitleColumn;
  @FXML
  private TableColumn<RequestRow, String> numCopiesColumn;
  @FXML
  private TableColumn<RequestRow, String> requestDateColumn;

  @FXML
  private TextArea conditionNotes;
  @FXML
  private Button approveBtn;
  @FXML
  private Button rejectBtn;
  @FXML
  private Button refreshBtn;

  private ObservableList<RequestRow> requests = FXCollections.observableArrayList();

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
    numCopiesColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
      String.valueOf(cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? cell.getValue().getLoan().getBookLoan().getNumCopies() : 0)
    ));
    requestDateColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(
      cell.getValue() != null && cell.getValue().getLoan() != null && cell.getValue().getLoan().getBookLoan() != null ? DateUtil.dateToString(cell.getValue().getLoan().getBookLoan().getRequestDate()) : "N/A"
    ));

    loadRequests();

    approveBtn.setOnAction(e -> onApprove());
    rejectBtn.setOnAction(e -> onReject());
    approveBtn.setDisable(true);
    rejectBtn.setDisable(true);
    requestsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
      boolean has = newV != null;
      approveBtn.setDisable(!has);
      rejectBtn.setDisable(!has);
      if (!has) {
        conditionNotes.clear();
      }
    });
    refreshBtn.setOnAction(e -> loadRequests());
  }

  private void loadRequests() {
    Task<List<ReturnBookLoan>> task = new Task<>() {
      @Override
      protected List<ReturnBookLoan> call() {
        return BookLoanController.getPendingRequests(0, 100);
      }
    };
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      List<ReturnBookLoan> vals = task.getValue();
      requests.clear();
      if (vals == null || vals.isEmpty()) {
        // no pending requests
        requestsTable.setItems(requests);
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
          System.err.println("ManageLoanRequestsController: UserController.listUsers threw: " + t.getMessage());
          t.printStackTrace();
        }
        // merge duplicates by uid when collecting
        Map<String, User> userMap = users != null ? users.stream().collect(Collectors.toMap(User::getUid, u -> u, (u1, u2) -> u1)) : new java.util.HashMap<>();
        for (ReturnBookLoan r : vals) {
          User u = null;
          if (r.getBookLoan() != null) u = userMap.get(r.getBookLoan().getUserId());
          requests.add(new RequestRow(r, u));
        }
      } catch (Throwable ex) {
        System.err.println("ManageLoanRequestsController.loadRequests: unexpected throwable: " + ex.getMessage());
        ex.printStackTrace();
        for (ReturnBookLoan r : vals) {
          try {
            requests.add(new RequestRow(r, null));
          } catch (Throwable inner) {
            inner.printStackTrace();
          }
        }
      }
      requestsTable.setItems(requests);
    });
    task.setOnFailed(e -> {
      showLoading(false);
    });
    new Thread(task).start();
  }

  private RequestRow getSelected() {
    return requestsTable.getSelectionModel().getSelectedItem();
  }

  private static class RequestRow {
    private final ReturnBookLoan loan;
    private final User user;

    public RequestRow(ReturnBookLoan loan, User user) {
      this.loan = loan;
      this.user = user;
    }

    public ReturnBookLoan getLoan() {
      return loan;
    }

    public User getUser() {
      return user;
    }
  }

  private void onApprove() {
    RequestRow sel = getSelected();
    if (sel == null) return;
    ObjectId id = sel.getLoan().getBookLoan().get_id();
    String notes = conditionNotes.getText();
    setLoadingText("Approving...");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        String adminId = AuthController.getInstance().getCurrentUser().getUid();
        return BookLoanController.approveRequest(id, adminId, notes);
      }
    };
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      if (task.getValue() == null) {
        AlertDialog.showAlert("error", "Approve Failed", "Could not approve request (maybe not enough copies).", null);
      } else {
        AlertDialog.showAlert("success", "Approved", "Request approved successfully.", null);
        loadRequests();
      }
    });
    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to approve request", null);
    });
    new Thread(task).start();
  }

  private void onReject() {
    RequestRow sel = getSelected();
    if (sel == null) return;
    ObjectId id = sel.getLoan().getBookLoan().get_id();
    String notes = conditionNotes.getText();
    setLoadingText("Rejecting...");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        String adminId = AuthController.getInstance().getCurrentUser().getUid();
        return BookLoanController.rejectRequest(id, adminId, notes);
      }
    };
    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      if (task.getValue() == null) {
        AlertDialog.showAlert("error", "Reject Failed", "Could not reject request.", null);
      } else {
        AlertDialog.showAlert("success", "Rejected", "Request rejected.", null);
        loadRequests();
      }
    });
    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to reject request", null);
    });
    new Thread(task).start();
  }
}
