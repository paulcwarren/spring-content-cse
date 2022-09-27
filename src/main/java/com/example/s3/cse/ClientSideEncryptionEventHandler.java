package com.example.s3.cse;

import com.Ostermiller.util.CircularByteBuffer;
import internal.org.springframework.content.s3.io.S3StoreResource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.HandleAfterGetResource;
import org.springframework.content.commons.annotations.HandleBeforeSetContent;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.events.AfterGetResourceEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.vault.core.VaultOperations;

import javax.annotation.PostConstruct;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@StoreEventHandler
public class ClientSideEncryptionEventHandler {

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


//    @Autowired
//    private EnvelopeEncryptionService encrypter;

    @Autowired
    private VaultOperations vaultOperations;

    @HandleBeforeSetContent
    public void onBeforeSetContent(BeforeSetContentEvent event)
            throws IOException {

        CircularByteBuffer cbb = new CircularByteBuffer();
//        Pair<CipherOutputStream, byte[]> encryptionContext = encrypter.encrypt(cbb.getOutputStream());
        EnvelopeEncryptionService encrypt = new EnvelopeEncryptionService(vaultOperations);
        Pair<CipherOutputStream, byte[]> encryptionContext = encrypt.encrypt(cbb.getOutputStream());

//        File entity = (File)event.getSource();
        ((File)event.getSource()).setContentKey(encryptionContext.getSecond());

        CipherOutputStream cos = encryptionContext.getFirst();
        new Thread(
                new Runnable(){
                    @Override
                    public void run(){
                        try {
                            IOUtils.copyLarge(event.getIs(), cos);
//                            IOUtils.copyLarge(new ByteArrayInputStream("this is a test".getBytes()), cos);
                            cos.flush();
                            IOUtils.closeQuietly(cos);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
        event.setReplacementInputStream(cbb.getInputStream());
    }

    @HandleAfterGetResource
    public void onAfterGetResource(AfterGetResourceEvent event) throws IOException {

        S3StoreResource r = (S3StoreResource) event.getResult();

        if (r != null) {
            EnvelopeEncryptionService encrypt = new EnvelopeEncryptionService(vaultOperations);
            CipherInputStream unencryptedStream = encrypt.decrypt2(((File)event.getSource()).getContentKey(), r.getInputStream());
            Resource ir = new InputStreamResource(unencryptedStream);
            event.setResult(ir);
        }
    }

    @PostConstruct
    public void init() throws IOException {

//        // check vault operations
//        VaultKeyValueOperations ops = vaultOperations.opsForKeyValue("secret", KeyValueBackend.versioned());
//        VaultResponse res = ops.get("testing");
//
//        // generate (and store) an ek
//        SecretKey ek = KEY_GENERATOR.generateKey();
//        String keyVal = Base64.getEncoder().encodeToString(ek.getEncoded());
//        ops.put("ek", new Ek(keyVal));
//        VaultResponseSupport<Ek> k = ops.get("ek", Ek.class);
//        Assert.assertEquals(keyVal, k.getData().getValue());
//
//        // vault transit provides a way to encrypt the encyption key (and rotate keys)
//        // ciphertext can be stored
//        // plaintext used to encrypt the data
//        VaultTransitOperations transit = vaultOperations.opsForTransit();
//        String ciphertext = transit.encrypt("test", keyVal);
//        System.out.println("--->" + ciphertext + "<---");
//        String decrypted = transit.decrypt("test", ciphertext);
//        Assert.assertEquals(decrypted, keyVal);
//
//
//        CircularByteBuffer cbb = new CircularByteBuffer();
////        Pair<CipherOutputStream, byte[]> encryptionContext = encrypter.encrypt(cbb.getOutputStream());
//        EnvelopeEncryptionService encrypt = new EnvelopeEncryptionService(vaultOperations);
//        Pair<CipherOutputStream, byte[]> encryptionContext = encrypt.encrypt(cbb.getOutputStream());
//
////        File entity = (File)event.getSource();
//        File entity = new File();
//        entity.setContentKey(encryptionContext.getSecond());
//
//        CipherOutputStream cos = encryptionContext.getFirst();
//        new Thread(
//          new Runnable(){
//            @Override
//            public void run(){
//                try {
////                    IOUtils.copyLarge(event.getIs(), cos);
//                    IOUtils.copyLarge(new ByteArrayInputStream("this is a test".getBytes()), cos);
//                    cos.flush();
//                    IOUtils.closeQuietly(cos);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//          }
//        ).start();
////        event.setReplacementInputStream(cbb.getInputStream());
////        System.out.println(IOUtils.toString(cbb.getInputStream()));
//
//        CipherInputStream unencryptedStream = encrypt.decrypt2(entity.getContentKey(), cbb.getInputStream());
//        System.out.println(IOUtils.toString(unencryptedStream));
//
//        int i=0;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    private static class Ek {
        String value;
    }
}
