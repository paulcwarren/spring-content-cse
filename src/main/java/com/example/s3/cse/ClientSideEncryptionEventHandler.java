package com.example.s3.cse;

import internal.org.springframework.content.s3.io.S3StoreResource;
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
import javax.crypto.KeyGenerator;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
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

        Pair<CipherInputStream, byte[]> encryptionContext = encrypter.encrypt(event.getIs());
        ((File)event.getSource()).setContentKey(encryptionContext.getSecond());
        event.setInputStream(encryptionContext.getFirst());
    }

    @HandleAfterGetResource
    public void onAfterGetResource(AfterGetResourceEvent event) throws IOException {

        S3StoreResource r = (S3StoreResource) event.getResult();

        if (r != null) {
            CipherInputStream unencryptedStream = encrypter.decrypt(((File)event.getSource()).getContentKey(), r.getInputStream());
            Resource ir = new InputStreamResource(new SkipInputStream(unencryptedStream));
            event.setResult(ir);
        }
    }

    // CipherInputStream skip does not work.  This wraps a cipherinputstream purely to override the skip with a
    // working version
    public class SkipInputStream extends FilterInputStream
    {
        private static final int MAX_SKIP_BUFFER_SIZE = 2048;

        protected SkipInputStream (InputStream in)
        {
            super(in);
        }

        public long skip(long n)
                throws IOException
        {
            long remaining = n;
            int nr;

            if (n <= 0) {
                return 0;
            }

            int size = (int)Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
            byte[] skipBuffer = new byte[size];
            while (remaining > 0) {
                nr = in.read(skipBuffer, 0, (int)Math.min(size, remaining));
                if (nr < 0) {
                    break;
                }
                remaining -= nr;
            }

            return n - remaining;
        }

    }
}
