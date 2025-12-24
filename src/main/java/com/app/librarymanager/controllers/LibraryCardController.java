package com.app.librarymanager.controllers;

import static com.mongodb.client.model.Filters.eq;

import com.app.librarymanager.models.LibraryCard;
import com.app.librarymanager.models.LibraryCard.Status;
import com.app.librarymanager.services.MongoDB;
import com.mongodb.client.model.Filters;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class LibraryCardController {

  private static final String COLLECTION_NAME = "libraryCard";

  public static LibraryCard getCardOfUser(String userId) {
    try {
      Document doc = MongoDB.getInstance().findAnObject(COLLECTION_NAME, "userId", userId);
      if (doc == null) {
        return null;
      }
      return new LibraryCard(doc);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * User request a new library card.
   * If the user already has a PENDING or APPROVED card, we keep the existing one.
   * If the last card was REJECTED or there is no card, we create a new PENDING request.
   */
  public static Document requestCard(String userId, String userName) {
    return requestCard(userId, userName, 1); // Default to 1 month
  }

  /**
   * User request a new library card with specified number of months.
   * If the user already has a PENDING or APPROVED card, we keep the existing one.
   * If the last card was REJECTED or there is no card, we create a new PENDING request.
   */
  public static Document requestCard(String userId, String userName, int months) {
    try {
      MongoDB database = MongoDB.getInstance();
      Document existing = database.findAnObject(COLLECTION_NAME, "userId", userId);
      if (existing != null) {
        String status = existing.getString("status");
        if (!Status.REJECTED.name().equals(status)) {
          return existing;
        }
      }
      LibraryCard card = new LibraryCard(userId, userName);
      Map<String, Object> data = MongoDB.objectToMap(card);
      data.put("registerDate", new Date());
      data.put("status", Status.PENDING.name());
      data.put("months", months); // Store the number of months
      return database.addToCollection(COLLECTION_NAME, data);
    } catch (Exception e) {
      return null;
    }
  }

  public static Document approveCard(ObjectId id, Date expireDate) {
    try {
      Map<String, Object> update = new HashMap<>();
      update.put("expireDate", expireDate);
      update.put("status", Status.APPROVED.name());
      return MongoDB.getInstance().updateData(COLLECTION_NAME, "_id", id, update);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Approve card and calculate expire date based on register date and number of months.
   * If months is not stored in the document, defaults to 1 month.
   */
  public static Document approveCardWithMonths(ObjectId id) {
    try {
      Document cardDoc = MongoDB.getInstance().findAnObject(COLLECTION_NAME, "_id", id);
      if (cardDoc == null) {
        return null;
      }

      Date registerDate = cardDoc.getDate("registerDate");
      if (registerDate == null) {
        registerDate = new Date();
      }

      Integer months = cardDoc.getInteger("months");
      if (months == null || months <= 0) {
        months = 1; // Default to 1 month
      }

      // Calculate expire date
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(registerDate);
      calendar.add(Calendar.MONTH, months);
      Date expireDate = calendar.getTime();

      Map<String, Object> update = new HashMap<>();
      update.put("expireDate", expireDate);
      update.put("status", Status.APPROVED.name());
      return MongoDB.getInstance().updateData(COLLECTION_NAME, "_id", id, update);
    } catch (Exception e) {
      return null;
    }
  }

  public static Document rejectCard(ObjectId id) {
    try {
      Map<String, Object> update = new HashMap<>();
      update.put("status", Status.REJECTED.name());
      return MongoDB.getInstance().updateData(COLLECTION_NAME, "_id", id, update);
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean removeAllCardOf(String userId) {
    try {
      return MongoDB.getInstance()
          .deleteAll(COLLECTION_NAME, Filters.eq("userId", userId));
    } catch (Exception e) {
      return false;
    }
  }

  public static List<LibraryCard> getAllCards() {
    try {
      List<Document> docs = MongoDB.getInstance()
          .findAllObject(COLLECTION_NAME, Filters.empty());
      if (docs == null) {
        return null;
      }
      return docs.stream().map(LibraryCard::new).collect(Collectors.toList());
    } catch (Exception e) {
      return null;
    }
  }

  public static List<LibraryCard> getCardsByStatus(Status status) {
    try {
      Bson filter = eq("status", status.name());
      List<Document> docs = MongoDB.getInstance()
          .findAllObject(COLLECTION_NAME, filter);
      if (docs == null) {
        return null;
      }
      return docs.stream().map(LibraryCard::new).collect(Collectors.toList());
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean updateCard(LibraryCard card) {
    try {
      if (card.get_id() == null) {
        return false;
      }
      Map<String, Object> update = new HashMap<>();
      // userName is not editable, so we don't update it
      update.put("registerDate", card.getRegisterDate());
      update.put("expireDate", card.getExpireDate());
      update.put("status", card.getStatus() != null ? card.getStatus().name() : Status.PENDING.name());
      Document result = MongoDB.getInstance().updateData(COLLECTION_NAME, "_id", card.get_id(), update);
      return result != null;
    } catch (Exception e) {
      return false;
    }
  }
}



