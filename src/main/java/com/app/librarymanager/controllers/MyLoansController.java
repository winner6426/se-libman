package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.BookLoanController.ReturnBookLoan;
import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookLoan.Mode;
import com.app.librarymanager.models.User;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DateUtil;
import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.List;
import org.bson.Document;

public class MyLoansController extends ControllerWithLoader {

  @FXML
  private ScrollPane loansScrollPane;
  @FXML
  private FlowPane loansFlowPane;
  @FXML
  private TextField searchField;
  @FXML
  private Label searchStatus;
  @FXML
  private ComboBox<String> modeFilter;
  @FXML
  private ComboBox<String> statusFilter;
  @FXML
  private ComboBox<String> validityFilter;
  @FXML
  private ComboBox<Integer> pageSize;
  @FXML
  private Button prevPageButton;
  @FXML
  private Button nextPageButton;

  private int pageSizeValue = 10;
  private int currentPage = 0;
  private String validity = "All";
  private String mode = "All";
  private int totalResults = 0;

  private ObservableList<ReturnBookLoan> loans = FXCollections.observableArrayList();
  private PauseTransition pauseTransition;
  private List<Task<HBox>> renderTasks = new ArrayList<>();

  @FXML
  private void initialize() {
    validityFilter.getItems().addAll("All", "Valid", "Invalid");
    validityFilter.setValue(validity);
    modeFilter.getItems().addAll("All", "Online", "Offline");
    modeFilter.setValue(mode);
    statusFilter.getItems().addAll("All", "PENDING", "AVAILABLE", "REJECTED", "EXPIRED", "RETURNED");
    statusFilter.setValue("All");
    pageSize.getItems().addAll(5, 10, 15, 20);
    pageSize.setValue(10);
    loansScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
      double deltaY = event.getDeltaY() * 3;
      loansScrollPane.setVvalue(
          loansScrollPane.getVvalue() - deltaY / loansScrollPane.getContent().getBoundsInLocal()
              .getHeight());
      event.consume();
    });
    loansScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> {
      loansFlowPane.setPrefWidth(newValue.getWidth());
    });
    loadLoans();
    pauseTransition = new PauseTransition(Duration.millis(200));
    pauseTransition.setOnFinished(event -> handleSearch());
    searchField.textProperty().addListener((observable, oldValue, newValue) -> {
      pauseTransition.playFromStart();
    });
    showCancel(false);
    pageSize.valueProperty().addListener((observable, oldValue, newValue) -> {
      pageSizeValue = newValue;
      currentPage = 0;
      loadLoans();
    });

    nextPageButton.setOnAction(event -> {
      currentPage++;
      loadLoans();
    });

    prevPageButton.setOnAction(event -> {
      if (currentPage > 0) {
        currentPage--;
        loadLoans();
      }
    });

    modeFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      mode = newValue;
      loadLoans();
    });

    statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      loadLoans();
    });

    validityFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
      currentPage = 0;
      validity = newValue;
      loadLoans();
    });
  }

  private void loadLoans() {
    // Ensure user is authenticated and present before loading loans
    if (!AuthController.getInstance().isAuthenticated() || AuthController.getInstance().getCurrentUser() == null) {
      AuthController.requireLogin();
      return;
    }

    Task<List<ReturnBookLoan>> task = new Task<>() {
      @Override
      protected List<ReturnBookLoan> call() {
        User currentUser = AuthController.getInstance().getCurrentUser();
        try {
          totalResults = (int) BookLoanController.countAllLoansOfUser(currentUser.getUid());
        } catch (Exception e) {
          totalResults = 0;
          System.err.println("Error counting loans for user " + (currentUser != null ? currentUser.getUid() : "null"));
          e.printStackTrace();
        }
        nextPageButton.setDisable((currentPage + 1) * pageSizeValue >= totalResults);
        prevPageButton.setDisable(currentPage == 0);
        try {
          return BookLoanController.getAllLentBookOf(currentUser.getUid(), currentPage * pageSizeValue,
              pageSizeValue);
        } catch (Exception e) {
          System.err.println("Error loading loans for user " + (currentUser != null ? currentUser.getUid() : "null"));
          e.printStackTrace();
          return new ArrayList<>();
        }
      }
    };

    task.setOnRunning(event -> showLoading(true));

    task.setOnSucceeded(event -> {
      showLoading(false);
      List<ReturnBookLoan> vals = task.getValue();
      if (vals == null) {
        searchStatus.setText("0 results found");
        loans.clear();
        updateLoansFlowPane(loans);
        return;
      }
      // apply status filter on client-side
      String statusFilterValue = statusFilter != null ? statusFilter.getValue() : "All";
      if (statusFilterValue != null && !"All".equals(statusFilterValue)) {
        vals = vals.stream().filter(r -> r.getBookLoan() != null && r.getBookLoan().getStatus() != null && r.getBookLoan().getStatus().toString().equalsIgnoreCase(statusFilterValue)).toList();
      }
      searchStatus.setText(vals.size() + " results found");
      loans.clear();
      loans.addAll(vals);
      updateLoansFlowPane(loans);
    });

    task.setOnFailed(event -> {
      showLoading(false);
      Throwable ex = task.getException();
      if (ex != null) ex.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to load loans: " + (ex != null ? ex.getMessage() : "Unknown"), null);
    });

    new Thread(task).start();
  }

  private void updateLoansFlowPane(ObservableList<ReturnBookLoan> loans) {
    renderTasks.forEach(Task::cancel);
    renderTasks.clear();
    loansFlowPane.getChildren().clear();
    // Sort loans so that PENDING come first, then AVAILABLE (active), then EXPIRED, then RETURNED, then others
    List<ReturnBookLoan> sortedLoans = new ArrayList<>(loans);
    sortedLoans.sort((a, b) -> {
      int pa = statusPriority(a.getBookLoan());
      int pb = statusPriority(b.getBookLoan());
      if (pa != pb) return Integer.compare(pa, pb);
      // Tie breaker: newer request first
      java.util.Date da = a.getBookLoan().getRequestDate();
      java.util.Date db = b.getBookLoan().getRequestDate();
      if (da == null && db == null) return 0;
      if (da == null) return 1;
      if (db == null) return -1;
      return db.compareTo(da);
    });

    for (ReturnBookLoan loan : sortedLoans) {
      Task<HBox> task = new Task<>() {
        @Override
        protected HBox call() {
          return createLoanCell(loan);
        }
      };
      task.setOnSucceeded(event -> loansFlowPane.getChildren().add(task.getValue()));
      task.setOnFailed(event -> {
        Throwable ex = task.getException();
        if (ex != null) ex.printStackTrace();
        AlertDialog.showAlert("error", "Error", "Failed to load loan cell: " + (ex != null ? ex.getMessage() : "Unknown"), null);
      });
      new Thread(task).start();

      renderTasks.add(task);
    }
    searchStatus.setText(loans.size() + " results found");
  }

  private HBox createLoanCell(ReturnBookLoan item) {
    HBox content = new HBox();
    content.getStyleClass().add("flowPane-cell");
    ImageView thumbnail = new ImageView();
    VBox details = new VBox(3);
    Text title = new Text();
    title.setWrappingWidth(280);
    Text dates = new Text();
    Text borrowNotes = new Text();
    borrowNotes.setWrappingWidth(280);
    Text returnNotes = new Text();
    returnNotes.setWrappingWidth(280);
    Label type = new Label();
    Label statusLabel = new Label();
    Text numCopies = new Text();
    title.setOnMouseClicked(event -> handleBookLoanClick(item.getBookLoan().getBookId(), content));
    title.getStyleClass().add("link");
    HBox actionButtons = new HBox();
    actionButtons.setAlignment(Pos.CENTER_LEFT);
    Button returnButton = new Button("Request Return");
    Button reBorrowButton = new Button("Re-borrow");
    Button readButton = new Button("Read");
    Button cancelRequestButton = new Button("Cancel");

    reBorrowButton.getStyleClass().addAll("btn", "btn-default");
    returnButton.getStyleClass().addAll("btn", "btn-danger");
    readButton.getStyleClass().addAll("btn", "btn-primary");
    cancelRequestButton.getStyleClass().addAll("btn", "btn-danger");

    try {
      String thumb = item.getThumbnailBook();
      if (thumb != null && !thumb.isBlank()) {
        thumbnail.setImage(new Image(thumb, true));
      } else {
        thumbnail.setImage(null);
      }
    } catch (Exception e) {
      // Don't crash the cell render on bad image URLs
      e.printStackTrace();
      thumbnail.setImage(null);
    }
    thumbnail.setFitHeight(180);
    thumbnail.setPreserveRatio(true);
    title.setText(item.getTitleBook());
    title.getStyleClass().add("bold");
    BookLoan loan = item.getBookLoan();
    if (BookLoan.Status.PENDING.equals(loan.getStatus())) {
      String req = loan.getRequestDate() != null ? DateUtil.dateToString(loan.getRequestDate()) : "N/A";
      dates.setText("Requested: " + req);
    } else {
      String borrow = loan.getBorrowDate() != null ? DateUtil.dateToString(loan.getBorrowDate()) : "N/A";
      String due = loan.getDueDate() != null ? DateUtil.dateToString(loan.getDueDate()) : "N/A";
      dates.setText(borrow + " - " + due);
    }
    // show borrow/return notes if available
    borrowNotes.setText(loan.getBorrowConditionNotes() != null ? "Borrow notes: " + loan.getBorrowConditionNotes() : "");
    returnNotes.setText(loan.getReturnConditionNotes() != null ? "Return notes: " + loan.getReturnConditionNotes() : "");
    type.setText(String.valueOf(loan.getType()));
    type.getStyleClass().addAll("chip", loan.getType().name().toLowerCase());
    String statusText = BookLoanController.computeDisplayStatus(loan);
    statusLabel.setText(statusText);
    statusLabel.getStyleClass().addAll("chip", statusText.toLowerCase());
    HBox chips = new HBox(type, statusLabel);
    if (loan.isReturnRequested()) {
      Label returnReq = new Label("RETURN REQUESTED");
      returnReq.getStyleClass().addAll("chip", "return-requested");
      chips.getChildren().add(returnReq);
    }

    Region spacer = new Region();
    VBox.setVgrow(spacer, Priority.ALWAYS);

    if (Mode.OFFLINE.equals(loan.getType())) {
      numCopies.setText("Copies: " + loan.getNumCopies());
    } else {
      numCopies.setVisible(false);
      numCopies.setManaged(false);
    }
    // actions depend on status
    if (BookLoan.Status.PENDING.equals(loan.getStatus())) {
      // pending - allow user to cancel their request
      returnButton.setVisible(false);
      returnButton.setManaged(false);
      reBorrowButton.setVisible(false);
      reBorrowButton.setManaged(false);
      cancelRequestButton.setVisible(true);
      cancelRequestButton.setManaged(true);
      cancelRequestButton.setOnAction(event -> handleCancelRequest(item));
    } else {
      // rejected or expired
      // hide return button by default for users - returns are processed by librarians
      returnButton.setVisible(false);
      returnButton.setManaged(false);
      reBorrowButton.setVisible(true);
      reBorrowButton.setOnAction(
          event -> handleBookLoanClick(item.getBookLoan().getBookId(), content));
    }
    if (Mode.ONLINE.equals(loan.getType())) {
      readButton.setVisible(BookLoan.Status.AVAILABLE.equals(loan.getStatus()));
      readButton.setManaged(BookLoan.Status.AVAILABLE.equals(loan.getStatus()));
      readButton.setOnAction(event -> handleReadBook(item));
    } else {
      readButton.setVisible(false);
      readButton.setManaged(false);
    }

    // hide cancel by default; only visible/managed for PENDING
    cancelRequestButton.setVisible(false);
    cancelRequestButton.setManaged(false);
    actionButtons.getChildren().addAll(returnButton, reBorrowButton, readButton, cancelRequestButton);
    actionButtons.setSpacing(5);
    details.getChildren().addAll(title, dates, borrowNotes, returnNotes, numCopies, chips, spacer, actionButtons);
    content.getChildren().addAll(thumbnail, details);
    content.setSpacing(10);
    chips.setSpacing(5);

    content.setUserData(loan);

    return content;
  }

  private void handleReturnBook(ReturnBookLoan item) {
    // deprecated: direct returning is now handled by librarians
    AlertDialog.showAlert("info", "Info", "Returning books must be processed by librarians. Use Request Return instead.", null);
  }

  private void handleRequestReturn(ReturnBookLoan item) {
    if (!AlertDialog.showConfirm("Request return", "Are you sure you want to request a return for this loan?")) {
      return;
    }

    BookLoan bookLoan = item.getBookLoan();
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        return BookLoanController.requestReturn(bookLoan.get_id());
      }
    };

    setLoadingText("Requesting return...");

    task.setOnSucceeded(event -> {
      setLoadingText(null);
      AlertDialog.showAlert("success", "Success", "Return requested. A librarian will process this return.", null);
      Document result = task.getValue();
      if (result != null) {
        BookLoan updated = new BookLoan(result);
        item.setBookLoan(updated);
        updateLoanInFlowPane(item);
      }
    });

    task.setOnFailed(event -> {
      setLoadingText(null);
      AlertDialog.showAlert("error", "Error", "Failed to request return", null);
    });

    new Thread(task).start();
  }

  private void handleCancelRequest(ReturnBookLoan item) {
    BookLoan bookLoan = item.getBookLoan();
    if (!BookLoan.Status.PENDING.equals(bookLoan.getStatus())) {
      AlertDialog.showAlert("info", "Info", "Only pending requests can be cancelled.", null);
      return;
    }

    if (!AlertDialog.showConfirm("Cancel request", "Are you sure you want to cancel this loan request?")) {
      return;
    }
    Task<Document> task = new Task<>() {
      @Override
      protected Document call() {
        String userId = AuthController.getInstance().getCurrentUser().getUid();
        return BookLoanController.rejectRequest(bookLoan.get_id(), userId, "Canceled by user");
      }
    };

    setLoadingText("Cancelling request...");

    task.setOnRunning(event -> showLoading(true));

    task.setOnSucceeded(event -> {
      showLoading(false);
      if (task.getValue() == null) {
        AlertDialog.showAlert("error", "Cancel Failed", "Could not cancel request.", null);
      } else {
        AlertDialog.showAlert("success", "Cancelled", "Request cancelled.", null);
        BookLoan updatedLoan = new BookLoan(task.getValue());
        item.setBookLoan(updatedLoan);
        updateLoanInFlowPane(item);
      }
    });

    task.setOnFailed(event -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", "Failed to cancel request", null);
    });

    new Thread(task).start();
  }

  private void handleReBorrowBook(ReturnBookLoan item) {

  }

  private void handleReadBook(ReturnBookLoan item) {
    String bookId = item.getBookLoan().getBookId();
    Book b = BookController.findBookByID(bookId);
    try {
      if (!b.isActivated()) {
        AlertDialog.showAlert("error", "Error", "Book is inactive and not available for reading",
            null);
        return;
      }
      if (b.getPdfLink() == null || b.getPdfLink().isEmpty() || b.getPdfLink().equals("N/A")) {
        AlertDialog.showAlert("error", "Error", "No pdf link found", null);
        return;
      }
      Desktop.getDesktop().browse(URI.create(b.getPdfLink()));
    } catch (Exception e) {
      e.printStackTrace();
      AlertDialog.showAlert("error", "Error", "Failed to open book", null);
    }
  }

  private void updateLoanInFlowPane(ReturnBookLoan bookLoan) {
    loansFlowPane.getChildren().removeIf(node -> {
      HBox loanCell = (HBox) node;
      BookLoan loan = (BookLoan) loanCell.getUserData();
      return loan.get_id().equals(bookLoan.getBookLoan().get_id());
    });
    loansFlowPane.getChildren().add(createLoanCell(
        new ReturnBookLoan(bookLoan.getBookLoan(), bookLoan.getTitleBook(),
            bookLoan.getThumbnailBook())));
    if ("Valid".equals(validity)) {
      loadLoans();
    }
  }

  /**
   * Priority used for sorting loans in the UI.
   * 0 = PENDING
   * 1 = AVAILABLE and valid (currently borrowed)
   * 2 = EXPIRED
   * 3 = Returned (had borrowDate and now not valid)
   * 4 = others (e.g., REJECTED)
   */
  private int statusPriority(BookLoan loan) {
    if (loan == null) return 4;
    if (BookLoan.Status.PENDING.equals(loan.getStatus())) return 0;
    if (BookLoan.Status.AVAILABLE.equals(loan.getStatus()) && loan.isValid()) return 1;
    if (BookLoan.Status.EXPIRED.equals(loan.getStatus())) return 2;
    if (loan.getBorrowDate() != null && !loan.isValid()) return 3;
    return 4;
  }

  private void handleBookLoanClick(String id, Parent container) {
    try {
      FXMLLoader loader = new FXMLLoader(
          getClass().getResource("/views/components/book-detail.fxml"));
      Parent root = loader.load();
      BookDetailController controller = loader.getController();
      controller.getBookDetail(id);

      StackPane overlay = new StackPane(root);
      overlay.getStyleClass().add("overlay");
      StackPane stackPane = (StackPane) container.getScene().lookup("#contentPane");
      if (stackPane != null) {
        stackPane.getChildren().add(overlay);
        Button closeButton = (Button) root.lookup("#closeBtn");
        closeButton.setOnAction(event -> stackPane.getChildren().remove(overlay));
      } else {
        //  System.err.println("StackPane with id 'contentPane' not found.");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void handleSearch() {
    searchStatus.setText("Searching...");
    String query = searchField.getText().trim();
    if (query.isEmpty()) {
      updateLoansFlowPane(loans);
      return;
    }

    FilteredList<ReturnBookLoan> filteredLoans = loans.filtered(
        loan -> loan.getTitleBook().toLowerCase().contains(query.toLowerCase()));
    searchStatus.setText(filteredLoans.size() + " results found");
    updateLoansFlowPane(filteredLoans);
  }
}