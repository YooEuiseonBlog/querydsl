package study.querydsl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class HelloController {

    @GetMapping(value = {"/hello", "/hello/{id}"})
    public String hello(@PathVariable(value = "id", required = false)Optional<String> id) {
        return id.orElse("hello!");
    }
}
