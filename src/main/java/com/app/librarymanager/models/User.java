package com.app.librarymanager.models;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import lombok.Data;
// Removed NotNull annotation usage to avoid runtime annotation accessibility issues
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {

  private String uid;
  private String email;
  private String password;
  private String displayName;
  private String birthday;
  private String phoneNumber;
  private String photoUrl;
  private String createdAt;
  private String lastModifiedDate;
  private String lastLoginAt;
  private String providerId;
  private boolean admin;
  // Single role per account (one of Role enum values). Default is "USER" for new accounts.
  private String role = "USER";
  private boolean emailVerified;
  private boolean disabled;
  private Image avatar;


  public User(String email, String password, String displayName, String birthday,
      String phoneNumber, String photoUrl, String createdAt, String lastModifiedDate,
      boolean admin) {
    this.email = email;
    this.password = password;
    this.displayName = displayName;
    this.birthday = birthday;
    this.phoneNumber = phoneNumber;
    this.photoUrl = photoUrl;
    this.createdAt = createdAt;
    this.lastModifiedDate = lastModifiedDate;
    this.admin = admin;
  }

  public User(String uid, String email, String password, String displayName, String birthday,
      String phoneNumber, String photoUrl, String createdAt, String lastModifiedDate,
      String lastLoginAt, String providerId,
      boolean admin, boolean emailVerified, boolean disabled) {
    this.uid = uid;
    this.email = email;
    this.password = password;
    this.displayName = displayName;
    this.birthday = birthday;
    this.phoneNumber = phoneNumber;
    this.photoUrl = photoUrl;
    this.createdAt = createdAt;
    this.lastModifiedDate = lastModifiedDate;
    this.admin = admin;
    this.emailVerified = emailVerified;
    this.disabled = disabled;
    this.lastLoginAt = lastLoginAt;
    this.providerId = providerId;
    if (photoUrl != null && !photoUrl.isEmpty()) {
      setAvatar(photoUrl);
    }
  }

  public User() {
    this.admin = false;
    this.emailVerified = false;
    this.disabled = false;
  }

  public User(String email, String password, String fullName, String birthday, String photoUrl,
      boolean admin) {
    this.email = email;
    this.password = password;
    this.displayName = fullName;
    this.birthday = birthday;
    this.admin = admin;
    this.photoUrl = photoUrl;

    if (photoUrl != null && !photoUrl.isEmpty()) {
      setAvatar(photoUrl);
    }
  }

  public User(String uid) {
    this.uid = uid;
  }

  public User(JSONObject data) {
    this.uid = data.optString("uid", data.optString("localId"));
    this.email = data.getString("email");
    this.displayName = data.optString("displayName", "");
    this.birthday = data.optString("birthday");
    this.phoneNumber = data.optString("phoneNumber");
    this.photoUrl = data.optString("photoUrl");
    this.createdAt = data.optString("createdAt");
    this.lastModifiedDate = data.optString("lastModifiedDate", "");
    this.lastLoginAt = data.optString("lastLoginAt", "");
    this.providerId = data.optString("providerId", "");
    this.admin = data.optBoolean("admin", false);
    // Read role if available (single-role system). Backwards compatibility: accept
    // a `role` string or a `roles` array (take first element), otherwise fall back to admin flag.
    if (data.has("role") && !data.isNull("role")) {
      this.role = data.optString("role", "USER");
    } else if (data.has("roles") && !data.isNull("roles")) {
      JSONArray arr = data.optJSONArray("roles");
      if (arr != null && arr.length() > 0) {
        this.role = arr.optString(0, "USER");
      }
    } else if (this.admin) {
      // Backwards compatibility: admin flag implies ADMIN role
      this.role = "ADMIN";
    } else {
      // Default role
      this.role = "USER";
    }
    this.emailVerified = data.optBoolean("emailVerified", false);
    this.disabled = data.optBoolean("disabled", false);

    if (photoUrl != null && !photoUrl.isEmpty()) {
      setAvatar(photoUrl);
    }
  }

  private void setAvatar(String photoUrl) {
    Task<Image> loadImageTask = getImageTask(photoUrl);
    new Thread(loadImageTask).start();
  }

  // Explicit getters for compilation safety
  public String getUid() {
    return this.uid;
  }

  public String getDisplayName() {
    return this.displayName;
  }

  public String getEmail() {
    return this.email;
  }

  public String getPhotoUrl() {
    return this.photoUrl;
  }

  public boolean isAdmin() {
    // Prefer explicit role; keep admin flag compatibility
    return this.admin || "ADMIN".equalsIgnoreCase(this.role);
  }
  public String getRole() { return this.role; }

  public void setRole(String role) {
    this.role = role == null ? "USER" : role;
    // keep admin boolean in sync for compatibility
    this.admin = "ADMIN".equalsIgnoreCase(this.role);
  }

  public boolean hasRole(String role) {
    if (role == null) return false;
    return role.equalsIgnoreCase(this.role);
  }

  // Additional explicit getters/setters used across controllers
  public String getPhoneNumber() { return this.phoneNumber; }
  public String getBirthday() { return this.birthday; }
  public String getPassword() { return this.password; }
  public boolean isEmailVerified() { return this.emailVerified; }
  public boolean isDisabled() { return this.disabled; }
  public Image getAvatar() { return this.avatar; }

  // Date-related getters used by PropertyValueFactory in tables
  public String getCreatedAt() { return this.createdAt; }
  public String getLastModifiedDate() { return this.lastModifiedDate; }
  public String getLastLoginAt() { return this.lastLoginAt; }

  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public void setEmail(String email) { this.email = email; }
  public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
  public void setBirthday(String birthday) { this.birthday = birthday; }
  public void setUid(String uid) { this.uid = uid; }
  public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; if (photoUrl != null && !photoUrl.isEmpty()) setAvatar(photoUrl); }
  public void setAdmin(boolean admin) {
    this.admin = admin;
    if (admin) this.role = "ADMIN";
    else if (this.role == null || "ADMIN".equalsIgnoreCase(this.role)) this.role = "USER";
  }
  public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
  public void setDisabled(boolean disabled) { this.disabled = disabled; }
  public void setPassword(String password) { this.password = password; }

  private Task<Image> getImageTask(String photoUrl) {
    Task<Image> loadImageTask = new Task<Image>() {
      @Override
      protected Image call() {
        return new Image(photoUrl, true);
      }
    };
    loadImageTask.setOnRunning(event -> {
    });
    loadImageTask.setOnSucceeded(event -> {
      Image image = loadImageTask.getValue();
      if (image.getException() == null) {
        avatar = image;
      } else {
        //  System.err.println("Failed to load image from photoUrl: " + photoUrl);
      }
    });
    loadImageTask.setOnFailed(event -> {
//      System.err.println(
//          "Exception while loading image from photoUrl: " + loadImageTask.getException()
//              .getMessage());
    });
    return loadImageTask;
  }

}
