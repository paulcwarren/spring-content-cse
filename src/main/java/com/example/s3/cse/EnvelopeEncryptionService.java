package com.example.s3.cse;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.data.util.Pair;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;

public class EnvelopeEncryptionService {

    private static KeyGenerator KEY_GENERATOR;

    static {
        // Create an encryption key.
        try {
            KEY_GENERATOR = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        KEY_GENERATOR.init(256, new SecureRandom());
    }

    //
    private static final String AES = "AES";
    private final VaultOperations vaultOperations;

    public EnvelopeEncryptionService(VaultOperations vaultOperations) {
        this.vaultOperations = vaultOperations;
    }

    private CipherOutputStream encryptMessage(OutputStream os, final SecretKey dataKey) throws NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException, InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(dataKey.getEncoded(), AES);
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.ENCRYPT_MODE, key);

//        OutputStream encodingStream = Base64.getEncoder().wrap(os);

        CipherOutputStream cos = new CipherOutputStream(/*encodingStream*/os, cipher);
        return cos;
    }

    public Pair<CipherOutputStream, byte[]> encrypt(OutputStream os) {
        try {
            SecretKey key = generateDataKey();

            // use transit to get ciphertext
            VaultTransitOperations transit = vaultOperations.opsForTransit();
            String base64Encoded = Base64.getEncoder().encodeToString(key.getEncoded());
            String ciphertext = transit.encrypt("test", base64Encoded);

            return Pair.of(encryptMessage(os, key), ciphertext.getBytes("UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException("unable to encrypt", e);
        }
    }

    private SecretKey generateDataKey() {
        return KEY_GENERATOR.generateKey();
    }

    private CipherInputStream decrypt3(final SecretKeySpec secretKeySpec, InputStream is) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException {
        Cipher cipher = Cipher.getInstance(AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

//        InputStream decodingStream = Base64.getDecoder().wrap(is);
//        InputStream decodingStream = new Base64InputStream(is, false, -1, new byte[] {}, CodecPolicy.LENIENT);

        CipherInputStream cos = new CipherInputStream(/*decodingStream*/is, cipher);
        return cos;
    }

    private SecretKeySpec decryptKey(byte[] encryptedKey) {
        VaultTransitOperations transit = vaultOperations.opsForTransit();
        String decryptedBase64Key = transit.decrypt("test", new String(encryptedKey));
        byte[] keyBytes = Base64.getDecoder().decode(decryptedBase64Key);

        SecretKeySpec key = new SecretKeySpec(keyBytes, AES);
        return key;
    }

    public CipherInputStream decrypt2(byte[] ecryptedKey, InputStream is) {
        try {
            SecretKeySpec key = decryptKey(ecryptedKey);
            return decrypt3(key, is);
        } catch (Exception e) {
            throw new RuntimeException("unable to decrypt", e);
        }
    }
}
