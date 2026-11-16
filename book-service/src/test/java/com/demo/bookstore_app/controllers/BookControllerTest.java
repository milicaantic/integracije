package com.demo.bookstore_app.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.demo.bookstore_app.models.Book;
import com.demo.bookstore_app.services.BookService;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookService bookService;

    @Test
    void getAllBooksReturnsList() throws Exception {
        Book book = new Book("Clean Code", 50.0, "Robert C. Martin", "Technology", 2008);
        when(bookService.getAllBooks()).thenReturn(List.of(book));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookName").value("Clean Code"));
    }

    @Test
    void countByGenreReturnsMap() throws Exception {
        when(bookService.getBookCountByGenre()).thenReturn(Map.of("Novel", 2L, "Technology", 1L));

        mockMvc.perform(get("/api/books/count-by-genre"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Novel").value(2))
                .andExpect(jsonPath("$.Technology").value(1));
    }

    @Test
    void priceRangeWithMinGreaterThanMaxReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/books/price").param("minPrice", "100").param("maxPrice", "10"))
                .andExpect(status().isBadRequest());
    }
}