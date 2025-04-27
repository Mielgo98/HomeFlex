package com.example.demo.chatbot.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.example.demo.chatbot.model.Answer;
import com.example.demo.chatbot.model.DocumentEmbedding;
import com.example.demo.chatbot.model.EntityType;
import com.example.demo.chatbot.model.ResponseType;

@Service
public class ChatbotServiceImpl implements IChatbotService {

    private final ChatModel chatModel;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Value("classpath:/templates/chatbot/propertyPrompt.st")
    private Resource propertyPromptTemplate;
    
    @Value("classpath:/templates/chatbot/generalPrompt.st")
    private Resource generalPromptTemplate;
    
    public ChatbotServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public Answer getAnswer(String question, EntityType entityType) {
        try {
            // Buscar documentos similares usando embeddings
            List<DocumentEmbedding> similarDocs = embeddingService.similaritySearch(question, entityType, 5);
            
            // Si no encontramos documentos relevantes
            if (similarDocs.isEmpty()) {
                return new Answer("Lo siento, no tengo información sobre esa consulta.", ResponseType.NO_INFORMATION);
            }
            
            // Convertir documentos a texto
            List<String> contentList = similarDocs.stream()
                    .map(DocumentEmbedding::getContent)
                    .collect(Collectors.toList());
            
            // Seleccionar la plantilla apropiada según el tipo de entidad
            Resource templateResource = (entityType == EntityType.PROPERTY) 
                                        ? propertyPromptTemplate 
                                        : generalPromptTemplate;
            
            PromptTemplate pt = new PromptTemplate(templateResource);
            Prompt prompt = pt.create(Map.of(
                "input", question, 
                "documents", String.join("\n", contentList)
            ));
            
            ChatResponse response = chatModel.call(prompt);
            String responseText = response.getResult().getOutput().getText();
            
            // Determinar el tipo de respuesta basado en el contenido
            ResponseType responseType = determineResponseType(responseText);
            
            return new Answer(responseText, responseType);
        } catch (Exception e) {
            return new Answer("Lo siento, ocurrió un error al procesar tu consulta: " + e.getMessage(), 
                             ResponseType.ERROR);
        }
    }

    private ResponseType determineResponseType(String response) {
        String lowerResponse = response.toLowerCase();
        
        if (lowerResponse.contains("no tengo información") || 
            lowerResponse.contains("no dispongo de datos") ||
            lowerResponse.contains("no puedo encontrar")) {
            return ResponseType.NO_INFORMATION;
        } else if (lowerResponse.contains("podrías especificar") ||
                   lowerResponse.contains("necesito más detalles") ||
                   lowerResponse.contains("puedes aclarar")) {
            return ResponseType.CLARIFICATION;
        }
        
        return ResponseType.SUCCESS;
    }

    @Override
    public void updatePropertyInVectorStore(Long propertyId) {
        embeddingService.updatePropertyEmbedding(propertyId);
    }

    @Override
    public void removePropertyFromVectorStore(Long propertyId) {
        embeddingService.removePropertyEmbedding(propertyId);
    }

    @Override
    public void updateVectorStore() {
        embeddingService.updateAllPropertyEmbeddings();
    }
}