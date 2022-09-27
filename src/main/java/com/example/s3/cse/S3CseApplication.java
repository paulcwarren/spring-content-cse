package com.example.s3.cse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.core.VaultOperations;

@SpringBootApplication
public class S3CseApplication {

	public static void main(String[] args) {
		SpringApplication.run(S3CseApplication.class, args);
	}

	@Configuration
	public static class Config extends AbstractVaultConfiguration {

	    @Override
	    public VaultEndpoint vaultEndpoint() {

	        String host = VaultContainerSupport.getVaultContainer().getHost();
	        int port = VaultContainerSupport.getVaultContainer().getMappedPort(8200);

	        VaultEndpoint vault = VaultEndpoint.create(host, port);
	        vault.setScheme("http");
	        return vault;
	    }

	    @Override
	    public ClientAuthentication clientAuthentication() {
	        return new TokenAuthentication("my-root-token");
	    }

        @Bean
        EnvelopeEncryptionService encrypter(VaultOperations vaultOperations) {
            return new EnvelopeEncryptionService(vaultOperations);
        }
        @Bean
        ClientSideEncryptionEventHandler eventHandler() {
            return new ClientSideEncryptionEventHandler();
        }
	}

}
