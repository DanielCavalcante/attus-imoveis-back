package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * O cliente do bucket, um so para dev e para producao.
 *
 * O emulador (fake-gcs-server) fala o protocolo do Cloud Storage, entao o que
 * muda entre os dois ambientes e o endereco e a credencial -- nunca o caminho
 * de codigo. Caminho separado para o emulador deixaria o de producao sem
 * nenhum teste, que e exatamente o que a C15 da spec 32 existe para impedir.
 */
@Configuration
public class StorageConfig {

    @Value("${attus.imagens.emulador:}")
    private String emulador;

    @Value("${attus.imagens.projeto:attus-imoveis}")
    private String projeto;

    @Bean
    Storage storage() throws java.io.IOException {
        if (emulador == null || emulador.isBlank()) {
            // Producao: a identidade vem da conta de servico da carga.
            return StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
                .getService();
        }

        // O emulador nao autentica; mandar credencial faria a chamada falhar
        // por motivo que nada tem a ver com o bucket.
        return StorageOptions.newBuilder()
            .setHost(emulador)
            .setProjectId(projeto)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    }
}
