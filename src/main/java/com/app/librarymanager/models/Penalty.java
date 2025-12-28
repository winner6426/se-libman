package com.app.librarymanager.models;

import org.bson.Document;
import org.bson.types.ObjectId;

public class Penalty {
  private ObjectId id;
  private String userId;
  private String description;
  private Double amount;
  private java.util.Date createdAt;
  private boolean paid;

  public Penalty() {}

  public Penalty(String userId, String description, Double amount) {
    this.id = new ObjectId();
    this.userId = userId;
    this.description = description;
    this.amount = amount;
    this.createdAt = new java.util.Date();
    this.paid = false;
  }

  public Document toDocument() {
    Document d = new Document();
    d.put("_id", id);
    d.put("userId", userId);
    d.put("description", description);
    d.put("amount", amount);
    d.put("createdAt", createdAt);
    d.put("paid", paid);
    return d;
  }

  public Penalty(Document d) {
    this.id = d.getObjectId("_id");
    this.userId = d.getString("userId");
    this.description = d.getString("description");
    this.amount = d.getDouble("amount");
    this.createdAt = d.getDate("createdAt");
    this.paid = d.getBoolean("paid");
  }

  // getters
  public ObjectId getId() { return id; }
  public String getUserId() { return userId; }
  public String getDescription() { return description; }
  public Double getAmount() { return amount; }
  public java.util.Date getCreatedAt() { return createdAt; }
  public boolean isPaid() { return paid; }
}
