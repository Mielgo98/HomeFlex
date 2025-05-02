package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeObject;
import com.stripe.net.ApiResource;

import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {
    private static final Logger logger = LoggerFactory.getLogger(StripeConfig.class);

    /**
     * Clave secreta de Stripe (test o live según tu properties).
     */
    @Value("${pago.api.key}")
    private String stripeApiKey;

    /**
     * Secreto de firma para verificar webhooks.
     */
    @Value("${pago.webhook.secret}")
    private String webhookSecret;
    
    /**
     * URL de la aplicación para redirecciones.
     */
    @Value("${app.url}")
    private String appUrl;
    
    /**
     * Indica si estamos en modo de prueba o producción.
     */
    @Value("${pago.use.sandbox}")
    private boolean useSandbox;

    /**
     * Al arrancar la aplicación, inicializa Stripe.apiKey.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        
        if (useSandbox) {
            // Configuración adicional para entorno de desarrollo
            logger.info("Inicializando Stripe en modo de PRUEBA");
            Stripe.setAppInfo(
                "HomeFlex", 
                "1.0.0", 
                "https://homeflex.example.com"
            );
            // Opcional: desactivar telemetría en desarrollo
            Stripe.enableTelemetry = false;
        } else {
            logger.info("Inicializando Stripe en modo de PRODUCCIÓN");
        }
        
        // Verificación simplificada
        try {
            logger.info("Clave API configurada: {}...", 
                stripeApiKey.substring(0, Math.min(8, stripeApiKey.length())) + "********");
            logger.info("Stripe configurado correctamente");
        } catch (Exception e) {
            logger.error("Error al configurar Stripe: {}", e.getMessage());
        }
    }

    /**
     * Para usarlo en tu controlador de webhook:
     * @return la clave para verificar la firma de eventos.
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }
    
    /**
     * Indica si estamos en modo sandbox (pruebas)
     * @return true si estamos en sandbox, false en producción
     */
    public boolean isUseSandbox() {
        return useSandbox;
    }
    
    /**
     * URL base de la aplicación
     * @return URL base configurada
     */
    public String getAppUrl() {
        return appUrl;
    }
}