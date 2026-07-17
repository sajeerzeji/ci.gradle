package com.demo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A simple POJO that uses Lombok-generated constructor and getter.
 * This class exercises the annotationProcessor dependency during compilation.
 */
@RequiredArgsConstructor
@Getter
public class Greeting {
    private final String message;
}
