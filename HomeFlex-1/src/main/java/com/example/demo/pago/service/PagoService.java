package com.example.demo.pago.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.pago.model.PagoRequest;
import com.example.demo.pago.model.PagoResponse;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.model.EstadoReserva;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Servicio para gestionar pagos a través de Stripe (Simulación)
 */
@Service
public class PagoService {

    @Value("${pago.api.key}")
    private String apiKey;
    
    @Value("${pago.webhook.secret}")
    private String webhookSecret;
    
    @Value("${pago.sandbox.url}")
    private String sandboxUrl;
    
    @Value("${pago.use.sandbox}")
    private boolean useSandbox;
    
    private final RestTemplate restTemplate;
    
    public PagoService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * Crea una sesión de pago para una reserva
     */
    public String crearSesionPago(ReservaVO reserva) {
 
        String idSesion = "session_" + UUID.randomUUID().toString().substring(0, 8);
        
        // En un entorno real, se enviará esta información a Stripe y se obtendrá una URL de redirección
        return "/pago/simulador?reservaId=" + reserva.getId() + 
               "&monto=" + reserva.getPrecioTotal() + 
               "&idSesion=" + idSesion;
    }
    
    /**
     * Procesa un pago (simulación)
     */
    public PagoResponse procesarPago(String numeroTarjeta, String cvv, String fechaExpiracion, 
                                     BigDecimal monto, String idSesion) {
        
     
    
        boolean pagoExitoso = validarTarjeta(numeroTarjeta, cvv, fechaExpiracion);
        
        PagoResponse respuesta = new PagoResponse();
        respuesta.setIdSesion(idSesion);
        respuesta.setMonto(monto);
        
        if (pagoExitoso) {
            respuesta.setIdPago("pi_" + System.currentTimeMillis());
            respuesta.setResultado("success");
            respuesta.setMensaje("Pago autorizado");
        } else {
            respuesta.setResultado("failed");
            respuesta.setMensaje("Pago rechazado");
            respuesta.setCodigoError("card_declined");
        }
        
        return respuesta;
    }
    
    /**
     * Realiza una transacción real con Stripe (para implementación completa)
     */
    public PagoResponse realizarTransaccionReal(PagoRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("amount", request.getAmount().multiply(new BigDecimal("100")).intValue()); 
        paymentData.put("currency", request.getCurrency());
        paymentData.put("payment_method", request.getPaymentMethodId());
        paymentData.put("confirm", true);
        paymentData.put("return_url", request.getReturnUrl());
        
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(paymentData, headers);
        
        // Endpoint para crear un pago
        String endpoint = useSandbox ? 
                sandboxUrl + "/v1/payment_intents" : 
                "https://api.stripe.com/v1/payment_intents";
        
        // Realizar la petición a Stripe
        ResponseEntity<PagoResponse> response = 
            restTemplate.postForEntity(endpoint, httpEntity, PagoResponse.class);
        
        return response.getBody();
    }
    
    /**
     * Validación básica de tarjeta (simulación)
     */
    private boolean validarTarjeta(String numeroTarjeta, String cvv, String fechaExpiracion) {
        // Eliminar espacios y guiones
        numeroTarjeta = numeroTarjeta.replaceAll("[\\s-]", "");
        
        // Validar formato básico
        boolean formatoValido = numeroTarjeta.matches("\\d{16}") && // 16 dígitos
                                cvv.matches("\\d{3}") && // 3 dígitos
                                fechaExpiracion.matches("\\d{2}/\\d{2}");
        
        if (!formatoValido) {
            return false;
        }
        
       
        if (numeroTarjeta.endsWith("0000")) {
            return false;
        }
        
        // Algoritmo de Luhn para la validacion de las tarjetas de credito
        int sum = 0;
        boolean alternate = false;
        for (int i = numeroTarjeta.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(numeroTarjeta.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        
        return (sum % 10 == 0);
    }
    
    /**
     * Verifica el estado de un pago
     */
    public PagoResponse verificarPago(String idSesion) {
      
        PagoResponse respuesta = new PagoResponse();
        respuesta.setIdSesion(idSesion);
        respuesta.setResultado("success");
        respuesta.setMensaje("Pago verificado");
        respuesta.setIdPago("pi_verified_" + System.currentTimeMillis());
        
        return respuesta;
    }
}