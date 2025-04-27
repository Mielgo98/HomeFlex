package com.example.demo.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
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
    
    /**
     * Actualiza los embeddings para todas las propiedades activas
     */
    @Transactional
    public void updateAllPropertyEmbeddings() {
        List<PropiedadVO> propiedades = propiedadRepository.findByActivoTrue();
        
        propiedades.forEach(propiedad -> {
            updatePropertyEmbedding(propiedad.getId());
        });
    }
    
    /**
     * Actualiza los embeddings para una propiedad específica
     */
    @Transactional
    public void updatePropertyEmbedding(Long propiedadId) {
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        // Primero eliminamos los embeddings existentes
        embeddingRepository.deleteByEntityTypeAndEntityId(EntityType.PROPERTY.getCode(), propiedadId);
        
        // Convertimos la propiedad a texto
        String propertyText = chatbotDataService.convertPropertyToText(propiedad);
        
        // Generamos el embedding
        EmbeddingResponse response = embeddingModel.embed(propertyText);
        float[] embedding = response.getEmbedding().stream()
                .map(Double::floatValue)
                .collect(java.util.stream.Collectors.toList())
                .stream()
                .mapToFloat(Float::floatValue)
                .toArray();
        
        // Guardamos el embedding
        DocumentEmbedding docEmbedding = new DocumentEmbedding();
        docEmbedding.setContent(propertyText);
        docEmbedding.setEntityType(EntityType.PROPERTY);
        docEmbedding.setEntityId(propiedadId);
        docEmbedding.setEmbedding(embedding);
        docEmbedding.setCreatedAt(LocalDateTime.now());
        docEmbedding.setUpdatedAt(LocalDateTime.now());
        
        embeddingRepository.save(docEmbedding);
    }
    
    /**
     * Elimina los embeddings de una propiedad
     */
    @Transactional
    public void removePropertyEmbedding(Long propiedadId) {
        embeddingRepository.deleteByEntityTypeAndEntityId(EntityType.PROPERTY.getCode(), propiedadId);
    }
    
    /**
     * Busca documentos similares a un texto
     */
    public List<DocumentEmbedding> similaritySearch(String query, EntityType entityType, int limit) {
        // Generamos el embedding para la consulta
        EmbeddingResponse response = embeddingModel.embed(query);
        float[] embedding = response.getEmbedding().stream()
                .map(Double::floatValue)
                .collect(java.util.stream.Collectors.toList())
                .stream()
                .mapToFloat(Float::floatValue)
                .toArray();
        
        // Realizamos la búsqueda vectorial
        if (entityType != null && entityType != EntityType.ALL) {
            return embeddingRepository.findNearestByEntityType(embedding, entityType.getCode(), limit);
        } else {
            return embeddingRepository.findNearest(embedding, limit);
        }
    }
}