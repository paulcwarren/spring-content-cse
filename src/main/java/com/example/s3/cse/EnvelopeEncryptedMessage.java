package com.example.s3.cse;

import javax.crypto.CipherOutputStream;

public final class EnvelopeEncryptedMessage {

    private byte[] encryptedKey;
    private String ciphertext;
    private CipherOutputStream cos;

    public byte[] getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(byte[] encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public void setCipherOutputStream(CipherOutputStream cos) {
        this.cos = cos;
    }

    public CipherOutputStream getCipherOutputStream() {
        return cos;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public String getCiphertext() {
        return ciphertext;
    }
}
