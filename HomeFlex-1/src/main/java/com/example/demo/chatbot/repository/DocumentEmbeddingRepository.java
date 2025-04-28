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

	  // Borra embeddings de una entidad
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM DocumentEmbedding d
         WHERE d.entityType = :entityType
           AND d.entityId   = :entityId
        """)
    void deleteByEntityTypeAndEntityId(
        @Param("entityType") EntityType entityType,
        @Param("entityId")   Long       entityId
    );
	
    /**
     * Vecinos más cercanos dentro de un tipo (consulta nativa).
     * @param entityType     el nombre del enum (p.ej. "PROPERTY")
     * @param vectorLiteral  el literal "[v1,v2,…]" casteado luego a vector
     * @param limit          cuántos vecinos devolver
     */
    @Query(value = """
        SELECT *
          FROM document_embeddings
         WHERE entity_type = :entityType
         ORDER BY embedding <-> CAST(:vectorLiteral AS vector)
         LIMIT :limit
        """, nativeQuery = true)
    List<DocumentEmbedding> findNearestByEntityTypeNative(
        @Param("entityType")    String entityType,
        @Param("vectorLiteral") String vectorLiteral,
        @Param("limit")         int    limit
    );

    /**
     * Vecinos más cercanos en toda la tabla (consulta nativa).
     */
    @Query(value = """
        SELECT *
          FROM document_embeddings
         ORDER BY embedding <-> CAST(:vectorLiteral AS vector)
         LIMIT :limit
        """, nativeQuery = true)
    List<DocumentEmbedding> findNearestNative(
        @Param("vectorLiteral") String vectorLiteral,
        @Param("limit")         int    limit
    );
}
