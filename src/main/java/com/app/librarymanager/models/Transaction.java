package com.app.librarymanager.models;

import java.util.Date;
import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

@Data
public class Transaction {

  public enum Type {
    SELL_BOOKS,
    REGISTER_CARD,
    ADD_BOOK,
    RETURN_BOOK
  }

  private ObjectId _id;
  private String id;
  private Type type;
  private Date date;
  private double amount; // Negative means expense (-), Positive means income (+)
  private String currencyCode;
  private String note;

  public Transaction() {
    this._id = null;
    this.id = null;
    this.type = null;
    this.date = new Date();
    this.amount = 0.0;
    this.currencyCode = "VND";
    this.note = "";
  }

  public Transaction(Type type, double amount, String currencyCode, String note) {
    this._id = null;
    this.id = generateId();
    this.type = type;
    this.date = new Date();
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.note = note;
  }

  public Transaction(ObjectId _id, String id, Type type, Date date, double amount,
      String currencyCode, String note) {
    this._id = _id;
    this.id = id;
    this.type = type;
    this.date = date;
    this.amount = amount;
    this.currencyCode = currencyCode;
    this.note = note;
  }

  public Transaction(Document document) {
    this._id = document.getObjectId("_id");
    this.id = document.getString("id");
    String typeString = document.getString("type");
    if (typeString == null) {
      this.type = Type.SELL_BOOKS;
    } else {
      try {
        this.type = Type.valueOf(typeString);
      } catch (IllegalArgumentException e) {
        this.type = Type.SELL_BOOKS;
      }
    }
    this.date = document.getDate("date");
    this.amount = document.getDouble("amount") != null ? document.getDouble("amount") : 0.0;
    this.currencyCode = document.getString("currencyCode");
    this.note = document.getString("note");
  }

  private String generateId() {
    return "TXN-" + System.currentTimeMillis();
  }

  // Explicit getters for compilation
  public ObjectId get_id() {
    return this._id;
  }

  public String getId() {
    return this.id;
  }

  public Type getType() {
    return this.type;
  }

  public Date getDate() {
    return this.date;
  }

  public double getAmount() {
    return this.amount;
  }

  public String getCurrencyCode() {
    return this.currencyCode;
  }

  public String getNote() {
    return this.note;
  }

  public void set_id(ObjectId _id) {
    this._id = _id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public void setAmount(double amount) {
    this.amount = amount;
  }

  public void setCurrencyCode(String currencyCode) {
    this.currencyCode = currencyCode;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getTypeDisplay() {
    switch (type) {
      case SELL_BOOKS:
        return "Sell Books";
      case REGISTER_CARD:
        return "Register Card";
      case ADD_BOOK:
        return "Add Book";
      case RETURN_BOOK:
        return "Return Book";
      default:
        return "Unknown";
    }
  }
}
