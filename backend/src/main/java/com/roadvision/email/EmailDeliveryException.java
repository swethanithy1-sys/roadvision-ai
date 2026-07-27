package com.roadvision.email;

/** The email provider refused to send. Registration/reset can't continue — the user would never receive their code. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
