package com.app.librarymanager.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.cloud.FirestoreClient;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.FileInputStream;
import java.io.IOException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;

public class Firebase {

  private static Firebase instance;
  private static final Dotenv dotenv = Dotenv.load();
  private static final String apiKey = dotenv.get("FIREBASE_API_KEY");
  private static final String authDomain = dotenv.get("FIREBASE_AUTH_DOMAIN");
  private static final String databaseURL = dotenv.get("FIREBASE_DATABASE_URL");
  private static final String projectId = dotenv.get("FIREBASE_PROJECT_ID");
  private static final String storageBucket = dotenv.get("FIREBASE_STORAGE_BUCKET");
  private static final String messagingSenderId = dotenv.get("FIREBASE_MESSAGING_SENDER_ID");
  private static final String appId = dotenv.get("FIREBASE_APP_ID");

  private FirebaseApp app;
  private Firestore db;

  private Firebase() {
    Dotenv dotenv = Dotenv.load();
    try {
      String serviceAccountPath = dotenv.get("FIREBASE_SERVICE_ACCOUNT_PATH");
      if (serviceAccountPath == null || serviceAccountPath.isEmpty()) {
        throw new IllegalArgumentException(
            "FIREBASE_SERVICE_ACCOUNT_PATH is not set in .env file. Please create a .env file with the path to your Firebase service account JSON file.");
      }
      FileInputStream serviceAccount = new FileInputStream(serviceAccountPath);
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
