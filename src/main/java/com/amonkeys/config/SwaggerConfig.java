package com.amonkeys.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.security.oauth2.client.provider.google.authorization-uri}")
    private String authorizationUrl;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String tokenUrl;

    @Value("${spring.security.oauth2.client.registration.google.scope}")
    private String scopes;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("oauth2"))
                .components(new Components()
                        .addSecuritySchemes("oauth2", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl(authorizationUrl)
                                                .tokenUrl(tokenUrl)
                                                .scopes(parseScopes())))))
                .info(new Info()
                        .title("User Management API")
                        .version("1.0")
                        .description("API for managing users"));
    }

    private Scopes parseScopes() {
        Scopes scopesObject = new Scopes();
        for (String scope : scopes.split(",")) {
            scopesObject.addString(scope.trim(), scope.trim());
        }
        return scopesObject;
    }
}
