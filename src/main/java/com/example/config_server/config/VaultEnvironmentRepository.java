package com.example.config_server.config;

import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
public class VaultEnvironmentRepository implements EnvironmentRepository {

    private final VaultTemplate vaultTemplate;

    public VaultEnvironmentRepository(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        Environment environment = new Environment(application, profile, label, null, null);

        try {
            Map<String, Object> properties = new HashMap<>();

            // Fetch secrets from different Vault paths
            // Path 1: Application-specific secrets
            fetchSecretsFromPath("secret/data/" + application, properties);

            // Path 2: Profile-specific secrets
            if (profile != null && !profile.isEmpty()) {
                fetchSecretsFromPath("secret/data/" + application + "-" + profile, properties);
            }

            // Path 3: Common/shared secrets
            fetchSecretsFromPath("secret/data/common", properties);

            // Add as a property source
            if (!properties.isEmpty()) {
                environment.add(new PropertySource("hashicorp-vault", properties));
            }

        } catch (Exception e) {
            System.err.println("Error connecting to HashiCorp Vault: " + e.getMessage());
            e.printStackTrace();
        }

        return environment;
    }

    private void fetchSecretsFromPath(String path, Map<String, Object> properties) {
        try {
            VaultResponse response = vaultTemplate.read(path);
            if (response != null && response.getData() != null) {
                Map<String, Object> data = response.getData();

                // Vault KV v2 stores data under a "data" key
                if (data.containsKey("data")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> secretData = (Map<String, Object>) data.get("data");
                    properties.putAll(secretData);
                } else {
                    // KV v1 format
                    properties.putAll(data);
                }

                System.out.println("Fetched " + properties.size() + " secrets from Vault path: " + path);
            }
        } catch (Exception e) {
            System.err.println("Could not fetch secrets from path: " + path + " - " + e.getMessage());
        }
    }
}