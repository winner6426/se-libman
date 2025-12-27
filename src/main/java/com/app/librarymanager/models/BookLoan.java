package com.app.librarymanager.models;

import com.app.librarymanager.utils.DateUtil;
import java.time.LocalDate;
import java.util.Date;
import lombok.EqualsAndHashCode;
import org.bson.Document;
import org.bson.types.ObjectId;
import lombok.Data;

@EqualsAndHashCode(callSuper = true)
@Data
public class BookLoan extends BookUser {

  public enum Mode {
    OFFLINE,
    ONLINE
  }

  public enum Status {
    PENDING,
    AVAILABLE,
    REJECTED,
    EXPIRED
  }

  private Date borrowDate;
  private Date dueDate;
  private boolean valid;
  private Status status;
  private Mode type;
  private int numCopies; // = 0 iff online
  private java.util.Date requestDate;
  private String processedBy;
  private java.util.Date processedAt;
  private String conditionNotes;

  public BookLoan() {
    super();
    borrowDate = null;
    dueDate = null;
    valid = false;
    type = Mode.ONLINE;
    numCopies = 0;
    status = Status.PENDING;
  }

  public BookLoan(String userId, String bookId) {
    super(userId, bookId);
    this.borrowDate = null;
    this.dueDate = null;
    this.valid = false;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
    this.status = Status.PENDING;
  }

  public BookLoan(String userId, String bookId, Date borrowDate, Date dueDate) {
    super(userId, bookId);
    this.borrowDate = borrowDate;
    this.dueDate = dueDate;
    this.valid = true;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
    this.status = Status.AVAILABLE;
  }

  public BookLoan(String userId, String bookId, LocalDate borrowDate, LocalDate dueDate) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(borrowDate);
    this.dueDate = DateUtil.localDateToDate(dueDate);
    this.valid = true;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
  }

  /**
   * Constructor with date like "dd/mm/yyyy"
   *
   * @param userId     user id
   * @param bookId     book id
   * @param borrowDate borrow date
   * @param dueDate    excepted return date
   */
  public BookLoan(String userId, String bookId, String borrowDate, String dueDate) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(DateUtil.parse(borrowDate));
    this.dueDate = DateUtil.localDateToDate(DateUtil.parse(dueDate));
    this.valid = true;
    this.type = Mode.ONLINE;
    this.numCopies = 0;
  }

  public BookLoan(String userId, String bookId, Date borrowDate, Date dueDate, int numCopies) {
    super(userId, bookId);
    this.borrowDate = borrowDate;
    this.dueDate = dueDate;
    this.valid = true;
    this.type = Mode.OFFLINE;
    this.numCopies = numCopies;
    this.status = Status.AVAILABLE;
  }

  public BookLoan(String userId, String bookId, LocalDate borrowDate, LocalDate dueDate,
      int numCopies) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(borrowDate);
    this.dueDate = DateUtil.localDateToDate(dueDate);
    this.valid = true;
    this.type = Mode.OFFLINE;
    this.numCopies = numCopies;
  }

  /**
   * Constructor with date like "dd/mm/yyyy"
   *
   * @param userId     user id
   * @param bookId     book id
   * @param borrowDate borrow date
   * @param dueDate    excepted return date
   * @param numCopies  number of copies borrowed
   */
  public BookLoan(String userId, String bookId, String borrowDate, String dueDate, int numCopies) {
    super(userId, bookId);
    this.borrowDate = DateUtil.localDateToDate(DateUtil.parse(borrowDate));
    this.dueDate = DateUtil.localDateToDate(DateUtil.parse(dueDate));
    this.valid = true;
    this.numCopies = 0;
    this.type = Mode.OFFLINE;
    this.numCopies = numCopies;
  }

  public BookLoan(ObjectId id, String userId, String bookId, Date borrowDate, Date dueDate,
      boolean valid, Mode type, int numCopies) {
    super(id, userId, bookId);
    this.setLastUpdated(new Date());
    this.borrowDate = borrowDate;
    this.dueDate = dueDate;
    this.valid = valid;
    this.type = type;
    this.numCopies = numCopies;
    this.status = valid ? Status.AVAILABLE : Status.EXPIRED;
  }

  public Document toDocument() {
    Document document = super.toDocument();
    document.put("borrowDate", borrowDate);
    document.put("dueDate", dueDate);
    document.put("valid", valid);
    document.put("type", type.toString());
    document.put("numCopies", numCopies);
    document.put("status", status == null ? Status.PENDING.toString() : status.toString());
    document.put("requestDate", requestDate);
    document.put("processedBy", processedBy);
    document.put("processedAt", processedAt);
    document.put("conditionNotes", conditionNotes);
    return document;
  }

  public BookLoan(Document document) {
    super(document);
    this.borrowDate = document.getDate("borrowDate");
    this.dueDate = document.getDate("dueDate");
    this.valid = document.getBoolean("valid");
    this.type = document.getString("type").equals("OFFLINE") ? Mode.OFFLINE : Mode.ONLINE;
    this.numCopies = document.getInteger("numCopies");
    String statusStr = document.getString("status");
    try {
      this.status = statusStr == null ? Status.PENDING : Status.valueOf(statusStr);
    } catch (IllegalArgumentException e) {
      this.status = Status.PENDING;
    }
    this.requestDate = document.getDate("requestDate");
    this.processedBy = document.getString("processedBy");
    this.processedAt = document.getDate("processedAt");
    this.conditionNotes = document.getString("conditionNotes");
  }

  // Explicit getters for compilation safety
  public BookLoan.Status getStatus() {
    return this.status;
  }

  public java.util.Date getRequestDate() {
    return this.requestDate;
  }

  public String getProcessedBy() {
    return this.processedBy;
  }

  public java.util.Date getProcessedAt() {
    return this.processedAt;
  }

  public String getConditionNotes() {
    return this.conditionNotes;
  }

  // Additional explicit accessors
  public java.util.Date getBorrowDate() { return this.borrowDate; }
  public java.util.Date getDueDate() { return this.dueDate; }
  public boolean isValid() { return this.valid; }
  public Mode getType() { return this.type; }
  public int getNumCopies() { return this.numCopies; }

  public void setType(Mode type) { this.type = type; }
  public void setBorrowDate(java.util.Date borrowDate) { this.borrowDate = borrowDate; }
  public void setDueDate(java.util.Date dueDate) { this.dueDate = dueDate; }
  public void setNumCopies(int numCopies) { this.numCopies = numCopies; }
  public void setValid(boolean valid) { this.valid = valid; }
}
