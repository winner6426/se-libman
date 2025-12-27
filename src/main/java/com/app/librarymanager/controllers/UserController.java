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
import org.json.JSONArray;
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
    if (customClaims != null) {
      birthday = customClaims.optString("birthday", "");
      isAdmin = customClaims.optBoolean("admin", false);
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
    return new User(uid, email, password, displayName, birthday, phone, photo, createdAt, lastModified, lastSignIn, providerId, isAdmin, emailVerified, disabled);
  }

  public static User getUser(String userId) {
    try {
      checkPermission();
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
      claims.put("admin", user.isAdmin());
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
      Iterable<ExportedUserRecord> iterable = FirebaseAuth.getInstance().listUsers(null).iterateAll();
      for (ExportedUserRecord ur : iterable) {
        try {
          User user = userFromExportedRecord(ur);
          users.add(user);
        } catch (Exception inner) {
          System.err.println("UserController.listUsers: failed to convert an ExportedUserRecord: " + inner.getMessage());
          inner.printStackTrace();
        }
      }
      return users;
    } catch (Exception e) {
      //  System.err.println(e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  public static List<User> listUsers(List<String> ids) {
    try {
      List<User> users = new ArrayList<>();
      for (String id : ids) {
        if (id == null || id.isEmpty()) {
          continue;
        }
        try {
          UserRecord userRecord = FirebaseAuth.getInstance().getUser(id);
          if (userRecord == null) {
            continue;
          }
          User user = userFromRecord(userRecord);
          users.add(user);
        } catch (Exception inner) {
          System.err.println("UserController.listUsers(ids): failed to fetch/convert user " + id + ": " + inner.getMessage());
          inner.printStackTrace();
          // continue with others
        }
      }
      return users;
    } catch (Exception e) {
        System.err.println("aaa" + e.getMessage());
      return null;
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
    if (claims != null) {
      Object admin = claims.get("admin");
      if (admin instanceof Boolean) isAdmin = (Boolean) admin;
      Object b = claims.get("birthday");
      if (b != null) birthday = String.valueOf(b);
    }
    return new User(uid, email, "", displayName, birthday, phone, photo, createdAt, lastModified, lastLogin, "", isAdmin, userRecord.isEmailVerified(), userRecord.isDisabled());
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
    if (claims != null) {
      Object admin = claims.get("admin");
      if (admin instanceof Boolean) isAdmin = (Boolean) admin;
      Object b = claims.get("birthday");
      if (b != null) birthday = String.valueOf(b);
    }
    return new User(uid, email, "", displayName, birthday, phone, photo, createdAt, lastModified, lastLogin, "", isAdmin, userRecord.isEmailVerified(), userRecord.isDisabled());
  }
}
