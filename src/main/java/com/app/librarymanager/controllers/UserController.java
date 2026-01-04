package com.app.librarymanager.controllers;

import com.app.librarymanager.models.User;
import com.app.librarymanager.services.FirebaseAuthentication;
import com.app.librarymanager.utils.UploadFileUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserIdentifier;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class UserController {

  private static void checkPermission() {
    User currentUser = AuthController.getInstance().getCurrentUser();
    if (currentUser == null || !currentUser.isAdmin()) {
      throw new SecurityException("Access denied! You don't have permission to make the request.");
    }
  }

  public static int countTotalUser() {
    try {
      checkPermission();
      return FirebaseAuthentication.countTotalUser();
    } catch (Exception e) {
      return 0;
    }
  }

  private static User jsonToUser(JSONObject jsonUser) {
    if (jsonUser == null) return null;
    String uid = jsonUser.optString("uid", jsonUser.optString("localId", ""));
    String email = jsonUser.optString("email", "");
    String password = jsonUser.optString("password", "");
    String displayName = jsonUser.optString("displayName", "");
    JSONObject customClaims = jsonUser.optJSONObject("customClaims");
    String birthday = "";
    boolean isAdmin = false;
    String role = "USER";
    if (customClaims != null) {
      birthday = customClaims.optString("birthday", "");
      isAdmin = customClaims.optBoolean("admin", false);
      if (customClaims.has("role") && !customClaims.isNull("role")) {
        role = customClaims.optString("role", "USER");
      } else if (customClaims.has("roles") && !customClaims.isNull("roles")) {
        org.json.JSONArray arr = customClaims.optJSONArray("roles");
        if (arr != null && arr.length() > 0) {
          role = arr.optString(0, "USER");
        }
      } else if (isAdmin) {
        role = "ADMIN";
      }
    }
    String phone = jsonUser.optString("phoneNumber", "");
    String photo = jsonUser.optString("photoUrl", "");
    JSONObject metadata = jsonUser.optJSONObject("userMetadata");
    String createdAt = metadata != null ? String.valueOf(metadata.optLong("creationTimestamp", 0L)) : "";
    String lastModified = metadata != null ? String.valueOf(metadata.optLong("lastModifiedAt", 0L)) : "";
    String lastSignIn = metadata != null ? String.valueOf(metadata.optLong("lastSignInTimestamp", 0L)) : "";
    String providerId = jsonUser.optString("providerId", "");
    boolean emailVerified = jsonUser.optBoolean("emailVerified", false);
    boolean disabled = jsonUser.optBoolean("disabled", false);
    User u = new User(uid, email, password, displayName, birthday, phone, photo, createdAt, lastModified, lastSignIn, providerId, isAdmin, emailVerified, disabled);
    u.setRole(role);
    return u;
  }

  public static User getUser(String userId) {
    try {
      UserRecord ur = FirebaseAuth.getInstance().getUser(userId);
      if (ur == null) return null;
      try {
        return userFromRecord(ur);
      } catch (Exception e) {
        // Fallback: try JSON path when record conversion fails
        try {
          JSONObject jsonUser = new JSONObject(FirebaseAuth.getInstance().getUser(userId));
          return jsonToUser(jsonUser);
        } catch (Exception inner) {
          inner.printStackTrace();
          return null;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static JSONObject createUser(User user) {
    try {
      checkPermission();
      JSONObject registerResponse = FirebaseAuthentication.createAccountWithEmailAndPassword(user);

      if (!registerResponse.getBoolean("success")) {
        // Giữ nguyên cấu trúc trả về lỗi từ FirebaseAuthentication
        return registerResponse;
      }

      // Lấy localId từ response đăng ký để truy vấn đầy đủ thông tin user (bao gồm createdAt)
      JSONObject data = registerResponse.getJSONObject("data");
      String localId = data.optString("localId", null);

      if (localId == null || localId.isEmpty()) {
        return new JSONObject().put("success", false)
            .put("message", "User created but missing localId in response.");
      }

      // Lấy lại user từ Firebase Admin SDK để có metadata (creationTimestamp, lastSignIn, ...)
      JSONObject createdUserJson = new JSONObject(FirebaseAuth.getInstance().getUser(localId));

      return new JSONObject()
          .put("success", true)
          .put("data", createdUserJson)
          .put("message", "User created successfully.");
    } catch (Exception e) {
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static JSONObject updateUser(User user) {
    //  System.out.println(user);
    try {
      checkPermission();
      String newPhotoUrl = "";
      if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty() && !user.getPhotoUrl()
          .startsWith("http")) {
        JSONObject resp = UploadFileUtil.uploadImage(user.getPhotoUrl(), user.getUid(), 96);
        if (resp.getBoolean("success")) {
          newPhotoUrl = resp.getString("longURL");
        } else {
          return resp;
        }
      }
      if (newPhotoUrl != null && !newPhotoUrl.isEmpty()) {
        user.setPhotoUrl(newPhotoUrl);
      }
      UpdateRequest userUpdate = getUpdateRequest(user);
      UserRecord updatedUser = FirebaseAuth.getInstance().updateUser(userUpdate);
      Map<String, Object> claims = new HashMap<>();
      String role = user.getRole() == null ? "USER" : user.getRole();
      claims.put("role", role);
      claims.put("admin", user.isAdmin() || "ADMIN".equalsIgnoreCase(role));
      claims.put("birthday", user.getBirthday());
      FirebaseAuth.getInstance().setCustomUserClaims(user.getUid(), claims);
      return new JSONObject().put("success", true).put("data", new JSONObject(updatedUser))
          .put("message", "User updated successfully.");
    } catch (Exception e) {
      //  System.err.println(e.getMessage());
      JSONObject errorResponse = new JSONObject();
      try {
        String responseBody = e.getMessage().substring(e.getMessage().indexOf("{"));
        //  System.out.println(responseBody);
        if (responseBody.contains("{")) {
          responseBody = responseBody.substring(responseBody.indexOf("{"));
          //  System.out.println(responseBody);
          JSONObject responseJson = new JSONObject(responseBody);
          String errorMessage = responseJson.getJSONObject("error").getString("message");
          errorResponse.put("message", errorMessage);
        } else {
          errorResponse.put("message", e.getMessage());
        }
      } catch (Exception parseException) {
        errorResponse.put("message", e.getMessage());
      }
      return new JSONObject().put("success", false)
          .put("message", errorResponse.getString("message"));
    }
  }

  private static UpdateRequest getUpdateRequest(User user) {
    UpdateRequest userUpdate = new UpdateRequest(user.getUid());
    userUpdate.setEmailVerified(user.isEmailVerified());
    userUpdate.setPhoneNumber(
        user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty() ? user.getPhoneNumber()
            : null);
    if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
      userUpdate.setDisplayName(user.getDisplayName());
    }
    if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
      userUpdate.setPhotoUrl(user.getPhotoUrl());
    }
    if (user.getPassword() != null && !user.getPassword().isEmpty()) {
      userUpdate.setPassword(user.getPassword());
    }
    userUpdate.setDisabled(user.isDisabled());
    return userUpdate;
  }

  public static JSONObject deleteUser(User user) {
    try {
      checkPermission();

      //if(user.
      BookLoanController.returnAllBookOf(user.getUid());
      BookLoanController.removeAllLoanOf(user.getUid());
      CommentController.removeAllCommentOf(user.getUid());
      BookRatingController.removeAllRatingOf(user.getUid());
      FavoriteController.removeAllFavoriteOf(user.getUid());
      LibraryCardController.removeAllCardOf(user.getUid());
      FirebaseAuth.getInstance().deleteUser(user.getUid());
      return new JSONObject().put("success", true).put("message", "User deleted successfully.");
    } catch (Exception e) {
      return new JSONObject().put("success", false).put("message", e.getMessage());
    }
  }

  public static List<User> listUsers() {
    try {
      checkPermission();
      List<User> users = new ArrayList<>();
      // Use FirebaseAdmin's iterator to avoid converting to JSON and potential runtime issues
      try {
        // Use getValues() for compatibility with the firebase-admin version used at runtime
        Iterable<ExportedUserRecord> iterable = FirebaseAuth.getInstance().listUsers(null).getValues();
        for (ExportedUserRecord ur : iterable) {
          try {
            User user = userFromExportedRecord(ur);
            users.add(user);
          } catch (Throwable inner) {
            System.err.println("UserController.listUsers: failed to convert an ExportedUserRecord: " + inner.getMessage());
            inner.printStackTrace();
          }
        }
      } catch (Throwable t) {
        // Could not iterate using Admin SDK; log and return empty list rather than crash the UI
        System.err.println("UserController.listUsers: failed to list via Admin SDK (" + t.getClass().getName() + "): " + t);
        t.printStackTrace();
        return users;
      }
      return users;
    } catch (Error err) {
      // Explicitly catch Errors (including compilation stubs thrown by some build systems)
      System.err.println("UserController.listUsers: Caught Error (" + err.getClass().getName() + "): " + err);
      try {
        java.net.URL src = UserController.class.getProtectionDomain().getCodeSource().getLocation();
        System.err.println("UserController.class loaded from: " + src);
      } catch (Throwable ignore) {
      }
      err.printStackTrace();
      return new ArrayList<>();
    } catch (Throwable e) {
      System.err.println("UserController.listUsers: unexpected throwable: " + e.getMessage());
      e.printStackTrace();
      return new ArrayList<>();
    }
  }

  public static List<User> listUsers(List<String> ids) {
    try {
      System.err.println("UserController.listUsers: called with ids=" + (ids == null ? 0 : ids.size()));
      List<User> users = new ArrayList<>();
      if (ids == null) return users;
      // First try a batched REST lookup via Identity Toolkit (more robust across SDK versions)
      try {
        List<org.json.JSONObject> found = FirebaseAuthentication.lookupUsersByLocalIds(ids);
        if (found != null && !found.isEmpty()) {
          for (org.json.JSONObject j : found) {
            try {
              User u = jsonToUser(j);
              if (u != null) users.add(u);
            } catch (Throwable inner) { inner.printStackTrace(); }
          }
          return users;
        }
      } catch (Throwable t) {
        System.err.println("UserController.listUsers: REST lookup failed: " + t.getMessage());
      }

      // Fallback: fetch individually using Admin SDK (with safeguards)
      for (String id : ids) {
        if (id == null || id.isEmpty()) continue;
        try {
          User user = getUser(id);
          if (user == null) {
            System.err.println("UserController.listUsers: no user found for id=" + id);
            continue;
          }
          users.add(user);
        } catch (Throwable inner) {
          System.err.println("UserController.listUsers(ids): failed to fetch/convert user " + id + " (" + inner.getClass().getName() + "): " + inner);
          inner.printStackTrace();
        }
      }
      return users;
    } catch (Error err) {
      System.err.println("UserController.listUsers(ids): Caught Error (" + err.getClass().getName() + "): " + err);
      try {
        java.net.URL src = UserController.class.getProtectionDomain().getCodeSource().getLocation();
        System.err.println("UserController.class loaded from: " + src);
      } catch (Throwable ignore) {
      }
      err.printStackTrace();
      return new ArrayList<>();
    } catch (Throwable e) {
      System.err.println("UserController.listUsers: unexpected throwable: " + e.getMessage());
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
  
  /**
   * Safe version of listing users by ids. Attempts to use batched REST lookup first,
   * then falls back to per-id Admin SDK lookup for any missing users.
   */
  public static List<User> listUsersSafe(List<String> ids) {
    try {
      if (ids == null || ids.isEmpty()) return new ArrayList<>();
      List<org.json.JSONObject> found;
      try {
        found = FirebaseAuthentication.lookupUsersByLocalIds(ids);
      } catch (Error err) {
        // Defensive: catch Errors (e.g., unresolved compilation stubs from incremental compilers)
        System.err.println("UserController.listUsersSafe: lookupUsersByLocalIds threw Error: " + err);
        try {
          java.net.URL src = com.app.librarymanager.services.FirebaseAuthentication.class.getProtectionDomain().getCodeSource().getLocation();
          System.err.println("FirebaseAuthentication.class loaded from: " + src);
        } catch (Throwable ignore) {}
        err.printStackTrace();
        found = java.util.List.of();
      } catch (Throwable t) {
        System.err.println("UserController.listUsersSafe: lookupUsersByLocalIds threw: " + (t == null ? "null" : t.getMessage()));
        t.printStackTrace();
        found = java.util.List.of();
      }
      List<User> users = new ArrayList<>();
      if (found != null && !found.isEmpty()) {
        for (org.json.JSONObject j : found) {
          try {
            User u = jsonToUser(j);
            if (u != null) users.add(u);
          } catch (Throwable inner) {
            inner.printStackTrace();
          }
        }
      }
      // If REST lookup returned fewer users than requested, try per-id Admin SDK lookup as a fallback
      if (users.size() < ids.size()) {
        for (String id : ids) {
          boolean already = users.stream().anyMatch(u -> id.equals(u.getUid()));
          if (already) continue;
          try {
            User u = getUser(id);
            if (u != null) users.add(u);
          } catch (Throwable t) {
            System.err.println("UserController.listUsersSafe: per-id fallback failed for " + id + ": " + (t == null ? "null" : t.getMessage()));
            // continue without failing
          }
        }
      }
      if (users.size() < ids.size()) {
        // Log which ids are still missing for diagnostics (only when non-empty)
        java.util.Set<String> present = users.stream().map(User::getUid).collect(java.util.stream.Collectors.toSet());
        List<String> missing = ids.stream().filter(i -> !present.contains(i)).toList();
        if (!missing.isEmpty()) {
          System.err.println("UserController.listUsersSafe: missing user ids after fallback: " + missing);
        }
      }
      return users;
    } catch (Throwable t) {
      System.err.println("UserController.listUsersSafe: failed: " + (t == null ? "null" : t.getMessage()));
      t.printStackTrace();
      return new ArrayList<>();
    }
  }

  /**
   * Safe version of listing all users. Attempts to use the Admin SDK iterator,
   * but never allows Errors to propagate to callers; on failure it returns an
   * empty list and logs the full throwable for diagnostics.
   */
  public static List<User> listAllUsersSafe() {
    // Safer implementation: attempt reflective enumeration to avoid direct SDK linkage
    try {
      List<User> users = new ArrayList<>();
      try {
        // Use Class.forName to avoid static linking to Firebase SDK which may cause
        // 'Unresolved compilation problem' Errors when SDK versions mismatch at runtime.
        Class<?> firebaseAuthClass = Class.forName("com.google.firebase.auth.FirebaseAuth");
        java.lang.reflect.Method getInstance = firebaseAuthClass.getMethod("getInstance");
        Object authInstance = getInstance.invoke(null);
        java.lang.reflect.Method[] methods = firebaseAuthClass.getMethods();
        for (java.lang.reflect.Method m : methods) {
          if (!m.getName().contains("listUsers")) continue;
          Object res = null;
          try {
            if (m.getParameterCount() == 0) {
              res = m.invoke(authInstance);
            } else {
              Object[] params = new Object[m.getParameterCount()];
              for (int i = 0; i < params.length; i++) params[i] = null;
              res = m.invoke(authInstance, params);
            }
          } catch (Throwable invokeErr) {
            continue;
          }
          if (res == null) continue;
          // Try getValues()
          try {
            java.lang.reflect.Method gv = res.getClass().getMethod("getValues");
            Object vals = gv.invoke(res);
            if (vals instanceof Iterable) {
              for (Object o : (Iterable<?>) vals) {
                  try {
                    // Try to extract common fields via reflection
                    String uid = null;
                    String email = null;
                    String displayName = null;
                    String phone = null;
                    String photo = null;
                    String birthday = null;
                    String createdAt = null;
                    String lastModified = null;
                    String lastLogin = null;
                    boolean isAdmin = false;
                    boolean emailVerified = false;
                    boolean disabled = false;
                    String role = null;

                    try { java.lang.reflect.Method mUid = o.getClass().getMethod("getUid"); uid = String.valueOf(mUid.invoke(o)); } catch (Throwable ignore) {}
                    try { java.lang.reflect.Method mEmail = o.getClass().getMethod("getEmail"); Object em = mEmail.invoke(o); email = em == null ? "" : String.valueOf(em);} catch (Throwable ignore) {}
                    try { java.lang.reflect.Method mName = o.getClass().getMethod("getDisplayName"); Object dn = mName.invoke(o); displayName = dn == null ? "" : String.valueOf(dn);} catch (Throwable ignore) {}
                    try { java.lang.reflect.Method mPhone = o.getClass().getMethod("getPhoneNumber"); Object ph = mPhone.invoke(o); phone = ph == null ? "" : String.valueOf(ph);} catch (Throwable ignore) {}
                    try { java.lang.reflect.Method mPhoto = o.getClass().getMethod("getPhotoUrl"); Object ph = mPhoto.invoke(o); photo = ph == null ? "" : String.valueOf(ph);} catch (Throwable ignore) {}

                    // user metadata timestamps
                    try {
                      java.lang.reflect.Method mMeta = o.getClass().getMethod("getUserMetadata");
                      Object meta = mMeta.invoke(o);
                      if (meta != null) {
                        try { java.lang.reflect.Method mCreate = meta.getClass().getMethod("getCreationTimestamp"); Object v = mCreate.invoke(meta); createdAt = v == null ? null : String.valueOf(v);} catch (Throwable ignore) {}
                        try { java.lang.reflect.Method mRefresh = meta.getClass().getMethod("getLastRefreshTimestamp"); Object v = mRefresh.invoke(meta); lastModified = v == null ? null : String.valueOf(v);} catch (Throwable ignore) {}
                        try { java.lang.reflect.Method mSignIn = meta.getClass().getMethod("getLastSignInTimestamp"); Object v = mSignIn.invoke(meta); lastLogin = v == null ? null : String.valueOf(v);} catch (Throwable ignore) {}
                      }
                    } catch (Throwable ignore) {}

                    // boolean flags and custom claims
                    try { java.lang.reflect.Method mEmailVerified = o.getClass().getMethod("isEmailVerified"); Object v = mEmailVerified.invoke(o); emailVerified = v != null && (Boolean) v;} catch (Throwable ignore) {}
                    try { java.lang.reflect.Method mDisabled = o.getClass().getMethod("isDisabled"); Object v = mDisabled.invoke(o); disabled = v != null && (Boolean) v;} catch (Throwable ignore) {}
                    try {
                      java.lang.reflect.Method mClaims = o.getClass().getMethod("getCustomClaims");
                      Object claims = mClaims.invoke(o);
                      if (claims instanceof java.util.Map) {
                        @SuppressWarnings("unchecked") java.util.Map<String, Object> map = (java.util.Map<String, Object>) claims;
                        Object adminObj = map.get("admin"); if (adminObj instanceof Boolean) isAdmin = (Boolean) adminObj;
                        Object roleObj = map.get("role"); if (roleObj != null) role = String.valueOf(roleObj);
                        Object bObj = map.get("birthday"); if (bObj != null) birthday = String.valueOf(bObj);
                      }
                    } catch (Throwable ignore) {}

                    // Build user object when we have at least uid
                    if (uid != null) {
                        User u = new User(uid, email == null ? "" : email, "", displayName == null ? "" : displayName,
                          birthday == null ? "" : birthday, phone == null ? "" : phone, photo == null ? "" : photo,
                          createdAt == null ? "" : createdAt, lastModified == null ? "" : lastModified,
                          lastLogin == null ? "" : lastLogin, "", isAdmin, emailVerified, disabled);
                      if (role != null && !role.isEmpty()) u.setRole(role);
                      users.add(u);
                      continue;
                    }

                    // Fallback to JSON conversion
                    try {
                      org.json.JSONObject j = new org.json.JSONObject(o);
                      User u = jsonToUser(j);
                      if (u != null) users.add(u);
                    } catch (Throwable ignore) {}
                  } catch (Throwable inner) { inner.printStackTrace(); }
              }
              return users;
            }
          } catch (NoSuchMethodException ns) {
            if (res instanceof Iterable) {
              for (Object o : (Iterable<?>) res) {
                try {
                  try {
                    java.lang.reflect.Method getUid = o.getClass().getMethod("getUid");
                    String uid = String.valueOf(getUid.invoke(o));
                    String email = null;
                    try {
                      java.lang.reflect.Method getEmail = o.getClass().getMethod("getEmail");
                      Object em = getEmail.invoke(o);
                      email = em == null ? "" : String.valueOf(em);
                    } catch (Throwable ignore) { email = ""; }
                    User u = new User(uid, email, "", "", "", "", "", "", "", "", "", false, false, false);
                    users.add(u);
                    continue;
                  } catch (Throwable ignore) {}
                } catch (Throwable inner) { inner.printStackTrace(); }
              }
              return users;
            }
          }
        }
      } catch (Throwable t) {
        // fallthrough to empty
      }
      return users;
    } catch (Throwable t) {
      t.printStackTrace();
      return new ArrayList<>();
    }
  }

  private static User userFromRecord(UserRecord userRecord) {
    String uid = userRecord.getUid();
    String email = userRecord.getEmail();
    String displayName = userRecord.getDisplayName();
    String phone = userRecord.getPhoneNumber();
    String photo = userRecord.getPhotoUrl();
    String createdAt = userRecord.getUserMetadata() != null ? String.valueOf(userRecord.getUserMetadata().getCreationTimestamp()) : "";
    String lastModified = userRecord.getUserMetadata() != null ? String.valueOf(userRecord.getUserMetadata().getLastRefreshTimestamp()) : "";
    String lastLogin = userRecord.getUserMetadata() != null ? String.valueOf(userRecord.getUserMetadata().getLastSignInTimestamp()) : "";
    Map<String, Object> claims = userRecord.getCustomClaims();
    boolean isAdmin = false;
    String birthday = "";
    String role = "USER";
    if (claims != null) {
      Object admin = claims.get("admin");
      if (admin instanceof Boolean) isAdmin = (Boolean) admin;
      Object b = claims.get("birthday");
      if (b != null) birthday = String.valueOf(b);
      Object r = claims.get("role");
      if (r != null) role = String.valueOf(r);
      else {
        Object rr = claims.get("roles");
        if (rr instanceof java.util.List) {
          @SuppressWarnings("unchecked")
          java.util.List<Object> list = (java.util.List<Object>) rr;
          if (!list.isEmpty()) role = String.valueOf(list.get(0));
        }
      }
      if ((role == null || role.isEmpty()) && isAdmin) role = "ADMIN";
    }
    User u = new User(uid, email, "", displayName, birthday, phone, photo, createdAt, lastModified, lastLogin, "", isAdmin, userRecord.isEmailVerified(), userRecord.isDisabled());
    u.setRole(role);
    return u;
  }

  private static User userFromExportedRecord(ExportedUserRecord userRecord) {
    String uid = userRecord.getUid();
    String email = userRecord.getEmail();
    String displayName = userRecord.getDisplayName();
    String phone = userRecord.getPhoneNumber();
    String photo = userRecord.getPhotoUrl();
    String createdAt = userRecord.getUserMetadata() != null ? String.valueOf(userRecord.getUserMetadata().getCreationTimestamp()) : "";
    String lastModified = userRecord.getUserMetadata() != null ? String.valueOf(userRecord.getUserMetadata().getLastRefreshTimestamp()) : "";
    String lastLogin = userRecord.getUserMetadata() != null ? String.valueOf(userRecord.getUserMetadata().getLastSignInTimestamp()) : "";
    Map<String, Object> claims = userRecord.getCustomClaims();
    boolean isAdmin = false;
    String birthday = "";
    String role = "USER";
    if (claims != null) {
      Object admin = claims.get("admin");
      if (admin instanceof Boolean) isAdmin = (Boolean) admin;
      Object b = claims.get("birthday");
      if (b != null) birthday = String.valueOf(b);
      Object r = claims.get("role");
      if (r != null) role = String.valueOf(r);
      else {
        Object rr = claims.get("roles");
        if (rr instanceof java.util.List) {
          @SuppressWarnings("unchecked")
          java.util.List<Object> list = (java.util.List<Object>) rr;
          if (!list.isEmpty()) role = String.valueOf(list.get(0));
        }
      }
      if ((role == null || role.isEmpty()) && isAdmin) role = "ADMIN";
    }
    User u = new User(uid, email, "", displayName, birthday, phone, photo, createdAt, lastModified, lastLogin, "", isAdmin, userRecord.isEmailVerified(), userRecord.isDisabled());
    u.setRole(role);
    return u;
  }
}
