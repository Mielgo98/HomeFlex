package com.example.demo.chatbot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

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
        logger.info("Actualizando embeddings de todas las propiedades activas");
        
        List<PropiedadVO> propiedadesActivas = propiedadRepository.findByActivoTrue();
        logger.info("Se encontraron {} propiedades activas para actualizar", propiedadesActivas.size());
        
        for (PropiedadVO propiedad : propiedadesActivas) {
            try {
                updatePropertyEmbedding(propiedad.getId());
            } catch (Exception e) {
                logger.error("Error al actualizar embedding para propiedad ID {}: {}", propiedad.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public void updatePropertyEmbedding(Long propiedadId) {
        logger.info("Actualizando embedding para propiedad ID: {}", propiedadId);
        
        try {
            PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada con ID: " + propiedadId));

            // Borrar embeddings anteriores
            logger.debug("Eliminando embeddings anteriores para la propiedad");
            embeddingRepository.deleteByEntityTypeAndEntityId(
                EntityType.PROPERTY, propiedadId);

            // Generar texto para el embedding
            String text = chatbotDataService.convertPropertyToText(propiedad);
            logger.debug("Texto generado para embedding: {}", text);
            
            // Generar vector de embedding
            float[] vector = embeddingModel.embed(text);
            logger.debug("Vector de embedding generado con {} dimensiones", vector.length);

            // Guardar nuevo embedding
            DocumentEmbedding doc = new DocumentEmbedding();
            doc.setContent(text);
            doc.setEntityType(EntityType.PROPERTY);
            doc.setEntityId(propiedadId);
            doc.setEmbedding(vector);
            doc.setCreatedAt(LocalDateTime.now());
            doc.setUpdatedAt(LocalDateTime.now());
            
            embeddingRepository.save(doc);
            logger.info("Embedding guardado correctamente para propiedad ID: {}", propiedadId);
        } catch (Exception e) {
            logger.error("Error al actualizar embedding: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar embedding para propiedad: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void removePropertyEmbedding(Long propiedadId) {
        logger.info("Eliminando embedding para propiedad ID: {}", propiedadId);
        embeddingRepository.deleteByEntityTypeAndEntityId(
            EntityType.PROPERTY, propiedadId);
    }

    public List<DocumentEmbedding> similaritySearch(
            String query, EntityType entityType, int limit) {
        logger.info("Realizando búsqueda por similitud. Query: '{}', EntityType: {}, Limit: {}", 
                   query, entityType, limit);

        try {
            // Generar vector de la consulta
            float[] queryVector = embeddingModel.embed(query);
            String pgVectorLiteral = toPgVectorLiteral(queryVector);
            logger.debug("Vector PG generado para consulta");

            List<DocumentEmbedding> results;
            
            // Realizar la búsqueda según el tipo de entidad
            if (entityType != null && entityType != EntityType.ALL) {
                logger.debug("Buscando embeddings solo para tipo: {}", entityType);
                results = embeddingRepository.findNearestByEntityTypeNative(
                    entityType.getCode(), pgVectorLiteral, limit
                );
            } else {
                logger.debug("Buscando embeddings para todos los tipos");
                results = embeddingRepository.findNearestNative(pgVectorLiteral, limit);
            }
            
            logger.info("Búsqueda completada. Encontrados {} resultados", results.size());
            return results;
        } catch (Exception e) {
            logger.error("Error en búsqueda por similitud: {}", e.getMessage(), e);
            throw new RuntimeException("Error en búsqueda por similitud: " + e.getMessage(), e);
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