package com.demo.author.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authors")
public class AuthorController {

    @GetMapping
    public List<AuthorDto> findAll() {
        return List.of(
                new AuthorDto(1L, "Antoine de Saint-Exupéry"),
                new AuthorDto(2L, "Robert C. Martin"));
    }

    public record AuthorDto(Long id, String name) {}
}