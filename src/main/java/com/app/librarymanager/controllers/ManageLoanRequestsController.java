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
import javafx.scene.control.TableRow;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
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

    // borrow condition is fixed to NORMAL for requests; selection removed from UI

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

    // Add context menu to process requests via right-click (opens a popup with details)
    requestsTable.setRowFactory(tableView -> {
      final TableRow<RequestRow> row = new TableRow<>();
      final ContextMenu contextMenu = new ContextMenu();
      final MenuItem processItem = new MenuItem("Process Request");
      processItem.setOnAction(e -> {
        RequestRow rr = row.getItem();
        if (rr != null) openProcessRequestDialog(rr);
      });
      final MenuItem refreshItem = new MenuItem("Refresh");
      refreshItem.setOnAction(e -> loadRequests());
      contextMenu.getItems().addAll(processItem, refreshItem);
      row.contextMenuProperty().bind(
          javafx.beans.binding.Bindings.when(row.emptyProperty()).then((ContextMenu) null)
              .otherwise(contextMenu));
      return row;
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
          users = UserController.listUsersSafe(ids);
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
    String borrowCond = "NORMAL";
    setLoadingText("Approving...");
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        String adminId = AuthController.getInstance().getCurrentUser().getUid();
        return BookLoanController.approveRequest(id, adminId, borrowCond, notes);
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

  private void openProcessRequestDialog(RequestRow row) {
    try {
      Dialog<Void> dialog = new Dialog<>();
      dialog.setTitle("Process Loan Request");
      dialog.initOwner(requestsTable.getScene().getWindow());

      // build content
      javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8);
      content.setPadding(new javafx.geometry.Insets(10));
      javafx.scene.text.Text info = new javafx.scene.text.Text("User: " + (row.getUser() != null ? row.getUser().getDisplayName() + " (" + row.getUser().getEmail() + ")" : row.getLoan().getBookLoan().getUserId()));
      javafx.scene.text.Text book = new javafx.scene.text.Text("Book: " + row.getLoan().getTitleBook());
      javafx.scene.control.Label notesLabel = new javafx.scene.control.Label("Condition Notes");
      javafx.scene.control.TextArea notesArea = new javafx.scene.control.TextArea();
      notesArea.setPrefRowCount(3);

      content.getChildren().addAll(info, book, notesLabel, notesArea);

      dialog.getDialogPane().setContent(content);
      ButtonType approveType = new ButtonType("Approve", ButtonBar.ButtonData.OK_DONE);
      ButtonType rejectType = new ButtonType("Reject", ButtonBar.ButtonData.NO);
      ButtonType cancelType = ButtonType.CANCEL;
      dialog.getDialogPane().getButtonTypes().addAll(approveType, rejectType, cancelType);

      dialog.setResultConverter(b -> null);

      javafx.scene.control.Button approveBtn = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(approveType);
      approveBtn.getStyleClass().addAll("btn", "btn-primary");
      approveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
        String borrowCondVal = "NORMAL";
        String notes = notesArea.getText();
        // call approve
        ObjectId id = row.getLoan().getBookLoan().get_id();
        setLoadingText("Approving...");
        Task<Document> task = new Task<>() {
          @Override
          protected Document call() {
            String adminId = AuthController.getInstance().getCurrentUser().getUid();
            return BookLoanController.approveRequest(id, adminId, borrowCondVal, notes);
          }
        };
        task.setOnRunning(ev -> showLoading(true));
        task.setOnSucceeded(ev -> { showLoading(false); if (task.getValue() == null) { AlertDialog.showAlert("error","Approve Failed","Could not approve request.",null); } else { AlertDialog.showAlert("success","Approved","Request approved.", null); loadRequests(); dialog.close(); } });
        task.setOnFailed(ev -> { showLoading(false); AlertDialog.showAlert("error","Error","Failed to approve request", null); });
        new Thread(task).start();
        e.consume();
      });

      javafx.scene.control.Button rejectBtn = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(rejectType);
      rejectBtn.getStyleClass().addAll("btn", "btn-danger");
      rejectBtn.addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
        String notes = notesArea.getText();
        ObjectId id = row.getLoan().getBookLoan().get_id();
        setLoadingText("Rejecting...");
        Task<Document> task = new Task<>() {
          @Override
          protected Document call() {
            String adminId = AuthController.getInstance().getCurrentUser().getUid();
            return BookLoanController.rejectRequest(id, adminId, notes);
          }
        };
        task.setOnRunning(ev -> showLoading(true));
        task.setOnSucceeded(ev -> { showLoading(false); if (task.getValue() == null) { AlertDialog.showAlert("error","Reject Failed","Could not reject request.",null); } else { AlertDialog.showAlert("success","Rejected","Request rejected.", null); loadRequests(); dialog.close(); } });
        task.setOnFailed(ev -> { showLoading(false); AlertDialog.showAlert("error","Error","Failed to reject request", null); });
        new Thread(task).start();
        e.consume();
      });

      dialog.showAndWait();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
