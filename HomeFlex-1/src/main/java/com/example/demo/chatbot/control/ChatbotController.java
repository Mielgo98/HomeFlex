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
        log.info("Pregunta recibida: '{}', tipo: {}", question.question(), question.entityType());
        
        // Verificar que los datos de entrada sean válidos
        if (question.question() == null || question.question().trim().isEmpty()) {
            log.warn("Se recibió una pregunta vacía");
            return new Answer("Por favor, formula una pregunta válida.", 
                            com.example.demo.chatbot.model.ResponseType.ERROR);
        }
        
        try {
            // Llamar al servicio para obtener la respuesta
            Answer answer = chatbotService.getAnswer(question.question(), question.entityType());
            log.info("Respuesta enviada: '{}' ({})", answer.answer(), answer.responseType());
            return answer;
        } catch (Exception e) {
            log.error("Error al procesar la pregunta: {}", e.getMessage(), e);
            return new Answer("Lo siento, ocurrió un error al procesar tu consulta: " + e.getMessage(),
                            com.example.demo.chatbot.model.ResponseType.ERROR);
        }
    }
}