package com.universidade.bot.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EncryptionService {
    private static EncryptionService instance;
    private final SecretKey secretKey;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final Map<String, String> sessionStore = new ConcurrentHashMap<>();
    private final Map<String, String> reverseSessionStore = new ConcurrentHashMap<>();

    private EncryptionService() {
        String envKey = System.getenv("ENCRYPTION_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            byte[] keyBytes = Base64.getDecoder().decode(envKey);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            KeyGenerator keyGen;
            try {
                keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256);
                this.secretKey = keyGen.generateKey();
                System.out.println("AVISO: Chave de criptografia gerada aleatoriamente. Defina ENCRYPTION_KEY para persistencia.");
            } catch (Exception e) {
                throw new RuntimeException("Erro ao gerar chave de criptografia", e);
            }
        }
    }

    public static synchronized EncryptionService getInstance() {
        if (instance == null) {
            instance = new EncryptionService();
        }
        return instance;
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criptografar", e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar", e);
        }
    }

    public String createSession(String internalId) {
        String token = UUID.randomUUID().toString();
        sessionStore.put(token, internalId);
        reverseSessionStore.put(internalId, token);
        return token;
    }

    public String getSession(String token) {
        return sessionStore.get(token);
    }

    public String getOrCreateSession(String internalId) {
        String existing = reverseSessionStore.get(internalId);
        if (existing != null && sessionStore.containsKey(existing)) {
            return existing;
        }
        return createSession(internalId);
    }

    public boolean invalidateSession(String token) {
        String internalId = sessionStore.remove(token);
        if (internalId != null) {
            reverseSessionStore.remove(internalId);
            return true;
        }
        return false;
    }

    public String hashForLog(String sensitive) {
        return "***" + sensitive.substring(Math.max(0, sensitive.length() - 3));
    }
}
