package com.app.librarymanager.controllers;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;

import com.app.librarymanager.models.Book;
import com.app.librarymanager.models.BookCopies;
import com.app.librarymanager.models.BookLoan;
import com.app.librarymanager.models.BookLoan.Mode;
import com.app.librarymanager.models.User;
import com.app.librarymanager.services.Firebase;
import com.app.librarymanager.services.MongoDB;

import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import com.app.librarymanager.utils.DateUtil;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Data;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import com.app.librarymanager.models.ReturnRecord;
import com.app.librarymanager.models.Penalty;

public class BookLoanController {

  private static final int MAX_BORROW_LIMIT = 5;
  private static final int DEFAULT_LOAN_DAYS = 30;

  public static Map<String, Object> bookLoanToMap(BookLoan bookLoan) {
    Map<String, Object> map = new HashMap<>();
    map.put("userId", bookLoan.getUserId());
    map.put("bookId", bookLoan.getBookId());
    map.put("borrowDate", bookLoan.getBorrowDate());
    map.put("dueDate", bookLoan.getDueDate());
    map.put("valid", bookLoan.isValid());
    map.put("type", bookLoan.getType() != null ? bookLoan.getType().name() : null);
    map.put("numCopies", bookLoan.getNumCopies());
    map.put("status", bookLoan.getStatus() == null ? BookLoan.Status.PENDING.toString() : bookLoan.getStatus().toString());
    map.put("requestDate", bookLoan.getRequestDate());
    map.put("processedBy", bookLoan.getProcessedBy());
    map.put("processedAt", bookLoan.getProcessedAt());
    // Borrow-time condition details
    map.put("borrowCondition", bookLoan.getBorrowCondition());
    map.put("borrowConditionNotes", bookLoan.getBorrowConditionNotes());
    // Legacy conditionNotes kept for backward compatibility
    map.put("conditionNotes", bookLoan.getConditionNotes());
    // Return-time condition details
    map.put("returnCondition", bookLoan.getReturnCondition());
    map.put("returnConditionNotes", bookLoan.getReturnConditionNotes());
    map.put("returnedBy", bookLoan.getReturnedBy());
    map.put("returnedAt", bookLoan.getReturnedAt());
    return map;
  }

  private static Document findOnlineLoan(BookLoan bookLoan) {
    return MongoDB.getInstance().findAnObject("bookLoan", new HashMap<>(
        Map.of("userId", bookLoan.getUserId(), "bookId", bookLoan.getBookId(), "type", "ONLINE")));
  }

  private static Document addOfflineLoan(BookLoan bookLoan) {
    BookCopiesController.increaseCopy(
        new BookCopies(bookLoan.getBookId(), -bookLoan.getNumCopies()));
    return MongoDB.getInstance().addToCollection("bookLoan", bookLoanToMap(bookLoan));
  }

  private static Document addOnlineLoan(BookLoan bookLoan) {
    Document currentLoan = findOnlineLoan(bookLoan);
    ObjectId idInDatabase = null;
    if (currentLoan != null) {
      idInDatabase = currentLoan.getObjectId("_id");
    }
    MongoDB database = MongoDB.getInstance();
    if (idInDatabase != null) {
      return database.updateData("bookLoan", "_id", idInDatabase, bookLoanToMap(bookLoan));
    }
    return database.addToCollection("bookLoan", bookLoanToMap(bookLoan));
  }

  public static Document addLoan(BookLoan bookLoan) {
    return bookLoan.getType() == Mode.OFFLINE ? addOfflineLoan(bookLoan) : addOnlineLoan(bookLoan);
  }

  /**
   * Create a loan request (pending) instead of immediately lending.
   */
  public static Document createLoanRequest(BookLoan bookLoan) {
    try {
      System.err.println("BookLoanController.createLoanRequest: user=" + bookLoan.getUserId() + ", bookId=" + bookLoan.getBookId() + ", type=" + bookLoan.getType() + ", numCopies=" + bookLoan.getNumCopies());
      if (bookLoan.getUserId() == null || bookLoan.getUserId().isBlank()) {
        System.err.println("BookLoanController.createLoanRequest: invalid userId provided: '" + bookLoan.getUserId() + "'");
        return null;
      }
      // check borrow limit
      int requested = bookLoan.getType() == Mode.OFFLINE ? bookLoan.getNumCopies() : 1;
      long current = countBorrowedCopiesOfUser(bookLoan.getUserId());
      System.err.println("BookLoanController.createLoanRequest: currentBorrowed=" + current + ", requested=" + requested + ", max=" + MAX_BORROW_LIMIT);
      if (current + requested > MAX_BORROW_LIMIT) {
        System.err.println("BookLoanController.createLoanRequest: borrow limit exceeded for user " + bookLoan.getUserId()
            + " current=" + current + " requested=" + requested + " max=" + MAX_BORROW_LIMIT);
        return null;
      }
      // prepare document
      Map<String, Object> map = bookLoanToMap(bookLoan);
      map.put("status", BookLoan.Status.PENDING.toString());
      map.put("requestDate", new Date());
      map.put("valid", false);
      Document doc = MongoDB.getInstance().addToCollection("bookLoan", map);
      if (doc == null) {
        System.err.println("BookLoanController.createLoanRequest: MongoDB.addToCollection returned null for user=" + bookLoan.getUserId());
      } else {
        System.err.println("BookLoanController.createLoanRequest: created loan request _id=" + doc.getObjectId("_id") + " for user=" + bookLoan.getUserId());
      }
      return doc;
    } catch (Exception e) {
      System.err.println("BookLoanController.createLoanRequest: exception when creating loan request: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public static long countBorrowedCopiesOfUser(String userId) {
    try {
      if (userId == null || userId.isBlank()) {
        System.err.println("countBorrowedCopiesOfUser: invalid userId provided: '" + userId + "'");
        return 0;
      }
      List<Document> docs = MongoDB.getInstance().findAllObject("bookLoan",
          Filters.and(Filters.eq("userId", userId), Filters.or(Filters.eq("status", "PENDING"), Filters.eq("status", "AVAILABLE"))));
      if (docs == null) {
        System.err.println("countBorrowedCopiesOfUser: found no loan documents for user=" + userId);
        return 0;
      }
      long sum = 0;
      System.err.println("countBorrowedCopiesOfUser: found " + docs.size() + " loan doc(s) for user=" + userId);
      for (Document d : docs) {
        String id = d.getObjectId("_id") != null ? d.getObjectId("_id").toHexString() : "<no-id>";
        String type = d.getString("type");
        int num = d.getInteger("numCopies", 0);
        System.err.println("  loan doc: _id=" + id + ", type=" + type + ", numCopies=" + num + ", status=" + d.getString("status"));
        if ("OFFLINE".equalsIgnoreCase(type)) {
          sum += num;
        } else {
          sum += 1;
        }
      }
      System.err.println("countBorrowedCopiesOfUser: total computed for user=" + userId + " -> " + sum);
      return sum;
    } catch (Exception e) {
      return 0;
    }
  }

  public static int getMaxBorrowLimit() {
    return MAX_BORROW_LIMIT;
  }

  public static List<ReturnBookLoan> getPendingRequests(int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance().findAllObject("bookLoan", Filters.eq("status", "PENDING"), start, length));
  }

  public static long countPendingRequests() {
    return MongoDB.getInstance().countDocuments("bookLoan", Filters.eq("status", "PENDING"));
  }

  public static Document approveRequest(ObjectId id, String processedBy, String borrowCondition, String conditionNotes) {
    try {
      Document doc = MongoDB.getInstance().findAnObject("bookLoan", Filters.eq("_id", id));
      if (doc == null) return null;
      BookLoan request = new BookLoan(doc);
      // check availability for OFFLINE
      if (request.getType() == Mode.OFFLINE) {
        Document cp = BookCopiesController.findCopy(new BookCopies(request.getBookId()));
        int available = cp == null ? 0 : cp.getInteger("copies", 0);
        if (available < request.getNumCopies()) {
          return null;
        }
        // decrease copies
        BookCopiesController.decreaseCopy(new BookCopies(request.getBookId(), request.getNumCopies()));
      }
      Date now = new Date();
      Date dueDate = DateUtil.addDays(now, DEFAULT_LOAN_DAYS);
      Map<String, Object> update = new HashMap<>();
      update.put("valid", true);
      update.put("status", BookLoan.Status.AVAILABLE.toString());
      update.put("borrowDate", now);
      update.put("dueDate", dueDate);
      update.put("processedBy", processedBy);
      update.put("processedAt", now);
      // store borrow-time condition and notes; mark as available for return processing
      update.put("borrowCondition", borrowCondition != null ? borrowCondition : "NORMAL");
      update.put("borrowConditionNotes", conditionNotes);
      update.put("returnRequested", true);
      update.put("conditionNotes", conditionNotes);
      update.put("lastUpdated", new Timestamp(System.currentTimeMillis()));

      return MongoDB.getInstance().updateData("bookLoan", "_id", id, update);
    } catch (Exception e) {
      return null;
    }
  }

  public static Document rejectRequest(ObjectId id, String processedBy, String conditionNotes) {
    try {
      Date now = new Date();
      Map<String, Object> update = new HashMap<>();
      update.put("valid", false);
      update.put("status", BookLoan.Status.REJECTED.toString());
      update.put("processedBy", processedBy);
      update.put("processedAt", now);
      update.put("conditionNotes", conditionNotes);
      update.put("lastUpdated", new Timestamp(System.currentTimeMillis()));
      return MongoDB.getInstance().updateData("bookLoan", "_id", id, update);
    } catch (Exception e) {
      return null;
    }
  }

  public static Document editLoan(BookLoan bookLoan) {
    return MongoDB.getInstance()
        .updateData("bookLoan", "_id", bookLoan.get_id(), bookLoanToMap(bookLoan));
  }

  public static boolean removeAllLoan(String bookId) {
    return MongoDB.getInstance().deleteAll("bookLoan", Filters.eq("bookId", bookId));
  }

  public static boolean removeAllLoanOf(String userId) {
    return MongoDB.getInstance().deleteAll("bookLoan", Filters.eq("userId", userId));
  }

  public static boolean returnAllBookOf(String userId) {
    // Just use once when deleting users, so I hardcoded it too
    // After this, userId's loan will be deleted, so I don't handle it here
    try {
      List<Map<String, Object>> updates = MongoDB.getInstance().findAllObject("bookLoan",
              Filters.and(Filters.eq("valid", true), Filters.eq("userId", userId))).stream()
          .map(doc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bookId", doc.getString("bookId"));
            map.put("copies", doc.getInteger("numCopies"));
            return map;
          }).toList();
      List<UpdateOneModel<Document>> bulkOperations = updates.stream().map(update -> {
        String id = (String) update.get("bookId");
        int copies = (int) update.get("copies");
        return new UpdateOneModel<Document>(new Document("bookId", id),
            Updates.inc("copies", copies));
      }).toList();
      MongoDB.getInstance().getDatabase().getCollection("bookCopies")
          .bulkWrite(bulkOperations, new BulkWriteOptions().ordered(false));
      return true;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return false;
    }
  }

  public static Document returnBook(BookLoan bookLoan) {
    if (bookLoan.getType() == Mode.OFFLINE) {
      BookCopiesController.increaseCopy(
          new BookCopies(bookLoan.getBookId(), bookLoan.getNumCopies()));
    }
    return MongoDB.getInstance()
        .updateData("bookLoan", "_id", bookLoan.get_id(), new HashMap<>(Map.of("valid", false)));
  }

  @Data
  public static class ReturnBookLoan {

    private BookLoan bookLoan;
    private String titleBook;
    private String thumbnailBook;

    public ReturnBookLoan(BookLoan bookLoan, String titleBook, String thumbnailBook) {
      this.bookLoan = bookLoan;
      this.thumbnailBook = thumbnailBook;
      this.titleBook = titleBook;
    }

    public BookLoan getBookLoan() { return this.bookLoan; }
    public String getTitleBook() { return this.titleBook; }
    public String getThumbnailBook() { return this.thumbnailBook; }
    public void setBookLoan(BookLoan bookLoan) { this.bookLoan = bookLoan; }
  }

  private static List<ReturnBookLoan> bookLoanFromDocument(List<Document> documents) {
    try {
      Map<String, Document> bookDocs = BookController.findBookByListID(
              documents.stream().map(doc -> doc.getString("bookId")).toList()).stream()
          .collect(Collectors.toMap(doc -> doc.getString("id"), doc -> doc));
      return documents.stream().map(doc -> {
        Document bookDoc = bookDocs.get(doc.getString("bookId"));
        String title = "N/A";
        String thumbnail = null;
        if (bookDoc != null) {
          try {
            title = bookDoc.getString("title") != null ? bookDoc.getString("title") : "N/A";
            thumbnail = bookDoc.getString("thumbnail");
          } catch (Exception e) {
            // ignore and use defaults
            System.err.println("bookLoanFromDocument: failed to read book metadata for bookId=" + doc.getString("bookId") + ": " + e.getMessage());
          }
        } else {
          System.err.println("bookLoanFromDocument: missing book metadata for bookId=" + doc.getString("bookId"));
        }
        return new ReturnBookLoan(new BookLoan(doc), title, thumbnail);
      }).toList();
    } catch (Exception e) {
      System.err.println("bookLoanFromDocument: unexpected error: " + e.getMessage());
      e.printStackTrace();
      return List.of();
    }
  }

  @Data
  public static class BookLoanUser {

    private final ObjectProperty<User> user;
    private final ObjectProperty<BookLoan> bookLoan;
    private final ObjectProperty<Book> book;
//
//    User user;
//    Book book;
//    BookLoan bookLoan;

    public BookLoanUser(User user, Book book, BookLoan bookLoan) {
      this.user = new SimpleObjectProperty<>(user);
      this.bookLoan = new SimpleObjectProperty<>(bookLoan);
      this.book = new SimpleObjectProperty<>(book);
    }

    public User getUser() {
      return user.get();
    }

    public ObjectProperty<User> userProperty() {
      return user;
    }

    public BookLoan getBookLoan() {
      return bookLoan.get();
    }

    public ObjectProperty<BookLoan> bookLoanProperty() {
      return bookLoan;
    }

    public Book getBook() {
      return book.get();
    }

    public ObjectProperty<Book> bookProperty() {
      return book;
    }

  }

  public static List<BookLoanUser> getAllLentBook(int start, int length) {
    try {
      List<Document> bookLoanDocs = MongoDB.getInstance()
          .findAllObject("bookLoan", Filters.empty(), start, length);
      Map<String, User> relatedUser = new HashMap<>();
      try {
        List<String> uids = bookLoanDocs.stream().map(doc -> doc.getString("userId")).toList();
        List<User> users = UserController.listUsers(uids);
        if (users != null) {
          relatedUser = users.stream().collect(Collectors.toMap(User::getUid, user -> user, (existing, replacement) -> existing));
        }
      } catch (Throwable t) {
        System.err.println("BookLoanController.getAllLentBook: UserController.listUsers threw (" + t.getClass().getName() + "): " + t);
        t.printStackTrace();
        relatedUser = new HashMap<>();
      }
        Map<String, Book> relatedBook = BookController.listDocsToListBook(
            BookController.findBookByListID(
              bookLoanDocs.stream().map(doc -> doc.getString("bookId")).toList())).stream()
          .collect(Collectors.toMap(Book::getId, book -> book));
        final Map<String, User> relatedUserFinal = relatedUser;
        final Map<String, Book> relatedBookFinal = relatedBook;
        return bookLoanDocs.stream().map(
          doc -> new BookLoanUser(relatedUserFinal.get(doc.getString("userId")),
            relatedBookFinal.get(doc.getString("bookId")), new BookLoan(doc))).toList();
    } catch (Exception e) {
      //  System.out.println(e.getMessage());
      return null;
    }
  }

  public static List<ReturnBookLoan> getAllLentBookOf(String userId, int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findAllObject("bookLoan", Filters.and(eq("userId", userId)), start, length));
  }

  public static long countAllLoansOfUser(String userId) {
    return MongoDB.getInstance().countDocuments("bookLoan", Filters.eq("userId", userId));
  }

  public static long countLentBookOf(String userId) {
    return MongoDB.getInstance()
        .countDocuments("bookLoan", Filters.and(eq("userId", userId), eq("valid", true)));
  }

  public static List<ReturnBookLoan> getRecentLoan(int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findSortedObject("bookLoan", Filters.eq("valid", true),
            Sorts.orderBy(Sorts.descending("lastUpdated")), start, length));
  }

  /**
   * Get accepted (lent) loans - status AVAILABLE and valid == true
   */
  public static List<ReturnBookLoan> getAcceptedLoans(int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findAllObject("bookLoan", Filters.and(Filters.eq("status", "AVAILABLE"), Filters.eq("valid", true)), start, length));
  }

  /**
   * User (or system) requests a return for a loan. Marks returnRequested flag.
   */
  public static Document requestReturn(ObjectId id) {
    try {
      Map<String, Object> update = new HashMap<>();
      update.put("returnRequested", true);
      update.put("returnRequestedAt", new Date());
      update.put("lastUpdated", new Timestamp(System.currentTimeMillis()));
      return MongoDB.getInstance().updateData("bookLoan", "_id", id, update);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Process return by librarian. Creates a ReturnRecord, optionally a Penalty, updates copies and loan.
   */
  public static Document processReturn(ObjectId loanId, String processedBy, String condition, String conditionNotes, Double penaltyAmount) {
    try {
      Document doc = MongoDB.getInstance().findAnObject("bookLoan", Filters.eq("_id", loanId));
      if (doc == null) return null;
      BookLoan loan = new BookLoan(doc);
      Date now = new Date();
      // increase copies if offline
      if (loan.getType() == Mode.OFFLINE) {
        BookCopiesController.increaseCopy(new BookCopies(loan.getBookId(), loan.getNumCopies()));
      }
      Map<String, Object> update = new HashMap<>();
      update.put("valid", false);
      update.put("status", BookLoan.Status.RETURNED.toString());
      update.put("returnedBy", processedBy);
      update.put("returnedAt", now);
      update.put("returnCondition", condition);
      update.put("returnConditionNotes", conditionNotes);
      update.put("returnRequested", false);
      update.put("lastUpdated", new Timestamp(System.currentTimeMillis()));
      Document updatedLoan = MongoDB.getInstance().updateData("bookLoan", "_id", loanId, update);

      // create return record
      ReturnRecord rr = new ReturnRecord(loanId, processedBy, now, condition, conditionNotes, penaltyAmount);
      MongoDB.getInstance().addToCollection("returnRecord", rr.toDocument());

      // create penalty/invoice if needed
      if (penaltyAmount != null && penaltyAmount > 0) {
        // try to find user id and create penalty
        Penalty p = new Penalty(loan.getUserId(), "Return penalty for loan " + loanId.toHexString(), penaltyAmount);
        MongoDB.getInstance().addToCollection("penalty", p.toDocument());
      }
      return updatedLoan;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static List<ReturnBookLoan> getTopLentBook(int start, int length) {
    try {
      List<Document> documents = MongoDB.getInstance().getAggregate("bookLoan", List.of(
          new Document("$group",
              new Document("_id", "$bookId").append("count", new Document("$sum", 1))),
          new Document("$sort", new Document("count", -1)), new Document("$skip", start),
          new Document("$limit", length)));
      Map<String, Document> bookDocs = BookController.findBookByListID(
              documents.stream().map(doc -> doc.getString("_id")).toList()).stream()
          .collect(Collectors.toMap(doc -> doc.getString("id"), doc -> doc));
      return documents.stream().map(doc -> {
        Document bookDoc = bookDocs.get(doc.getString("_id"));
        return new ReturnBookLoan(
            new BookLoan("", doc.getString("_id"), new Date(), new Date(), doc.getInteger("count")),
            bookDoc.getString("title"), bookDoc.getString("thumbnail"));
      }).toList();
    } catch (Exception e) {
        System.out.println(e.getMessage());
      return null;
    }
  }

  private static Bson getFilter(boolean isValid, boolean isNotValid, boolean isOnline,
      boolean isOffline) {
    List<Bson> validConditions = new ArrayList<>();
    if (isValid) {
      validConditions.add(Filters.eq("valid", true));
    }
    if (isNotValid) {
      validConditions.add(Filters.eq("valid", false));
    }
    List<Bson> onlineConditions = new ArrayList<>();
    if (isOnline) {
      onlineConditions.add(Filters.eq("type", Mode.ONLINE.toString()));
    }
    if (isOffline) {
      onlineConditions.add(Filters.eq("type", Mode.OFFLINE.toString()));
    }
    Bson filters = Filters.empty();
    if (!validConditions.isEmpty()) {
      filters = Filters.and(filters, Filters.or(validConditions.toArray(Bson[]::new)));
    } else {
      return Filters.expr(false);
    }
    if (!onlineConditions.isEmpty()) {
      filters = Filters.and(filters, Filters.or(onlineConditions.toArray(Bson[]::new)));
    } else {
      return Filters.expr(false);
    }
    return filters;
  }

  public static List<ReturnBookLoan> getLoanWithFilter(boolean isValid, boolean isNotValid,
      boolean isOnline, boolean isOffline, int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance()
        .findAllObject("bookLoan", getFilter(isValid, isNotValid, isOnline, isOffline), start,
            length));
  }

  public static long countLoanWithFilter(boolean isValid, boolean isNotValid, boolean isOnline,
      boolean isOffline) {
    return MongoDB.getInstance()
        .countDocuments("bookLoan", getFilter(isValid, isNotValid, isOnline, isOffline));
  }

  public static List<ReturnBookLoan> getLoanWithFilterOfUser(String userId, boolean isValid,
      boolean isNotValid, boolean isOnline, boolean isOffline, int start, int length) {
    return bookLoanFromDocument(MongoDB.getInstance().findAllObject("bookLoan",
        Filters.and(Filters.eq("userId", userId),
            getFilter(isValid, isNotValid, isOnline, isOffline)), start, length));
  }

  public static long countLoanWithFilterOfUser(String userId, boolean isValid, boolean isNotValid,
      boolean isOnline, boolean isOffline) {
    return MongoDB.getInstance().countDocuments("bookLoan",
        Filters.and(Filters.eq("userId", userId),
            getFilter(isValid, isNotValid, isOnline, isOffline)));
  }

  public static long numberOfRecords() {
    return MongoDB.getInstance().countDocuments("bookLoan");
  }

  public static long countLentBook() {
    return MongoDB.getInstance().countDocuments("bookLoan", Filters.eq("valid", true));
  }

  public static boolean refreshDatabase() {
    try {
      Date curDate = new Date();
      Bson filterDate = Filters.and(lte("dueDate", curDate), eq("valid", true));
      Bson filterOffline = Filters.and(filterDate, eq("type", Mode.OFFLINE.name()));
      MongoDB.getInstance().findAllObject("bookLoan", filterOffline).forEach(
          e -> BookCopiesController.increaseCopy(
              new BookCopies(e.getString("bookId"), e.getInteger("numCopies"))));
      return MongoDB.getInstance().updateAll("bookLoan", filterDate,
          Updates.combine(Updates.set("valid", false),
              Updates.set("status", BookLoan.Status.EXPIRED.toString()),
              Updates.set("lastUpdated", new Timestamp(System.currentTimeMillis()))));
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Compute a display-friendly status for the loan based on stored fields.
   * Priority: PENDING -> RETURNED (had borrowDate and not valid) -> EXPIRED -> AVAILABLE -> fallback to stored status
   */
  public static String computeDisplayStatus(BookLoan loan) {
    if (loan == null) return "N/A";
    if (BookLoan.Status.PENDING.equals(loan.getStatus())) return "PENDING";
    if (loan.getBorrowDate() != null && !loan.isValid()) return "RETURNED";
    Date now = new Date();
    if (loan.getDueDate() != null && loan.isValid() && loan.getDueDate().before(now)) return "EXPIRED";
    if (loan.isValid()) return "AVAILABLE";
    return loan.getStatus() != null ? loan.getStatus().name() : "N/A";
  }
}
