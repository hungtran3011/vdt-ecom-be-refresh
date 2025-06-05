package com.hungng3011.vdtecomberefresh.payment.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@Slf4j
public class ViettelSignatureHandler {

    /**
     * Sign a message using ECDSA with SHA256
     * @param message The message to sign (exact request body JSON or query string)
     * @param privateKeyBase64 The private key in Base64 format
     * @return Base64 encoded signature
     */
    public String signMessage(String message, String privateKeyBase64) {
        try {
            // Decode the private key from Base64
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            
            // Create private key from bytes
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            
            // Create signature
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            
            // Sign and encode to Base64
            byte[] signatureBytes = signature.sign();
            String signatureBase64 = Base64.getEncoder().encodeToString(signatureBytes);
            
            log.debug("Successfully signed message. Message length: {}, Signature: {}", 
                     message.length(), signatureBase64);
            
            return signatureBase64;
            
        } catch (Exception e) {
            log.error("Error signing message: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    /**
     * Verify a signature using ECDSA with SHA256
     * @param message The original message (exact response body JSON or query string)
     * @param signatureBase64 The signature to verify in Base64 format
     * @param publicKeyBase64 The public key in Base64 format
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String message, String signatureBase64, String publicKeyBase64) {
        try {
            // Decode the public key from Base64
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
            
            // Create public key from bytes
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            
            // Decode signature from Base64
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            
            // Verify signature
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            
            boolean isValid = signature.verify(signatureBytes);
            
            log.debug("Signature verification result: {}. Message length: {}", 
                     isValid, message.length());
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error verifying signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract query string for signature verification from a full URL
     * This method handles the case where signature verification should start from '?' or '&'
     * before the signature parameter
     * 
     * @param fullQueryString The complete query string
     * @param signatureParamName The name of the signature parameter (usually "signature")
     * @return The query string portion that should be used for verification
     */
    public String extractQueryStringForVerification(String fullQueryString, String signatureParamName) {
        if (fullQueryString == null || !fullQueryString.contains(signatureParamName + "=")) {
            return fullQueryString;
        }
        
        // Find the signature parameter
        int signatureIndex = fullQueryString.indexOf(signatureParamName + "=");
        if (signatureIndex == -1) {
            return fullQueryString;
        }
        
        // Find the character before the signature parameter
        if (signatureIndex == 0) {
            // Signature is the first parameter, return everything before it
            return "";
        }
        
        // Return everything before the signature parameter, including the separator
        char separator = fullQueryString.charAt(signatureIndex - 1);
        if (separator == '?' || separator == '&') {
            return fullQueryString.substring(0, signatureIndex - 1);
        }
        
        return fullQueryString.substring(0, signatureIndex);
    }
}
