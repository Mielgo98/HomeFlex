package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.stripe.Stripe;

import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {

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
     * Al arrancar la aplicación, inicializa Stripe.apiKey.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Para usarlo en tu controlador de webhook:
     * @return la clave para verificar la firma de eventos.
     */
    public String getWebhookSecret() {
        return webhookSecret;
    }
}
