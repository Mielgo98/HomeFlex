package com.example.demo.chatbot.model;

public record Answer(String answer, ResponseType responseType) {
    // Constructor conveniente que solo requiere la respuesta y asume SUCCESS
    public Answer(String answer) {
        this(answer, ResponseType.SUCCESS);
    }
}