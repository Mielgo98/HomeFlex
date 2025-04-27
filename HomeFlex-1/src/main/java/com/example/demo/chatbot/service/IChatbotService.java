package com.example.demo.chatbot.service;

import com.example.demo.chatbot.model.Answer;
import com.example.demo.chatbot.model.EntityType;

public interface IChatbotService {
    Answer getAnswer(String question, EntityType entityType);
    
    void updateVectorStore();
    
    void updatePropertyInVectorStore(Long propertyId);
    
    void removePropertyFromVectorStore(Long propertyId);
}