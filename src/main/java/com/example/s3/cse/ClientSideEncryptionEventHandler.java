package com.example.s3.cse;

import com.Ostermiller.util.CircularByteBuffer;
import internal.org.springframework.content.s3.io.S3StoreResource;
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

    @Autowired
    private EnvelopeEncryptionService encrypter;

    @HandleBeforeSetContent
    public void onBeforeSetContent(BeforeSetContentEvent event)
            throws IOException {

        CircularByteBuffer cbb = new CircularByteBuffer();
        Pair<CipherOutputStream, byte[]> encryptionContext = encrypter.encrypt(cbb.getOutputStream());

        ((File)event.getSource()).setContentKey(encryptionContext.getSecond());

        CipherOutputStream cos = encryptionContext.getFirst();
        new Thread(
                new Runnable(){
                    @Override
                    public void run(){
                        try {
                            IOUtils.copyLarge(event.getIs(), cos);
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
            CipherInputStream unencryptedStream = encrypter.decrypt2(((File)event.getSource()).getContentKey(), r.getInputStream());
            Resource ir = new InputStreamResource(unencryptedStream);
            event.setResult(ir);
        }
    }
}
