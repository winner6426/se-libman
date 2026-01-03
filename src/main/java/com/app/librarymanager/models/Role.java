package com.app.librarymanager.models;

public enum Role {
  GUEST,
  USER,
  READER,
  LIBRARIAN,
  ADMIN,
  ACCOUNTANT;

  public static Role fromString(String s) {
    if (s == null) return null;
    try {
      return Role.valueOf(s.trim().toUpperCase());
    } catch (Exception e) {
      return null;
    }
  }
}
