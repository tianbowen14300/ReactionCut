package com.tbw.cut.bilibili;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Base64Utils;

import javax.crypto.Cipher;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
public class BilibiliLoginUtils {
    
    /**
     * Encrypt password with RSA public key and salt
     * @param password Plain text password
     * @param hash Password salt
     * @param publicKey RSA public key (PEM format)
     * @return Base64 encoded encrypted password
     */
    public static String encryptPassword(String password, String hash, String publicKey) {
        try {
            // Concatenate salt and password
            String saltedPassword = hash + password;
            
            // Remove PEM header and footer, and whitespace
            String publicKeyContent = publicKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            
            // Decode public key
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pubKey = keyFactory.generatePublic(spec);
            
            // Encrypt with RSA
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] encryptedBytes = cipher.doFinal(saltedPassword.getBytes("UTF-8"));
            
            // Encode to Base64
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Password encryption failed: {}", e.getMessage(), e);
            throw new RuntimeException("Password encryption failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get encrypted password for Bilibili login
     * @param password Plain text password
     * @param loginKeyData Login key data from /x/passport-login/web/key API
     * @return Base64 encoded encrypted password
     */
    public static String getEncryptedPassword(String password, com.alibaba.fastjson.JSONObject loginKeyData) {
        try {
            String hash = loginKeyData.getString("hash");
            String key = loginKeyData.getString("key");
            return encryptPassword(password, hash, key);
        } catch (Exception e) {
            log.error("Failed to get encrypted password: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get encrypted password: " + e.getMessage(), e);
        }
    }
}