package com.freshmind.backend.ai;

public class RecipeGenerationException extends RuntimeException {

    public RecipeGenerationException(String message) {
        super(message);
    }

    public RecipeGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
