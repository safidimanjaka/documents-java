package com.example.confdoc.service;

import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.util.function.BiConsumer;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private final KeyStoreService keyStoreService;

    @Value("${app.supabase.bucket}")
    private String bucket;

    @Value("${app.supabase.secure.path}")
    private String securePath;

    private final SupabaseClientService client;

    public CryptoService(KeyStoreService keyStoreService, SupabaseClientService client) {
        this.keyStoreService = keyStoreService;
        this.client = client;
    }

    public static final int GCM_TAG_LENGTH = 128;
    public static final int GCM_IV_LENGTH = 12;

    public byte[] generateAesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256);
        SecretKey sk = kg.generateKey();
        return sk.getEncoded();
    }

    public byte[] wrapAesKey(byte[] aesKey) throws Exception {
        PublicKey publicKey = keyStoreService.getPublicKey();
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.WRAP_MODE, publicKey, oaepParams);
        SecretKey aes = new SecretKeySpec(aesKey, "AES");
        return cipher.wrap(aes);
    }

    public byte[] unwrapAesKey(byte[] wrapped) throws Exception {
        PrivateKey privateKey = keyStoreService.getPrivateKey();
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1",
                MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
        cipher.init(Cipher.UNWRAP_MODE, privateKey, oaepParams);
        Key key = cipher.unwrap(wrapped, "AES", Cipher.SECRET_KEY);
        return key.getEncoded();
    }

    public void encryptAndStoreWithWrappedKey(byte[] data, String filename, BiConsumer<String, byte[]> storeCallback) throws Exception {
        // Generate AES key, encrypt, wrap AES key with RSA public key, persist both:
        byte[] aesKey = generateAesKey();
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(iv);

        SecretKey aes = new SecretKeySpec(aesKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aes, spec);
        byte[] cipherText = cipher.doFinal(data);

        byte[] out = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);

        String newFilename = "doc_" + sanitizeFilename(filename);

        try {
            client.upload(bucket, securePath + "/" + newFilename, out);
        } catch (Exception e) {
            throw new Exception("Erreur lors de l'upload", e);
        }
        byte[] wrapped = wrapAesKey(aesKey);
        storeCallback.accept(newFilename, wrapped);
    }

    public byte[] decryptFromFile(String filePath, byte[] wrappedKey) throws Exception {
        byte[] fileBytes;
        try {
            fileBytes = client.download(bucket, securePath + "/" + filePath);
        } catch (Exception e) {
            throw new Exception("Erreur lors du téléchargement", e);
        }

        if (fileBytes.length < GCM_IV_LENGTH) throw new IllegalStateException("Fichier nvalid");
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(fileBytes, 0, iv, 0, iv.length);
        byte[] cipherText = new byte[fileBytes.length - iv.length];
        System.arraycopy(fileBytes, iv.length, cipherText, 0, cipherText.length);

        byte[] aesKey = unwrapAesKey(wrappedKey);
        SecretKey aes = new SecretKeySpec(aesKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aes, spec);
        return cipher.doFinal(cipherText);
    }

    private String sanitizeFilename(String f) {
        return f.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}