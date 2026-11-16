package com.demo.bookstore_app.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.bookstore_app.models.Book;
import com.demo.bookstore_app.repositories.BookRepository;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void getBookByIdReturnsBook() throws Exception {
        Book book = new Book("The Little Prince", 100.0, "Antoine de Saint-Exupery", "Novel", 1943);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        Book result = bookService.getBookById(1L);

        assertEquals("The Little Prince", result.getBookName());
    }

    @Test
    void getBookByIdThrowsWhenBookDoesNotExist() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> bookService.getBookById(99L));

        assertEquals("Cannot find book with this ID!", ex.getMessage());
    }

    @Test
    void getBookByIdThrowsOnInvalidId() {
        Exception ex = assertThrows(Exception.class, () -> bookService.getBookById(null));

        assertEquals("Invalid ID!", ex.getMessage());
        verifyNoInteractions(bookRepository);
    }

    @Test
    void getAllBooksThrowsWhenListIsEmpty() {
        when(bookRepository.findAll()).thenReturn(new ArrayList<>());

        assertThrows(Exception.class, () -> bookService.getAllBooks());
    }

    @Test
    void updateBookChangesFieldsAndSaves() throws Exception {
        Long id = 1L;
        Book existing = new Book("The Little Prince", 100.0, "Antoine de Saint-Exupery", "Novel", 1943);
        existing.setBookId(id);
        Book update = new Book("Vol de nuit", 120.0, "Antoine de Saint-Exupery", "Novel", 1931);

        when(bookRepository.findById(id)).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book result = bookService.updateBook(id, update);

        assertNotNull(result);
        assertEquals("Vol de nuit", result.getBookName());
        assertEquals(120.0, result.getBookPrice());
        verify(bookRepository).findById(id);
        verify(bookRepository).save(existing);
    }

    @Test
    void getBookCountByGenreBuildsMapFromRepositoryRows() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { "Novel", 2L });
        rows.add(new Object[] { "Technology", 1L });
        when(bookRepository.countBooksByGenre()).thenReturn(rows);

        Map<String, Long> result = bookService.getBookCountByGenre();

        assertEquals(2, result.size());
        assertEquals(2L, result.get("Novel").longValue());
        assertEquals(1L, result.get("Technology").longValue());
    }
}