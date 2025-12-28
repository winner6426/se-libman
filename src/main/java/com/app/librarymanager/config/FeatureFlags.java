package com.app.librarymanager.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Simple feature flag helper. Flags are read from environment or .env file.
 * To enable Loan Records admin feature set FEATURE_LOAN_RECORDS=true
 */
public final class FeatureFlags {

  private static final Dotenv dotenv = Dotenv.load();

  private FeatureFlags() {}

  public static boolean isLoanRecordsEnabled() {
    String v = dotenv.get("FEATURE_LOAN_RECORDS");
    if (v == null) v = System.getenv("FEATURE_LOAN_RECORDS");
    return v != null && (v.equalsIgnoreCase("1") || v.equalsIgnoreCase("true") || v.equalsIgnoreCase("yes"));
  }
}
