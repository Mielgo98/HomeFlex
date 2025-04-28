package com.example.demo.chatbot.service;

import com.example.demo.chatbot.model.Answer;
import com.example.demo.chatbot.model.DocumentEmbedding;
import com.example.demo.chatbot.model.EntityType;
import com.example.demo.chatbot.model.ResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotServiceImpl implements IChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotServiceImpl.class);

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
        logger.info("→ Procesando pregunta [{}] para entidad [{}]", question, entityType);
        try {
            // Aseguramos que entityType no sea null
            if (entityType == null) {
                entityType = EntityType.PROPERTY; // Valor por defecto
            }
            
            // Busca documentos similares en la base de datos vectorial
            List<DocumentEmbedding> similarDocs = embeddingService.similaritySearch(question, entityType, 5);
            logger.debug("  • Documentos similares encontrados: {}", similarDocs.size());

            if (similarDocs.isEmpty()) {
                logger.warn("  • No se encontraron documentos relevantes para la consulta");
                return new Answer("Lo siento, no tengo información sobre esa consulta en nuestra base de datos de propiedades.", ResponseType.NO_INFORMATION);
            }

            List<String> contentList = similarDocs.stream()
                    .map(DocumentEmbedding::getContent)
                    .collect(Collectors.toList());
            logger.debug("  • Contenido de documentos: {}", contentList);

            // Seleccionamos la plantilla adecuada según el tipo de entidad
            Resource template = (entityType == EntityType.PROPERTY)
                    ? propertyPromptTemplate
                    : generalPromptTemplate;

            // Creamos el prompt basado en la plantilla
            PromptTemplate pt = new PromptTemplate(template);
            Prompt prompt = pt.create(Map.of(
                    "input", question,
                    "documents", String.join("\n\n", contentList)
            ));

            // Llamamos al modelo de IA para obtener respuesta
            ChatResponse response = chatModel.call(prompt);
            String responseText = response.getResult().getOutput().getText();
            logger.info("← Texto de respuesta: {}", responseText);

            ResponseType responseType = determineResponseType(responseText);
            logger.debug("  • Tipo de respuesta: {}", responseType);

            return new Answer(responseText, responseType);
        } catch (Exception e) {
            logger.error("⚠️ Error al procesar la pregunta", e);
            return new Answer("Lo siento, ocurrió un error al procesar tu consulta: " + e.getMessage(),
                              ResponseType.ERROR);
        }
    }

    private ResponseType determineResponseType(String response) {
        String lower = response.toLowerCase();
        if (lower.contains("no tengo información") ||
            lower.contains("no dispongo de datos") ||
            lower.contains("no puedo encontrar") ||
            lower.contains("no tengo detalles")) {
            return ResponseType.NO_INFORMATION;
        } else if (lower.contains("podrías especificar") ||
                   lower.contains("necesito más detalles") ||
                   lower.contains("puedes aclarar")) {
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