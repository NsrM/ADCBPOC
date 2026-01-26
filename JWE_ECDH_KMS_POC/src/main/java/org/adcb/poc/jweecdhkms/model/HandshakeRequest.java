package org.adcb.poc.jweecdhkms.model;

import lombok.Data;

@Data
public class HandshakeRequest {
    private String clientPublicKey; // Base64
}
