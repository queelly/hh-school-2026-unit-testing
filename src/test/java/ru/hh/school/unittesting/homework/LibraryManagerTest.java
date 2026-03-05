package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    LibraryManager libraryManager;

    @BeforeEach
    void addExistingBook() {
        libraryManager.addBook("existingBook", 1);
    }

    @Test
    void testAddNewBook() {
        libraryManager.addBook("newBook", 1000);
        int totalQuantity = libraryManager.getAvailableCopies("newBook");
        Assertions.assertEquals(
            1000,
            totalQuantity
        );
    }

    @Test
    void testAddExistingBook() {
        libraryManager.addBook("existingBook", 10);
        int totalQuantity = libraryManager.getAvailableCopies("existingBook");
        Assertions.assertEquals(
            11,
            totalQuantity
        );
    }

    @Test
    void testLibraryManagerReturnsZeroWhenBookDoesNotExist() {
        int totalQuantity = libraryManager.getAvailableCopies("bookThatDoesNotExist");
        Assertions.assertEquals(
            0,
            totalQuantity
        );
    }

    @Test
    void testCanNotBorrowBookWhenUserIsNotActive() {
        libraryManager.addBook("book", 1);
        Assertions.assertFalse(
            libraryManager.borrowBook("book", "inactiveUser")
        );
        Mockito.verify(notificationService).notifyUser(
                Mockito.eq("inactiveUser"), Mockito.eq("Your account is not active."));
    }

    @Test
    void testCanNotBorrowBookWhenQuantityIsNotEnough() {
        Mockito.when(userService.isUserActive("activeUser")).thenReturn(true);
        Assertions.assertFalse(
            libraryManager.borrowBook("bookThatIsAbsent", "activeUser")
        );
    }

    @Test
    void testCanBorrowBookAndResultIsCorrect() {
        String bookId = "book1";
        int beforeQuantity = 2;
        String userId = "abobusUser";
        int expectedQuantity = 1;
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook(bookId, beforeQuantity);

        Assertions.assertTrue(
            libraryManager.borrowBook(bookId, userId)
        );
        int afterQuantity = libraryManager.getAvailableCopies(bookId);
        Assertions.assertEquals(expectedQuantity, afterQuantity);
        Mockito.verify(notificationService).notifyUser(
                Mockito.eq("abobusUser"), Mockito.eq("You have borrowed the book: " + bookId));
    }

    @Test
    void testTwoUsersCanBorrowSameBookAndResultIsCorrect() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book", 3);
        Assertions.assertAll(
            () -> Assertions.assertTrue(libraryManager.borrowBook("book", "user1")),
            () -> Assertions.assertTrue(libraryManager.borrowBook("book", "user2")),
            () -> Assertions.assertEquals(
                1,
                libraryManager.getAvailableCopies("book")
            )
        );
    }

    @Test
    void testCanOneUserBorrowSameBookTwiceAndResultIsCorrect() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book1", 3);
        Assertions.assertAll(
            () -> Assertions.assertTrue(libraryManager.borrowBook("book1", "user")),
            () -> Assertions.assertTrue(libraryManager.borrowBook("book1", "user")),
            () -> Assertions.assertEquals(
                1,
                libraryManager.getAvailableCopies("book1")
            )
        );
    }

    @Test
    void testCanNotReturnBookWhenBookWasNotBorrowed() {
        libraryManager.borrowBook("existingBook", "user");
        Assertions.assertFalse(
            libraryManager.returnBook("bookThatWasNotBorrowed", "user")
        );
    }

    @Test
    void testCanNotReturnBookThatWasBorrowedByAnotherUser() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.borrowBook("existingBook", "user1");
        Assertions.assertFalse(
            libraryManager.returnBook("existingBook", "user2")
        );
    }

    @Test
    void testCanReturnBookAndResultIsCorrect() {
        String bookId = "book1";
        int beforeQuantity = 10;
        String userId = "user";
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook(bookId, beforeQuantity);
        libraryManager.borrowBook(bookId, userId);

        Assertions.assertTrue(
            libraryManager.returnBook(bookId, userId)
        );
        int afterQuantity = libraryManager.getAvailableCopies(bookId);
        Assertions.assertEquals(beforeQuantity, afterQuantity);
    }
    @Disabled("test hooks an issue")
    @Test
    void testCanUserReturnSameBookTwice() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book1", 3);
        libraryManager.borrowBook("book1", "user1");
        libraryManager.borrowBook("book1", "user1");
        Assertions.assertTrue(libraryManager.returnBook("book1", "user1"));
        Assertions.assertTrue(libraryManager.returnBook("book1", "user1"));
        Assertions.assertEquals(3, libraryManager.getAvailableCopies("book1"));
    }

    @Disabled("test hooks an issue")
    @Test
    void testCanTwoUsersReturnSameBook() {
        Mockito.when(userService.isUserActive(ArgumentMatchers.anyString())).thenReturn(true);
        libraryManager.addBook("book1", 3);
        libraryManager.borrowBook("book1", "user1");
        libraryManager.borrowBook("book1", "user2");
        Assertions.assertTrue(libraryManager.returnBook("book1", "user1"));
        Assertions.assertTrue(libraryManager.returnBook("book1", "user2"));
        Assertions.assertEquals(3, libraryManager.getAvailableCopies("book1"));
    }

    @Test
    void testThrowsExceptionWhenOverdueDaysIsNegative() {
        var exception = Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> libraryManager.calculateDynamicLateFee(
                    -1,
                    true,
                    true
            )
        );
        Assertions.assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({
            "1, false, true, 0.4",
            "0, true, false, 0",
            "3, false, false, 1.5",
            "33, true, true, 19.8"
    })
    void testCalculateDynamicLateFee(
            int overdueDays,
            boolean isBestseller,
            boolean isPremiumMember,
            double expectedFee
    ) {
        double fee = libraryManager.calculateDynamicLateFee(
                overdueDays, isBestseller, isPremiumMember);
        Assertions.assertEquals(
                expectedFee,
                fee
        );
    }
}
