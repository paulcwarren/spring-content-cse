package com.example.s3.cse;

import org.testcontainers.vault.VaultContainer;

public class VaultContainerSupport {

    private static VaultContainer vaultContainer = null;

    public static VaultContainer getVaultContainer() {

        if (vaultContainer == null) {
            vaultContainer = new VaultContainer<>()
                    .withVaultToken("my-root-token")
                    .withVaultPort(8200)
                    .withSecretInVault("secret/testing", "top_secret=password1","db_password=dbpassword1")
                    .withInitCommand("secrets enable transit", "write -f transit/keys/test");

            vaultContainer.start();
        }

        return vaultContainer;
    }
}
