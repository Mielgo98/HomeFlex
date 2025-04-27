package com.example.demo.chatbot.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.chatbot.model.Answer;
import com.example.demo.chatbot.model.Question;
import com.example.demo.chatbot.service.IChatbotService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/chatbot")
@Tag(name = "Chatbot", description = "API para el chatbot de HomeFlex")
public class ChatbotController {
    
    @Autowired
    private IChatbotService chatbotService;
    
    @PostMapping("/ask")
    @Operation(summary = "Realiza una pregunta al chatbot", 
              description = "Envía una pregunta y opcionalmente el tipo de entidad sobre la que consultar")
    public Answer askQuestion(@RequestBody Question question) {
        return chatbotService.getAnswer(question.question(), question.entityType());
    }
}