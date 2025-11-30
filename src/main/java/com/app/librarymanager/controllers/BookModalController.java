package com.app.librarymanager.controllers;

import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.utils.AlertDialog;
import com.app.librarymanager.utils.DataValidation;
import com.app.librarymanager.utils.DatePickerUtil;
import com.app.librarymanager.utils.DateUtil;
import com.app.librarymanager.utils.DateUtil.DateFormat;
import com.app.librarymanager.utils.UploadFileUtil;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Popup;
import javafx.stage.Stage;
import com.app.librarymanager.models.Book;
import javax.imageio.ImageIO;
import lombok.Setter;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;

public class BookModalController extends ControllerWithLoader {

  @FunctionalInterface
  public interface SaveCallback {

    void onSave(Book book);
  }

  @FXML
  private TextField _idField;
  @FXML
  private TextField idField;
  @FXML
  private TextField iSBNField;
  @FXML
  private TextField titleField;
  @FXML
  private TextField publisherField;
  @FXML
  private TextArea descriptionField;
  @FXML
  private TextField pageCountField;
  @FXML
  private TextField categoriesField;
  @FXML
  private TextField authorsField;
  @FXML
  private TextField thumbnailField;
  @FXML
  private TextField languageField;
  @FXML
  private TextField priceField;
  @FXML
  private TextField currencyCodeField;
  @FXML
  private TextField pdfLinkField;
  @FXML
  private DatePicker publishedDateField;
  @FXML
  private TextField discountPriceField;
  @FXML
  private TextField numberOfCopies;
  @FXML
  private CheckBox isActiveCheckBox;
  @FXML
  private ImageView thumbnailPreview;
  @FXML
  private Button generateIdButton;
  @FXML
  private HBox searchGoogleBooksContainer;
  @FXML
  private TextField searchGoogleBooksField;

  private Book book;
  @Setter
  private SaveCallback saveCallback;
  private Popup searchResultsPopup = new Popup();

  private boolean isEditMode = false;

  private List<Book> searchResults = new ArrayList<>();

  @FXML
  private void initialize() {
    publishedDateField.getEditor().setOnMouseClicked(event -> {
      publishedDateField.show();
    });
    DatePickerUtil.disableFutureDates(publishedDateField);
    DatePickerUtil.setDatePickerFormat(publishedDateField);
    DatePickerUtil.disableEditor(publishedDateField);
    initNumberField(pageCountField);
    initNumberField(priceField);
    initNumberField(discountPriceField);
    initNumberField(numberOfCopies);
    Bounds boundsInScreen = searchGoogleBooksField.localToScreen(
        searchGoogleBooksField.getBoundsInLocal());
    if (boundsInScreen != null) {
      searchResultsPopup.setOnShown(event -> {
        double popupX = boundsInScreen.getMinX();
        double popupY = boundsInScreen.getMaxY();
        searchResultsPopup.setX(popupX - 10);
        searchResultsPopup.setY(popupY);
      });
    }
    searchGoogleBooksField.setOnMouseClicked(event -> {
      if (!searchResults.isEmpty()) {
        searchResultsPopup.show(searchGoogleBooksContainer.getScene().getWindow());
      }
    });
    searchResultsPopup.setAutoHide(true);
    searchGoogleBooksField.setOnAction(event -> searchGoogleBooks());
  }

  public void setBook(Book book) {
    this.book = book;
    if (book != null) {
      Task<Void> task = new Task<Void>() {
        @Override
        protected Void call() {
          if (book.get_id() != null) {
            isEditMode = true;
            _idField.setText(book.get_id().toString());
            iSBNField.setDisable(true);
          }
          _idField.setDisable(true);
          idField.setText(book.getId());
          idField.setDisable(true);
          iSBNField.setText(book.getISBN());
          titleField.setText(book.getTitle());
          publisherField.setText(book.getPublisher());
          descriptionField.setText(book.getDescription());
          pageCountField.setText(String.valueOf(book.getPageCount()));
          categoriesField.setText(
              book.getCategories().toString().replace("[", "").replace("]", ""));
          authorsField.setText(book.getAuthors().toString().replace("[", "").replace("]", ""));
          thumbnailField.setText(book.getThumbnail());
          languageField.setText(book.getLanguage());
          priceField.setText(new DecimalFormat("#.##").format(book.getPrice()));
          currencyCodeField.setText(book.getCurrencyCode());
          currencyCodeField.setText(book.getCurrencyCode());
          pdfLinkField.setText(book.getPdfLink());
          publishedDateField.setValue(DateUtil.parse(book.getPublishedDate()));
          discountPriceField.setText(new DecimalFormat("#.##").format(book.getDiscountPrice()));
          isActiveCheckBox.setSelected(book.isActivated());
          thumbnailPreview.setImage(
              book.getThumbnail() != null && !book.getThumbnail().isEmpty() ? new Image(
                  (book.getThumbnail())) : null);
          generateIdButton.setDisable(true);
          searchGoogleBooksContainer.setVisible(!searchGoogleBooksField.getText().isEmpty());
          return null;
        }
      };

      task.setOnSucceeded(e -> {
        Platform.runLater(() -> {
          showLoading(false);
          loadBookCopies(book.getId());
        });
      });

      new Thread(task).start();
    } else {
      isEditMode = false;
    }
  }

  private void loadBookCopies(String bookId) {
    Task<Document> task = new Task<Document>() {
      @Override
      protected Document call() throws Exception {
        return BookCopiesController.findCopy(new BookCopies(bookId));
      }
    };

//    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      Document copies = task.getValue();
      if (copies != null) {
        BookCopies bookCopies = new BookCopies(copies);
        numberOfCopies.setText(String.valueOf(bookCopies.getCopies()));
      } else {
        numberOfCopies.setText("0");
      }
    });
    task.setOnFailed(e -> {
      AlertDialog.showAlert("error", "Error", e.getSource().getException().getMessage(), null);
    });

    new Thread(task).start();
  }

  @FXML
  void onSubmit() {
    try {
      if (!iSBNField.getText().equals("N/A") && !DataValidation.validISBN(iSBNField.getText())) {
        throw new Exception("ISBN " + iSBNField.getText() + " is not valid.");
      }
      String parsePageCount = DataValidation.checkInt("Page Count", pageCountField.getText());
      if (!parsePageCount.isEmpty()) {
        throw new Exception(parsePageCount);
      }
      String parsePrice = DataValidation.checkDouble("Price", priceField.getText());
      if (!parsePrice.isEmpty()) {
        throw new Exception(parsePrice);
      }
      String parseDiscountPrice = DataValidation.checkDouble("Discount Price",
          discountPriceField.getText());
      if (!parseDiscountPrice.isEmpty()) {
        throw new Exception(parseDiscountPrice);
      }

      if (Double.parseDouble(discountPriceField.getText()) > Double.parseDouble(
          priceField.getText())) {
        throw new Exception("Discount Price must be less or equal than Price.");
      }

      if (book == null) {
        book = new Book();
      } else {
        if (!_idField.getText().isEmpty()) {
          book.set_id(new ObjectId(_idField.getText()));
        }
      }
      book.setId(idField.getText());
      book.setISBN(iSBNField.getText());
      book.setTitle(titleField.getText());
      book.setPublisher(publisherField.getText());
      book.setDescription(descriptionField.getText());
      book.setPageCount(Integer.parseInt(pageCountField.getText()));
      book.setCategories(new ArrayList<>(
          Arrays.stream(categoriesField.getText().split(",")).map(String::trim).toList()));
      book.setAuthors(new ArrayList<>(
          Arrays.stream(authorsField.getText().split(",")).map(String::trim).toList()));
      book.setThumbnail(thumbnailField.getText());
      book.setLanguage(languageField.getText());
      book.setPrice(Double.parseDouble(priceField.getText()));
      book.setCurrencyCode(currencyCodeField.getText());
      book.setPdfLink(pdfLinkField.getText());
      book.setPublishedDate(
          DateUtil.format(publishedDateField.getValue(), DateUtil.DateFormat.YYYY_MM_DD));
      book.setDiscountPrice(Double.parseDouble(discountPriceField.getText()));
      book.setActivated(isActiveCheckBox.isSelected());

      int copies = Integer.parseInt(numberOfCopies.getText());
      if (copies >= 0) {
        BookCopies bookCopies = new BookCopies(book.getId(), copies);
        Task<Document> updateCopiesTask = new Task<Document>() {
          @Override
          protected Document call() throws Exception {
            BookCopies newCopies = new BookCopies(book.getId(), copies);
            Document document = BookCopiesController.addCopy(newCopies);
            return BookCopiesController.editCopy(newCopies);
          }
        };
        new Thread(updateCopiesTask).start();
      } else {
        AlertDialog.showAlert("error", "Error", "Book copies must greater than 0", null);
      }
    } catch (Exception e) {
      AlertDialog.showAlert("error", "Error", e.getMessage(), null);
      return;
    }

    Task<Document> task = new Task<Document>() {
      @Override
      protected Document call() throws Exception {
        return isEditMode ? BookController.editBook(book) : BookController.addBook(book);
      }
    };

    setLoadingText(isEditMode ? "Updating book..." : "Adding book...");

    task.setOnRunning(e -> showLoading(true));

    task.setOnSucceeded(e -> {
      showLoading(false);
      Document resp = task.getValue();
      Stage stage = (Stage) idField.getScene().getWindow();
      if (resp == null) {
        AlertDialog.showAlert("error", "Error",
            "Book with id = " + idField.getText() + " or ISBN = " + iSBNField.getText()
                + " is already existed.", null);
      } else {
        if (resp.getObjectId("_id") != null) {
          AlertDialog.showAlert("success", "Success",
              isEditMode ? "Book updated successfully." : "Book added successfully.", null);
          stage.close();
          book.set_id(resp.getObjectId("_id"));
          if (saveCallback != null) {
            saveCallback.onSave(book);
          }
        } else {
          AlertDialog.showAlert("error", "Error", "An error occurred while saving the book.", null);
        }
      }
    });

    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", e.getSource().getException().getMessage(), null);
    });

    new Thread(task).start();
  }

  private void initNumberField(TextField field) {
    field.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
          String newValue) {
        if (!newValue.matches("\\d*(\\.\\d*)?")) {
          field.setText(oldValue);
        }
      }
    });
  }

  @FXML
  private void syncBookByISBN() {
    String iSBN = iSBNField.getText();
    if (iSBN.isEmpty()) {
      AlertDialog.showAlert("error", "Error", "ISBN field is empty.", null);
      return;
    }
    Task<Book> task = new Task<Book>() {
      @Override
      protected Book call() {
        return BookController.searchByISBN(iSBN);
      }
    };

    setLoadingText("Searching book by ISBN...");

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      Book book = task.getValue();
      if (book == null) {
        AlertDialog.showAlert("error", "Error", "Book not found.", null);
      } else {
        setBook(book);
      }
    });
    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", e.getSource().getException().getMessage(), null);
    });

    new Thread(task).start();
  }

  @FXML
  private void searchGoogleBooks() {
    String keyword = searchGoogleBooksField.getText().trim();
    if (keyword.isEmpty()) {
      AlertDialog.showAlert("error", "Error", "Search field is empty.", null);
      return;
    }

    Task<List<Book>> task = new Task<List<Book>>() {
      @Override
      protected List<Book> call() {
        return BookController.searchByKeyword(keyword, 0, 5);
      }
    };

    setLoadingText("Searching books from Google Books...");

    task.setOnRunning(e -> showLoading(true));
    task.setOnSucceeded(e -> {
      showLoading(false);
      List<Book> newResults = task.getValue();
      if (newResults.isEmpty()) {
        AlertDialog.showAlert("error", "Error", "No books found.", null);
      } else {
        searchResults.clear();
        searchResults.addAll(newResults);
        VBox vbox = new VBox(5);
        vbox.getStyleClass().add("popup-list-view");

        AtomicReference<Book> selectedBook = new AtomicReference<>();
        vbox.setOnMouseClicked(event -> {
          Node clickedNode = event.getPickResult().getIntersectedNode();
          while (clickedNode != null && !(clickedNode instanceof HBox)) {
            clickedNode = clickedNode.getParent();
          }
          if (clickedNode != null) {
            int selectedIndex = vbox.getChildren().indexOf(clickedNode);
            if (selectedIndex >= 0) {
              selectedBook.set(searchResults.get(selectedIndex));
              searchResultsPopup.hide();
            }
          }
        });
        for (Book book : newResults) {
          HBox hBox = new HBox(5);
          hBox.getStyleClass().add("popup-list-item");
          ImageView imageView = new ImageView(book.getThumbnail());
          imageView.setPreserveRatio(true);
          imageView.setFitHeight(60);
          hBox.getChildren().add(imageView);
          VBox vBox = new VBox(5);
          Label titleLabel = new Label(book.getTitle());
          titleLabel.setWrapText(true);
          vBox.getChildren().add(titleLabel);
          Label authorsLabel = new Label(book.getAuthors().toString().replaceAll("[\\[\\]]", ""));
          authorsLabel.setWrapText(true);
          vBox.getChildren().add(authorsLabel);
          hBox.getChildren().add(vBox);
          vbox.getChildren().add(hBox);
        }

        searchResultsPopup.getContent().clear();
        searchResultsPopup.getContent().add(vbox);

        searchResultsPopup.show(searchGoogleBooksContainer.getScene().getWindow());

        searchResultsPopup.setOnHidden(event -> {
          if (selectedBook.get() != null) {
            setBook(selectedBook.get());
          }
        });
      }
    });
    task.setOnFailed(e -> {
      showLoading(false);
      AlertDialog.showAlert("error", "Error", e.getSource().getException().getMessage(), null);
    });

    new Thread(task).start();
  }

  @FXML
  private void handleUploadThumbnail() {
    handleUploadFile(thumbnailField, thumbnailPreview, "Image Files", "*.jpg", "*.png", "*.jpeg");
  }

  @FXML
  private void handleUploadPdf() {
    handleUploadFile(pdfLinkField, null, "PDF File", "*.pdf");
  }

  private void handleUploadFile(TextField field, ImageView imgPreview, String title,
      String... type) {
    FileChooser fileChooser = new FileChooser();
    fileChooser.getExtensionFilters().add(new ExtensionFilter(title, type));
    File file = fileChooser.showOpenDialog(idField.getScene().getWindow());
    if (file != null) {
      field.setText(file.getAbsolutePath());
      Task<JSONObject> uploadTask = new Task<JSONObject>() {
        @Override
        protected JSONObject call() {
          return UploadFileUtil.uploadFile(file.getAbsolutePath(), file.getName());
        }
      };

      uploadTask.setOnRunning(e -> field.setText("Uploading..."));
      uploadTask.setOnSucceeded(e -> {
        JSONObject resp = uploadTask.getValue();
        if (resp.getBoolean("success")) {
//          AlertDialog.showAlert("success", "Success",
//              "File " + file.getName() + " uploaded successfully.", null);
          String fileLink = resp.getString("longURL");
          field.setText(fileLink);
          if (imgPreview != null) {
            try {
              BufferedImage image = ImageIO.read(new File(file.getAbsolutePath()));
              ByteArrayOutputStream baos = new ByteArrayOutputStream();
              ImageIO.write(image, file.getName().split("\\.")[1], baos);
              byte[] imageBytes = baos.toByteArray();
              imgPreview.setImage(new Image(new ByteArrayInputStream(imageBytes)));
            } catch (IOException ex) {
              ex.printStackTrace();
            }
          }
        } else {
          AlertDialog.showAlert("error", "Error", resp.getString("message"), null);
        }
      });
      uploadTask.setOnFailed(e -> {
        field.setText("");
        AlertDialog.showAlert("error", "Error", e.getSource().getException().getMessage(), null);
      });
      new Thread(uploadTask).start();
    } else {
//      AlertDialog.showAlert("error", "Error", "No file selected.", null);
    }
  }

  @FXML
  private void generateId() {
    String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    StringBuilder salt = new StringBuilder();
    Random rnd = new Random();
    while (salt.length() < 12) {
      int index = (int) (rnd.nextFloat() * CHARS.length());
      salt.append(CHARS.charAt(index));
    }
    idField.setText(salt.toString());
  }

  @FXML
  private void onCancel() {
    Stage stage = (Stage) idField.getScene().getWindow();
    stage.close();
  }
}