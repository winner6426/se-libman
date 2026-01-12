package com.app.librarymanager.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.cloud.FirestoreClient;
import com.app.librarymanager.utils.EnvLoader;
import java.io.InputStream;
import java.io.IOException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;

public class Firebase {

  private static Firebase instance;
  private static final String apiKey = EnvLoader.get("FIREBASE_API_KEY");
  private static final String authDomain = EnvLoader.get("FIREBASE_AUTH_DOMAIN");
  private static final String databaseURL = EnvLoader.get("FIREBASE_DATABASE_URL");
  private static final String projectId = EnvLoader.get("FIREBASE_PROJECT_ID");
  private static final String storageBucket = EnvLoader.get("FIREBASE_STORAGE_BUCKET");
  private static final String messagingSenderId = EnvLoader.get("FIREBASE_MESSAGING_SENDER_ID");
  private static final String appId = EnvLoader.get("FIREBASE_APP_ID");

  private FirebaseApp app;
  private Firestore db;


  private Firebase() {
    try {
      // Load service account from classpath
      String serviceAccountPath = EnvLoader.get("FIREBASE_SERVICE_ACCOUNT_PATH");
      if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
        serviceAccountPath = "/serviceAccountKey.json"; // Default to classpath resource
      }
      
      InputStream serviceAccount = getClass().getResourceAsStream(serviceAccountPath);
      if (serviceAccount == null) {
        throw new IllegalArgumentException(
            "Service account file not found in classpath: " + serviceAccountPath);
      }
      FirebaseOptions options = new FirebaseOptions.Builder()
          .setCredentials(GoogleCredentials.fromStream(serviceAccount))
          .setDatabaseUrl(databaseURL)
          .build();
      app = FirebaseApp.initializeApp(options);
      db = FirestoreClient.getFirestore();
      System.out.println("Successfully initialized Firebase!");
    } catch (IOException e) {
      System.err.println("Error when initializing Firebase: " + e.getMessage());
      e.printStackTrace();
      throw new RuntimeException("Failed to initialize Firebase. Please check your .env file and Firebase service account file.", e);
    }
  }

  public static synchronized Firebase getInstance() {
    if (instance == null || instance.app == null) {
      instance = new Firebase();
    }
    return instance;
  }

  public static FirebaseApp getApp() {
    if (instance == null || instance.app == null) {
      instance = new Firebase();
    }
    return instance.app;
  }

  public static Firestore getDb() {
    if (instance == null || instance.app == null) {
      instance = new Firebase();
    }
    return instance.db;
  }

  // Explicit getters to avoid reliance on Lombok during compilation
  public static String getApiKey() { return apiKey; }
  public static String getAuthDomain() { return authDomain; }
}
