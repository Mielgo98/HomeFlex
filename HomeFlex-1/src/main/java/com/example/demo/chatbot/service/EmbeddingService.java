package com.example.demo.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.chatbot.model.DocumentEmbedding;
import com.example.demo.chatbot.model.EntityType;
import com.example.demo.chatbot.repository.DocumentEmbeddingRepository;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.repository.PropiedadRepository;

@Service
public class EmbeddingService {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private DocumentEmbeddingRepository embeddingRepository;

    @Autowired
    private PropiedadRepository propiedadRepository;

    @Autowired
    private ChatbotDataService chatbotDataService;

    @Transactional
    public void updateAllPropertyEmbeddings() {
        propiedadRepository.findByActivoTrue()
            .forEach(p -> updatePropertyEmbedding(p.getId()));
    }

    @Transactional
    public void updatePropertyEmbedding(Long propiedadId) {
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
            .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));

        // Borrar antiguos
        embeddingRepository.deleteByEntityTypeAndEntityId(
            EntityType.PROPERTY, propiedadId);

        // Texto y vector
        String text = chatbotDataService.convertPropertyToText(propiedad);
        float[] vector = embeddingModel.embed(text);

        // Guardar nuevo embedding
        DocumentEmbedding doc = new DocumentEmbedding();
        doc.setContent(text);
        doc.setEntityType(EntityType.PROPERTY);
        doc.setEntityId(propiedadId);
        doc.setEmbedding(vector);
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        embeddingRepository.save(doc);
    }

    @Transactional
    public void removePropertyEmbedding(Long propiedadId) {
        embeddingRepository.deleteByEntityTypeAndEntityId(
            EntityType.PROPERTY, propiedadId);
    }

    public List<DocumentEmbedding> similaritySearch(
            String query, EntityType entityType, int limit) {

        // Vector de la pregunta
        float[] qv = embeddingModel.embed(query);
        String literal = toPgVectorLiteral(qv);

        if (entityType != null && entityType != EntityType.ALL) {
            return embeddingRepository.findNearestByEntityTypeNative(
                entityType.name(), literal, limit
            );
        } else {
            return embeddingRepository.findNearestNative(literal, limit);
        }
    }

    /** Convierte float[] → "[v1,v2,v3,...]" */
    private String toPgVectorLiteral(float[] emb) {
        String inside = IntStream.range(0, emb.length)
            .mapToObj(i -> Float.toString(emb[i]))
            .collect(Collectors.joining(","));
        return "[" + inside + "]";
    }
}
