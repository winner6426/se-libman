package com.app.librarymanager.controllers;

import static com.mongodb.client.model.Filters.eq;

import com.app.librarymanager.models.RolePermission;
import com.app.librarymanager.services.MongoDB;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

public class RolePermissionController {
  private static final String COLLECTION = "role_permissions";

  public static RolePermission getByRole(String role) {
    try {
      Document doc = MongoDB.getInstance().findAnObject(COLLECTION, "role", role);
      if (doc == null) {
        return null;
      }
      return new RolePermission(doc);
    } catch (Exception e) {
      System.err.println("RolePermissionController.getByRole: exception for role='" + role + "' -> " + e.getMessage());
      return null;
    }
  }

  public static List<RolePermission> listAll() {
    try {
      List<RolePermission> out = new ArrayList<>();
      List<Document> docs = MongoDB.getInstance().findAllObject(COLLECTION, null);
      if (docs == null) return out;
      for (Document d : docs) {
        out.add(new RolePermission(d));
      }
      return out;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public static boolean upsert(RolePermission rp) {
    try {
      Document doc = rp.toDocument();
      // if exists, replace
      Document existing = MongoDB.getInstance().findAnObject(COLLECTION, "role", rp.getRole());
      if (existing != null) {
        Document updated = MongoDB.getInstance().updateData(COLLECTION, "role", rp.getRole(), doc);
        if (updated != null) {
          return true;
        } else {
          return false;
        }
      }
      Document inserted = MongoDB.getInstance().addToCollection(COLLECTION, doc);
      if (inserted != null) {
        return true;
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }
}
