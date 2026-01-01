package com.app.librarymanager.models;

import org.bson.Document;
import org.bson.types.ObjectId;

public class ReturnRecord {
  private ObjectId id;
  private ObjectId loanId;
  private String processedBy;
  private java.util.Date processedAt;
  private String condition;
  private String conditionNotes;
  private Double penaltyAmount;

  public ReturnRecord() {}

  public ReturnRecord(ObjectId loanId, String processedBy, java.util.Date processedAt, String condition, String conditionNotes, Double penaltyAmount) {
    this.id = new ObjectId();
    this.loanId = loanId;
    this.processedBy = processedBy;
    this.processedAt = processedAt;
    this.condition = condition;
    this.conditionNotes = conditionNotes;
    this.penaltyAmount = penaltyAmount;
  }

  public Document toDocument() {
    Document d = new Document();
    d.put("_id", id);
    d.put("loanId", loanId);
    d.put("processedBy", processedBy);
    d.put("processedAt", processedAt);
    d.put("condition", condition);
    d.put("conditionNotes", conditionNotes);
    d.put("penaltyAmount", penaltyAmount);
    return d;
  }

  public ReturnRecord(Document d) {
    this.id = d.getObjectId("_id");
    this.loanId = d.getObjectId("loanId");
    this.processedBy = d.getString("processedBy");
    this.processedAt = d.getDate("processedAt");
    this.condition = d.getString("condition");
    this.conditionNotes = d.getString("conditionNotes");
    this.penaltyAmount = d.getDouble("penaltyAmount");
  }

  // getters
  public ObjectId getId() { return id; }
  public ObjectId getLoanId() { return loanId; }
  public String getProcessedBy() { return processedBy; }
  public java.util.Date getProcessedAt() { return processedAt; }
  public String getCondition() { return condition; }
  public String getConditionNotes() { return conditionNotes; }
  public Double getPenaltyAmount() { return penaltyAmount; }
}
