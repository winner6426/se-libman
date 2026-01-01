package com.app.librarymanager.controllers;

import com.app.librarymanager.controllers.CommentController.ReturnUserComment;
import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookUser;
import com.app.librarymanager.models.Comment;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.AvatarUtil;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.StageManager;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import org.bson.Document;
// Use simple Text nodes instead of FontIcon to avoid runtime icon resolution issues

public class BookDetailController extends ControllerWithLoader {

  private Book book;
  private BookCopies copies;

  @FXML
  private VBox detailContainer;
  @FXML
  private ImageView bookCover;
  @FXML
  private Label bookTitle;
  @FXML
  private Label bookAuthor;
  @FXML
  private TextArea bookDescription;
  @FXML
  private Label bookPublisher;
  @FXML
  private FlowPane bookCategories;
  @FXML
  private Label bookLanguage;
  @FXML
  private Label bookPublishedDate;
  @FXML
  private Label bookIsbn;
  @FXML
  private Button closeBtn;
  @FXML
  private Label bookPublishingInfo;
  @FXML
  private Label availableCopies;
  @FXML
  private Text bookPrice;
  @FXML
  private Text bookDiscountPrice;
  @FXML
  private Text currencyCode;
  @FXML
  private HBox starsContainer;
  @FXML
  private Button addToFavorite;
  @FXML
  private Button borrowEBook;
  @FXML
  private Button borrowPhysicalBook;
  @FXML
  private ListView<ReturnUserComment> commentsContainer;
  @FXML
  private TextArea newCommentTextArea;
  @FXML
  private Button addCommentButton;
  @FXML
  private Label emptyComments;


  private List<ReturnUserComment> commentList = new ArrayList<>();

  private boolean isFavorite = false;

  @FXML
  private void initialize() {
    showCancel(false);
    borrowEBook.setOnAction(event -> handleBorrowEBook());
    borrowPhysicalBook.setOnAction(event -> handleBorrowPhysicalBook());
    addToFavorite.setOnAction(event -> handleAddToFavorite());
//    detailContainer.setVisible(false);
    addCommentButton.setOnAction(e -> handleAddComment());
    commentsContainer.setCellFactory(listView -> new ListCell<>() {
      @Override
      protected void updateItem(ReturnUserComment comment, boolean empty) {
        super.updateItem(comment, empty);
        if (empty || comment == null) {
          setGraphic(null);
        } else {
          Task<VBox> renderTask = new Task<>() {
            @Override
            protected VBox call() {
              return createCommentComponent(comment);
            }
          };

          renderTask.setOnSucceeded(event -> setGraphic(renderTask.getValue()));
          renderTask.setOnFailed(event -> {
            //  System.out.println("Failed to render comment for: " + comment.getUserDisplayName());
            setGraphic(null);
          });

          new Thread(renderTask).start();
        }
      }
    });
  }

  @FXML
  private void close() {
    try {
      // If displayed as an overlay inside the main content pane, remove the overlay
      StackPane contentPane = (StackPane) closeBtn.getScene().lookup("#contentPane");
      if (contentPane != null) {
        contentPane.getChildren().removeIf(node -> node.getStyleClass().contains("overlay"));
        return;
      }
      // Otherwise close the window
      javafx.stage.Stage stage = (javafx.stage.Stage) closeBtn.getScene().getWindow();
      stage.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Stage loadingStage;

  void getBookDetail(String id) {
    Task<Map<String, Object>> task = new Task<>() {
      @Override
      protected Map<String, Object> call() {
        Book b = BookController.findBookByID(id);
        Document cp = BookCopiesController.findCopy(new BookCopies(id));
        // Safe check for authenticated user before checking favorites
        boolean isFavorite = false;
        try {
          if (AuthController.getInstance().isAuthenticated() && AuthController.getInstance().getCurrentUser() != null) {
            isFavorite = FavoriteController.findFavorite(
                new BookUser(AuthController.getInstance().getCurrentUser().getUid(), id)) != null;
          }
        } catch (Exception e) {
          // Log and proceed; favorite is optional
          System.err.println("BookDetailController.getBookDetail: error checking favorite: " + e.getMessage());
          e.printStackTrace();
        }
        double avgRating = BookRatingController.averageRating(id);
        try {
          commentList = CommentController.getAllCommentOfBook(id);
          if (commentList == null) {
            commentList = new ArrayList<>();
          }
        } catch (Exception e) {
          System.err.println("BookDetailController.getBookDetail: failed to load comments for book " + id + ": " + e.getMessage());
          e.printStackTrace();
          commentList = new ArrayList<>();
        }

        copies = (cp != null) ? new BookCopies(cp) : new BookCopies(id);

        return Map.of(
            "book", b,
            "copies", copies,
            "isFavorite", isFavorite,
            "avgRating", avgRating
        );
      }
    };

    task.setOnRunning(event -> showLoading(true));

    task.setOnSucceeded(event -> {
      showLoading(false);
      Map<String, Object> result = task.getValue();

      Platform.runLater(() -> {
        book = (Book) result.get("book");
        isFavorite = (boolean) result.get("isFavorite");
        double avgRating = (double) result.get("avgRating");
        updateBookDetailsUI(book, avgRating);
      });

      new Thread(() -> loadCommentsInBatches(commentList)).start();
    });

    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
      AlertDialog.showAlert("error", "Book not found", "Failed to load book details", null);
    });

    new Thread(task).start();
  }

  private void updateBookDetailsUI(Book book, double avgRating) {
    if (book == null) {
      AlertDialog.showAlert("error", "Book not found", "No details available for this book.", null);
      return;
    }

    bookTitle.setText(book.isActivated() ? book.getTitle() : "[INACTIVE] " + book.getTitle());
    bookAuthor.setText("by " + String.join(", ", book.getAuthors()));
    bookPublishingInfo.setText("Published by " + book.getPublisher() + " on " +
        DateUtil.ymdToDmy(book.getPublishedDate()));
    bookDescription.setText(book.getDescription());
    bookLanguage.setText(book.getLanguage());
    availableCopies.setText("Available copies: " + copies.getCopies());
    bookPrice.setText(parsePrice(book.getPrice()));
    currencyCode.setText(book.getCurrencyCode());

    if (book.getDiscountPrice() > 0) {
      bookDiscountPrice.setText(parsePrice(book.getDiscountPrice()));
      bookPrice.getStyleClass().add("small-strike");
    } else {
      bookDiscountPrice.setVisible(false);
    }

    // Use a simple text heart as graphic to avoid ikonli issues
    Text heart = new Text("\u2665");
    heart.getStyleClass().add("heart-icon");
    addToFavorite.setGraphic(heart);
    addToFavorite.getStyleClass().add(isFavorite ? "on" : "off");
    borrowPhysicalBook.setDisable(copies.getCopies() == 0 || !book.isActivated());
    if (!book.isActivated()) {
      detailContainer.setStyle("-fx-opacity: 0.5;");
      borrowEBook.setDisable(true);
      addToFavorite.setDisable(true);
      newCommentTextArea.setDisable(true);
      addCommentButton.setDisable(true);
      AlertDialog.showAlert("warning", "Book is inactive",
          "This book is currently inactive and cannot be borrowed", null);
    }

    for (int i = 0; i < 5; i++) {
      Text star = new Text(i < Math.round(avgRating) ? "\u2605" : "\u2606");
      star.getStyleClass().add("star");
      starsContainer.getChildren().add(star);
    }
    starsContainer.getChildren().add(new Label("(" + avgRating + ")"));
    Task<Image> imageTask = new Task<>() {
      @Override
      protected Image call() {
        try {
          return new Image("https://books.google.com/books/content?id=" + book.getId() +
              "&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api");
        } catch (Exception e) {
          return new Image(
              "https://books.google.com/books/content?id=&printsec=frontcover&img=1&zoom=0&edge=curl&source=gbs_api");
        }
      }
    };
    imageTask.setOnSucceeded(event -> bookCover.setImage(imageTask.getValue()));
    new Thread(imageTask).start();
  }

  private void loadCommentsInBatches(List<ReturnUserComment> comments) {
    ObservableList<ReturnUserComment> observableComments = FXCollections.observableArrayList();
    commentsContainer.setItems(observableComments);

    if (comments == null || comments.isEmpty()) {
      Platform.runLater(() -> {
        emptyComments.setVisible(true);
        commentsContainer.setVisible(false);
        commentsContainer.setManaged(false);
      });
      return;
    } else {
      Platform.runLater(() -> {
        emptyComments.setVisible(false);
        commentsContainer.setVisible(true);
        commentsContainer.setManaged(true);
      });
    }

    commentsContainer.getItems().clear();

    for (int i = 0; i < comments.size(); i++) {
      int index = i;
      Platform.runLater(() -> observableComments.add(comments.get(index)));

      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }


  private String parsePrice(double price) {
    return String.format("%,.0f", price);
  }

  private void handleBorrowEBook() {
    System.out.println("Borrowing E-Book");
    // require login before borrowing
    if (!AuthController.getInstance().isAuthenticated() || AuthController.getInstance().getCurrentUser() == null) {
      AuthController.requireLogin();
      return;
    }
    Task<Void> task = new Task<>() {
      @Override
      protected Void call() {
        Platform.runLater(() -> {
          LocalDate borrowDate = LocalDate.now();
          LocalDate dueDate = borrowDate.plusDays(90);
          BookLoan bookLoan = new BookLoan(AuthController.getInstance().getCurrentUser().getUid(),
              book.getId(), borrowDate, dueDate);
          System.out.println("Borrowing E-Book: " + bookLoan.toString());
          boolean confirm = AlertDialog.showConfirm("Borrow E-Book",
              "Are you sure you want to borrow this E-Book?");
          if (!confirm) {
            return;
          }
          // check borrow limit for e-book (counts as 1)
          long current = BookLoanController.countBorrowedCopiesOfUser(
              AuthController.getInstance().getCurrentUser().getUid());
          if (current + 1 > BookLoanController.getMaxBorrowLimit()) {
            int max = BookLoanController.getMaxBorrowLimit();
            int requested = 1;
            int remaining = Math.max(0, max - (int) current);
            String msg = String.format("You cannot request this loan: requested=%d, current=%d, max=%d, remainingSlots=%d", requested, current, max, remaining);
            AlertDialog.showAlert("error", "Borrow limit exceeded", msg, null);
            return;
          }
          // create a loan request (pending) for admin approval
          Document doc = BookLoanController.createLoanRequest(bookLoan);
          if (doc != null) {
            AlertDialog.showAlert("success", "Request Sent",
                "Your borrow request has been sent to the librarian for approval.", null);
          } else {
            AlertDialog.showAlert("error", "Request Failed",
                "Failed to create borrow request. Please contact admin or try again later.", null);
          }
        });
        return null;
      }
    };
    setLoadingText("Borrowing E-Book...");
    task.setOnRunning(event -> showLoading(true));
    task.setOnSucceeded(event -> showLoading(false));
    task.setOnFailed(event -> {
      showLoading(false);
      task.getException().printStackTrace();
      AlertDialog.showAlert("error", "Failed to borrow E-Book",
          "An error occurred while borrowing the E-Book", null);
    });
    new Thread(task).start();
  }

  private void handleBorrowPhysicalBook() {
    System.out.println("Borrowing Physical Book");
    showBorrowPopup();
  }

  @FXML
  private void showBorrowPopup() {
    if (!AuthController.getInstance().isAuthenticated() || AuthController.getInstance().getCurrentUser() == null) {
      AuthController.requireLogin();
      return;
    }
    DatePicker fromDate = new DatePicker();
    DatePicker dueDate = new DatePicker();
    Button confirmButton = new Button("Confirm");
    Button cancelButton = new Button("Cancel");

    fromDate.setDayCellFactory(picker -> new DateCell() {
      @Override
      public void updateItem(LocalDate date, boolean empty) {
        super.updateItem(date, empty);
        setDisable(empty || date.isBefore(LocalDate.now()));
      }
    });

    fromDate.valueProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        if (dueDate.getValue() != null && newValue.isAfter(dueDate.getValue()) || newValue
            .equals(dueDate.getValue())) {
          dueDate.setValue(null);
        }
        dueDate.setDayCellFactory(picker -> new DateCell() {
          @Override
          public void updateItem(LocalDate date, boolean empty) {
            super.updateItem(date, empty);
            setDisable(empty || date.isBefore(newValue) || date.equals(newValue) || date.isAfter(
                newValue.plusDays(90)));
          }
        });
      }
    });
    int maxCopies = copies.getCopies();

    TextField copiesTextField = new TextField();

    UnaryOperator<TextFormatter.Change> filter = change -> {
      String newText = change.getControlNewText();
      if (newText.matches("\\d*")) {
        return change;
      }
      return null;
    };

    TextFormatter<Integer> textFormatter = new TextFormatter<>(filter);
    copiesTextField.setTextFormatter(textFormatter);
    copiesTextField.setPromptText("Less than or equal to " + maxCopies + "...");

    confirmButton.getStyleClass().addAll("btn", "btn-primary");
    cancelButton.getStyleClass().addAll("btn", "btn-text");
    HBox buttonsBox = new HBox(10, confirmButton, cancelButton);
    VBox vbox = new VBox(10.0);
    fromDate.setPromptText("Select borrow date");
    dueDate.setPromptText("Select due date");
    fromDate.getStyleClass().addAll("input", "date-picker");
    dueDate.getStyleClass().addAll("input", "date-picker");
    copiesTextField.getStyleClass().add("input");
    fromDate.getEditor().setOnMouseClicked(event -> fromDate.show());
    dueDate.getEditor().setOnMouseClicked(event -> dueDate.show());
    DatePickerUtil.disableEditor(fromDate);
    DatePickerUtil.disableEditor(dueDate);
    DatePickerUtil.setDatePickerFormat(fromDate);
    DatePickerUtil.setDatePickerFormat(dueDate);

    vbox.getChildren().addAll(new Label("From Date:"), fromDate, new Label("Due Date:"), dueDate,
        new Label("Number of Copies:"), copiesTextField, buttonsBox);
    vbox.setPadding(new Insets(20));
    Scene scene = new Scene(vbox);
    scene.getStylesheets()
        .add(Objects.requireNonNull(StageManager.class.getResource("/styles/global.css"))
            .toExternalForm());
    Stage popupStage = new Stage();
    popupStage.setHeight(350);
    popupStage.setWidth(300);
    popupStage.setResizable(false);

    popupStage.setScene(scene);
    popupStage.setTitle("Select Borrow Dates and Copies");
    popupStage.initModality(Modality.APPLICATION_MODAL);

    confirmButton.setOnAction(event -> {
      LocalDate borrowDate = fromDate.getValue();
      LocalDate returnDate = dueDate.getValue();
      if (borrowDate == null || returnDate == null) {
        AlertDialog.showAlert("error", "Invalid Dates",
            "Please select valid borrow and return dates", null);
        return;
      }
      if (copiesTextField.getText().isEmpty()) {
        AlertDialog.showAlert("error", "Invalid Number of Copies",
            "Please enter the number of copies you want to borrow", null);
        return;
      }
      int numCopies = Integer.parseInt(copiesTextField.getText());
      if (numCopies <= 0) {
        AlertDialog.showAlert("error", "Invalid Number of Copies",
            "The number of copies must be a positive number", null);
        return;
      }
      System.out.println("Borrowing Physical Book: " + borrowDate + " - " + returnDate + " - "
          + numCopies);
      if (numCopies > maxCopies) {
        AlertDialog.showAlert("error", "Invalid Number of Copies",
            "The number of copies you requested is more than the available copies", null);
        return;
      }
      BookLoan bookLoan = new BookLoan(AuthController.getInstance().getCurrentUser().getUid(),
          book.getId(), borrowDate, returnDate, numCopies);
        // check borrow limit
        long current = BookLoanController.countBorrowedCopiesOfUser(
          AuthController.getInstance().getCurrentUser().getUid());
        if (current + numCopies > BookLoanController.getMaxBorrowLimit()) {
        int max = BookLoanController.getMaxBorrowLimit();
        int requested = numCopies;
        int remaining = Math.max(0, max - (int) current);
        String msg = String.format("You cannot request this loan: requested=%d, current=%d, max=%d, remainingSlots=%d", requested, current, max, remaining);
        AlertDialog.showAlert("error", "Borrow limit exceeded", msg, null);
        return;
        }
      Document doc = BookLoanController.createLoanRequest(bookLoan);
      if (doc != null) {
        AlertDialog.showAlert("success", "Request Sent",
            "Your borrow request has been sent to the librarian for approval.", null);
      } else {
        AlertDialog.showAlert("error", "Request Failed",
            "Failed to create borrow request. You might have reached the borrow limit.", null);
      }
      popupStage.close();
    });

    cancelButton.setOnAction(event -> popupStage.close());

    popupStage.showAndWait();
  }

  private void handleAddToFavorite() {
    boolean success;
    if (isFavorite) {
      success = FavoriteController.removeFromFavorite(
          new BookUser(AuthController.getInstance().getCurrentUser().getUid(), book.getId()));
    } else {
      Document favorite = FavoriteController.addToFavorite(
          new BookUser(AuthController.getInstance().getCurrentUser().getUid(), book.getId()));
      success = favorite != null;
    }
    if (success) {
      isFavorite = !isFavorite;
      Text heart = new Text("\u2665");
      heart.getStyleClass().add("heart-icon");
      addToFavorite.setGraphic(heart);
      addToFavorite.getStyleClass().add(isFavorite ? "on" : "off");
      addToFavorite.getStyleClass().removeAll(isFavorite ? "off" : "on");
    } else {
      AlertDialog.showAlert("error", "Failed to add to favorite",
          "An error occurred while adding the book to favorite", null);
    }
  }

  private VBox createCommentComponent(ReturnUserComment comment) {
    String user = comment.getUserDisplayName().isEmpty()
        ? comment.getUserEmail()
        : comment.getUserDisplayName();
    String photoUrl = comment.getUserPhotoUrl();
    String content = comment.getContent();

    VBox commentBox = new VBox(10);
    commentBox.getStyleClass().add("comment-box");
    ImageView userAvatar = new ImageView();
    userAvatar.setFitHeight(30);
    userAvatar.setFitWidth(30);
    userAvatar.setSmooth(true);
    userAvatar.setPickOnBounds(true);
    userAvatar.setPreserveRatio(true);
    userAvatar.getStyleClass().add("comment-avatar");

    Circle clip = new Circle(15);
    clip.setCenterX(15);
    clip.setCenterY(15);
    userAvatar.setClip(clip);
    if (photoUrl != null && !photoUrl.isEmpty()) {
      userAvatar.setImage(new Image(photoUrl));
    } else {
      userAvatar.setImage(new Image(new AvatarUtil().setRounded(true).getAvatarUrl(user)));
    }

    Label userLabel = new Label(user);
    userLabel.getStyleClass().add("comment-user");
    Label contentLabel = new Label(content);
    contentLabel.getStyleClass().add("comment-content");
    HBox commentHeader = new HBox(10, userAvatar, userLabel);
    commentHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    commentBox.getChildren().addAll(
        commentHeader,
        contentLabel
    );
    return commentBox;
  }

  private void handleAddComment() {
    String content = newCommentTextArea.getText().trim();
    if (content.isEmpty()) {
      AlertDialog.showAlert("error", "Empty Comment", "Comment cannot be empty", null);
      return;
    }
    Comment newComment = new Comment(
        AuthController.getInstance().getCurrentUser().getUid(),
        book.getId(),
        content
    );
    try {
      Document result = CommentController.addComment(newComment);
      if (result != null) {
        AlertDialog.showAlert("success", "Comment Added", "Your comment has been added", null);
        newCommentTextArea.clear();

        ReturnUserComment userComment = new ReturnUserComment(
            AuthController.getInstance().getCurrentUser().getDisplayName(),
            AuthController.getInstance().getCurrentUser().getEmail(),
            AuthController.getInstance().getCurrentUser().getPhotoUrl(),
            content
        );
        if (!(commentList instanceof ArrayList)) {
          commentList = new ArrayList<>(commentList);
        }
        commentList.add(userComment);
        commentsContainer.getItems().add(userComment);

        commentsContainer.scrollTo(commentsContainer.getItems().size() - 1);
        commentsContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
        if (!commentsContainer.isVisible()) {
          commentsContainer.setVisible(true);
          commentsContainer.setManaged(true);
          emptyComments.setVisible(false);
        }
      } else {
        AlertDialog.showAlert("error", "Failed to Add Comment",
            "An error occurred while adding your comment", null);
      }
    } catch (Exception e) {
      e.printStackTrace();
      AlertDialog.showAlert("error", "Failed to Add Comment",
          "An error occurred while adding your comment", null);
    }
  }
}
