package com.example.demo.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * As origens aceitas vem do ambiente.
 *
 * Antes havia `allowedOrigins("http://localhost:3000")` em codigo: em producao
 * o site nao conseguiria falar com a API, e mudar isso exigiria deploy.
 *
 * Lista vazia recusa TODAS as origens, e isso nao e o mesmo que `*`, que
 * aceita todas. Num servico com token no navegador, `*` e o contrario do que
 * "sem configuracao" deveria significar -- e por isso o valor nunca vira
 * curinga, em nenhum caminho deste arquivo.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> origens;

    public CorsConfig(@Value("${attus.cors.origens:}") String configuradas) {
        this.origens = configuradas.isBlank()
            ? List.of()
            : List.of(configuradas.split(",")).stream().map(String::trim).toList();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (origens.isEmpty()) {
            return;
        }

        registry.addMapping("/**")
                .allowedOrigins(origens.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
