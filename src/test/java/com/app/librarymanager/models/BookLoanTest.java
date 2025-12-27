package com.app.librarymanager.models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import org.junit.jupiter.api.Test;

public class BookLoanTest {

  @Test
  public void newBookLoanShouldBePending() {
    BookLoan loan = new BookLoan("user1", "book1");
    assertEquals(BookLoan.Status.PENDING, loan.getStatus());
  }

  @Test
  public void offlineLoanShouldBeAvailable() {
    Date now = new Date();
    BookLoan loan = new BookLoan("user1", "book1", now, now, 2);
    assertEquals(BookLoan.Status.AVAILABLE, loan.getStatus());
  }
}
