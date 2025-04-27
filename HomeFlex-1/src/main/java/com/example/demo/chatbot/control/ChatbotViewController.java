package com.example.demo.chatbot.control;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/chatbot")
public class ChatbotViewController {
    
    @GetMapping
    public String chatbotView() {
        return "chatbot/chatbot";
    }
}