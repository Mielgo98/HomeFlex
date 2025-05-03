package com.example.demo.admin.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log_accesos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogAccesoVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(length = 100)
    private String usuario;

    @Column(nullable = false, length = 100)
    private String accion;

    @Column(length = 45)
    private String ip;

    @Column(length = 255)
    private String url;
}
