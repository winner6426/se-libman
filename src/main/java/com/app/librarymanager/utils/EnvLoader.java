package com.app.librarymanager.utils;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Utility class to load .env file from either file system (IDE) or classpath (JAR).
 */
public class EnvLoader {
  private static Dotenv dotenv;

  static {
    try {
      // Try to load from current directory first (for IDE)
      dotenv = Dotenv.configure().ignoreIfMissing().load();
    } catch (Exception e) {
      try {
        // If that fails, try to load from classpath (for JAR)
        dotenv = Dotenv.configure().directory("/").ignoreIfMissing().load();
      } catch (Exception ex) {
        System.err.println("Failed to load .env file: " + ex.getMessage());
        dotenv = Dotenv.configure().ignoreIfMissing().load();
      }
    }
  }

  public static Dotenv get() {
    return dotenv;
  }

  public static String get(String key) {
    return dotenv.get(key);
  }
}
