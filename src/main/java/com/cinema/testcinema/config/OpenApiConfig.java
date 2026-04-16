package com.cinema.testcinema.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cinemaOpenAPI() {
        SecurityScheme oauth2Scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows()
                        .password(new OAuthFlow()
                                .tokenUrl("/auth/swagger-login")));
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("oauth2-bearer", oauth2Scheme))
                .addSecurityItem(new SecurityRequirement().addList("oauth2-bearer"))
                .info(new Info().title("Test Cinema API").version("v1"));
    }
}
