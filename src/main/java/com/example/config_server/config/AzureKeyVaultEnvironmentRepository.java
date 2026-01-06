package com.example.config_server.config;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AzureKeyVaultEnvironmentRepository implements EnvironmentRepository {

    private final SecretClient secretClient;

    public AzureKeyVaultEnvironmentRepository(SecretClient secretClient) {
        this.secretClient = secretClient;
    }

    @Override
    public Environment findOne(String application, String profile, String label) {
        Environment environment = new Environment(application, profile, label, null, null);

        try {
            Map<String, Object> properties = new HashMap<>();

            // Fetch all secrets from Key Vault
            secretClient.listPropertiesOfSecrets().forEach(secretProperties -> {
                try {
                    KeyVaultSecret secret = secretClient.getSecret(secretProperties.getName());
                    properties.put(secretProperties.getName(), secret.getValue());
                } catch (Exception e) {
                    System.err.println("Error fetching secret: " + secretProperties.getName());
                }
            });

            // Add as a property source
            environment.add(new PropertySource("azure-keyvault", properties));

        } catch (Exception e) {
            System.err.println("Error connecting to Azure Key Vault: " + e.getMessage());
        }

        return environment;
    }
}