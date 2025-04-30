package com.example.demo.pago.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.repository.PagoRepository;
import com.example.demo.pago.model.PagoDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoService {

    @Value("${pago.api.key}")
    private String apiKey;

    @Value("${pago.use.sandbox}")
    private boolean useSandbox;

    @Value("${pago.sandbox.url}")
    private String sandboxUrl;

    @Value("${app.url}")
    private String appUrl;

    @Value("${pago.moneda.defecto:EUR}")
    private String defaultCurrency;

    @Autowired
    private PagoRepository pagoRepository;
    
    public Session crearSesionPago(PagoVO pago) throws StripeException {
        com.stripe.Stripe.apiKey = apiKey;

        if (useSandbox) {
            String sessionId = UUID.randomUUID().toString();
            Session fake = new Session();
            fake.setId(sessionId);
            fake.setUrl(sandboxUrl + "/pago/simulador?reservaId="
                        + pago.getReserva().getId()
                        + "&idSesion=" + sessionId);
            return fake;
        }

        BigDecimal amount = pago.getMonto().multiply(new BigDecimal(100));

        SessionCreateParams params = SessionCreateParams.builder()
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(appUrl + "/pago/exito?idSesion={CHECKOUT_SESSION_ID}")
            .setCancelUrl(appUrl + "/pago/fallido?idSesion={CHECKOUT_SESSION_ID}")
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(defaultCurrency)
                            .setUnitAmount(amount.longValue())
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Reserva #" + pago.getReserva().getId())
                                    .build())
                            .build())
                    .build())
            .build();

        return Session.create(params);
    }
    
    public List<PagoDTO> obtenerPorReserva(Long reservaId) {
        return pagoRepository.findAllByReservaId(reservaId).stream()
                             .map(PagoDTO::from)   
                             .collect(Collectors.toList());
    }

}
