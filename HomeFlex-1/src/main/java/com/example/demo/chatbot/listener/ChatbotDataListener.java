package com.example.demo.chatbot.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.demo.chatbot.service.IChatbotService;
import com.example.demo.propiedad.event.PropiedadCreatedEvent;
import com.example.demo.propiedad.event.PropiedadDeletedEvent;
import com.example.demo.propiedad.event.PropiedadUpdatedEvent;

@Component
public class ChatbotDataListener {

    @Autowired
    private IChatbotService chatbotService;
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropiedadCreatedEvent(PropiedadCreatedEvent event) {
        chatbotService.updatePropertyInVectorStore(event.getPropiedadId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropiedadUpdatedEvent(PropiedadUpdatedEvent event) {
        chatbotService.updatePropertyInVectorStore(event.getPropiedadId());
    }
    
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePropiedadDeletedEvent(PropiedadDeletedEvent event) {
        chatbotService.removePropertyFromVectorStore(event.getPropiedadId());
    }
}