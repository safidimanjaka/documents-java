package com.example.confdoc.service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class KeyStoreService {

    @Value("${app.keystore.path}")
    private String keystorePath;

    @Value("${app.keystore.password}")
    private String keystorePassword;

    @Value("${app.keystore.alias}")
    private String keyAlias;

    @Value("${app.keystore.key-password}")
    private String keyPassword;

    private KeyStore keyStore;

    @PostConstruct
    public void init() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        ensureKeyStoreAndKey();
        loadKeyStore();
    }

    public void ensureKeyStoreAndKey() throws Exception {
        File ks = new File(keystorePath);
        if (!ks.exists()) {
            File parent = ks.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            generateKeystore(keystorePath, keystorePassword.toCharArray(), keyAlias, keyPassword.toCharArray());
        }
    }

    private void loadKeyStore() throws Exception {
        keyStore = KeyStore.getInstance("JKS");
        try (var in = Files.newInputStream(new File(keystorePath).toPath())) {
            keyStore.load(in, keystorePassword.toCharArray());
        }
    }

    public PrivateKey getPrivateKey() throws Exception {
        if (keyStore == null) loadKeyStore();
        Key k = keyStore.getKey(keyAlias, keyPassword.toCharArray());
        if (k instanceof PrivateKey) return (PrivateKey) k;
        throw new IllegalStateException("Aucune clé privée trouvée");
    }

    public PublicKey getPublicKey() throws Exception {
        if (keyStore == null) loadKeyStore();
        Certificate cert = keyStore.getCertificate(keyAlias);
        if (cert == null) throw new IllegalStateException("Aucun certificat trouvé");
        return cert.getPublicKey();
    }

    private void generateKeystore(String path, char[] storePassword, String alias, char[] keyPassword) throws Exception {
        // Ensure BouncyCastle provider
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        // Generate RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        // Create a self-signed certificate using BouncyCastle (bcpkix)
        X500Name dnName = new X500Name("CN=ConfidentialDoc, O=Example Org, C=FR");
        BigInteger certSerial = BigInteger.valueOf(System.currentTimeMillis());
        Date startDate = new Date(System.currentTimeMillis() - 24L * 60 * 60 * 1000);
        Date endDate = new Date(System.currentTimeMillis() + 3650L * 24 * 60 * 60 * 1000L);

        // Use JcaX509v3CertificateBuilder which accepts a PublicKey
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dnName, certSerial, startDate, endDate, dnName, pair.getPublic());

        // Use BouncyCastle provider explicitly for signer and converter
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(pair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(signer));
        cert.checkValidity(new Date());
        cert.verify(pair.getPublic());

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, storePassword);
        ks.setKeyEntry(alias, pair.getPrivate(), keyPassword, new Certificate[]{cert});

        try (FileOutputStream fos = new FileOutputStream(path)) {
            ks.store(fos, storePassword);
        }
    }
}