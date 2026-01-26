package org.adcb.poc.jweecdhkms.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HandshakeResponse {
    private String sessionId;
    private String backendPublicKey; // Base64
}
