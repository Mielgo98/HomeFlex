package com.example.demo.chatbot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.chatbot.model.DocumentEmbedding;
import com.example.demo.chatbot.model.EntityType;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {

    List<DocumentEmbedding> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);
    
    @Query(value = "SELECT * FROM document_embeddings WHERE entity_type = :entityType", nativeQuery = true)
    List<DocumentEmbedding> findByEntityType(@Param("entityType") String entityType);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM document_embeddings WHERE entity_type = :entityType AND entity_id = :entityId", nativeQuery = true)
    void deleteByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);
    
    @Query(value = "SELECT * FROM document_embeddings ORDER BY embedding <-> :embedding LIMIT :limit", nativeQuery = true)
    List<DocumentEmbedding> findNearest(@Param("embedding") float[] embedding, @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM document_embeddings WHERE entity_type = :entityType ORDER BY embedding <-> :embedding LIMIT :limit", nativeQuery = true)
    List<DocumentEmbedding> findNearestByEntityType(@Param("embedding") float[] embedding, @Param("entityType") String entityType, @Param("limit") int limit);
}