package com.example.demo.chatbot.control;

import com.example.demo.chatbot.model.Answer;
import com.example.demo.chatbot.model.Question;
import com.example.demo.chatbot.service.IChatbotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);

    @Autowired
    private IChatbotService chatbotService;

    @PostMapping("/ask")
    public Answer askQuestion(@RequestBody Question question) {
        log.info("Pregunta recibida: {}", question.question());
        Answer answer = chatbotService.getAnswer(question.question(), question.entityType());
        log.info("Respuesta enviada: {} ({})", answer.answer(), answer.responseType());
        return answer;
    }
}
