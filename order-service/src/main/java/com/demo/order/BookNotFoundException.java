package com.demo.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class BookNotFoundException extends ResponseStatusException {

    public BookNotFoundException(Long bookId) {
        super(HttpStatus.NOT_FOUND, "Book with id " + bookId + " does not exist");
    }
}