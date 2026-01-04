package com.app.librarymanager.controllers;

import com.app.librarymanager.models.Transaction;
import com.app.librarymanager.services.MongoDB;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

public class TransactionController {

  private static final String COLLECTION_NAME = "transactions";

  /**
   * Create a new transaction
   */
  public static Document createTransaction(Transaction transaction) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("id", transaction.getId());
      data.put("type", transaction.getType().name());
      data.put("date", transaction.getDate());
      data.put("amount", transaction.getAmount());
      data.put("currencyCode", transaction.getCurrencyCode());
      data.put("note", transaction.getNote());

      return MongoDB.getInstance().addToCollection(COLLECTION_NAME, data);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Get all transactions with pagination
   */
  public static List<Transaction> getAllTransactions(int skip, int limit) {
    try {
      Bson filter = new Document(); // Empty filter to get all
      Bson sort = Sorts.descending("date");
      List<Document> documents = MongoDB.getInstance()
          .findSortedObject(COLLECTION_NAME, filter, sort, skip, limit);
      List<Transaction> transactions = new ArrayList<>();
      if (documents != null) {
        for (Document doc : documents) {
          transactions.add(new Transaction(doc));
        }
      }
      return transactions;
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  /**
   * Search transactions by keyword (search in note field)
   */
  public static List<Transaction> searchTransactions(String keyword, int skip, int limit) {
    try {
      Bson filter = Filters.regex("note", keyword, "i"); // Case-insensitive search
      Bson sort = Sorts.descending("date");
      List<Document> documents = MongoDB.getInstance()
          .findSortedObject(COLLECTION_NAME, filter, sort, skip, limit);
      List<Transaction> transactions = new ArrayList<>();
      if (documents != null) {
        for (Document doc : documents) {
          transactions.add(new Transaction(doc));
        }
      }
      return transactions;
    } catch (Exception e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  /**
   * Count total transactions
   */
  public static long countTransactions() {
    try {
      return MongoDB.getInstance().countDocuments(COLLECTION_NAME);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * Count transactions by search keyword
   */
  public static long countTransactionsBySearch(String keyword) {
    try {
      Bson filter = Filters.regex("note", keyword, "i");
      return MongoDB.getInstance().countDocuments(COLLECTION_NAME, filter);
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  /**
   * Delete a transaction
   */
  public static boolean deleteTransaction(ObjectId id) {
    try {
      return MongoDB.getInstance().deleteFromCollection(COLLECTION_NAME, "_id", id);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  /**
   * Update a transaction
   */
  public static Document updateTransaction(Transaction transaction) {
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("id", transaction.getId());
      data.put("type", transaction.getType().name());
      data.put("date", transaction.getDate());
      data.put("amount", transaction.getAmount());
      data.put("currencyCode", transaction.getCurrencyCode());
      data.put("note", transaction.getNote());

      return MongoDB.getInstance().updateData(COLLECTION_NAME, "_id", 
          transaction.get_id(), data);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Helper method to create transaction for selling books
   */
  public static void createSellBookTransaction(String bookTitle, int quantity, double unitPrice,
      String currencyCode) {
    double totalAmount = quantity * unitPrice;
    String note = String.format("Sold %d copy/copies of '%s' at %.2f %s each",
        quantity, bookTitle, unitPrice, currencyCode);

    Transaction transaction = new Transaction(
        Transaction.Type.SELL_BOOKS,
        totalAmount, // Positive amount (income)
        currencyCode,
        note
    );

    createTransaction(transaction);
  }

  /**
   * Helper method to create transaction for adding books
   */
  public static void createAddBookTransaction(String bookTitle, int quantity, double unitPrice,
      String currencyCode) {
    double totalAmount = -(quantity * unitPrice); // Negative amount (expense)
    String note = String.format("Added %d copy/copies of '%s' at %.2f %s each",
        quantity, bookTitle, unitPrice, currencyCode);

    Transaction transaction = new Transaction(
        Transaction.Type.ADD_BOOK,
        totalAmount,
        currencyCode,
        note
    );

    createTransaction(transaction);
  }

  /**
   * Helper method to create transaction for registering card
   */
  public static void createRegisterCardTransaction(String userName, double fee,
      String currencyCode) {
    createRegisterCardTransaction(userName, fee, currencyCode, 1);
  }

  /**
   * Helper method to create transaction for registering card with months
   */
  public static void createRegisterCardTransaction(String userName, double totalFee,
      String currencyCode, int months) {
    double monthlyFee = totalFee / months;
    String note = String.format(
        "Card registration approved for user '%s' - %d month(s) at %.0f %s/month (Total: %.0f %s)",
        userName, months, monthlyFee, currencyCode, totalFee, currencyCode);

    Transaction transaction = new Transaction(
        Transaction.Type.REGISTER_CARD,
        totalFee, // Positive amount (income)
        currencyCode,
        note
    );

    createTransaction(transaction);
  }
}
