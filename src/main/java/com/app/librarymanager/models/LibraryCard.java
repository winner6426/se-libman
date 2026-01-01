package com.app.librarymanager.models;

import java.util.Date;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

@Data
public class LibraryCard {

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED
  }

  private ObjectId _id;
  private String userId;
  private String userName;
  private String cardNumber;
  private Date registerDate;
  private Date expireDate;
  private Status status;

  public LibraryCard() {
    this._id = null;
    this.userId = null;
    this.userName = null;
    this.cardNumber = null;
    this.registerDate = null;
    this.expireDate = null;
    this.status = Status.PENDING;
  }

  public LibraryCard(String userId, String userName) {
    this._id = null;
    this.userId = userId;
    this.userName = userName;
    this.cardNumber = null;
    this.registerDate = new Date();
    this.expireDate = null;
    this.status = Status.PENDING;
  }

  public LibraryCard(ObjectId id, String userId, String userName, String cardNumber,
      Date registerDate, Date expireDate, Status status) {
    this._id = id;
    this.userId = userId;
    this.userName = userName;
    this.cardNumber = cardNumber;
    this.registerDate = registerDate;
    this.expireDate = expireDate;
    this.status = status;
  }

  public LibraryCard(Document document) {
    this._id = document.getObjectId("_id");
    this.userId = document.getString("userId");
    this.userName = document.getString("userName");
    this.cardNumber = document.getString("cardNumber");
    this.registerDate = document.getDate("registerDate");
    this.expireDate = document.getDate("expireDate");
    String statusString = document.getString("status");
    if (statusString == null) {
      this.status = Status.PENDING;
    } else {
      try {
        this.status = Status.valueOf(statusString);
      } catch (IllegalArgumentException e) {
        this.status = Status.PENDING;
      }
    }
  }

  public Status getStatus() { return this.status; }
  public void setRegisterDate(Date registerDate) { this.registerDate = registerDate; }
  public void setExpireDate(Date expireDate) { this.expireDate = expireDate; }
  public void setStatus(Status status) { this.status = status; }

  // Explicit getters for compilation
  public String getUserId() { return this.userId; }
  public ObjectId get_id() { return this._id; }
  public String getUserName() { return this.userName; }
  public Date getRegisterDate() { return this.registerDate; }
  public Date getExpireDate() { return this.expireDate; }
}



