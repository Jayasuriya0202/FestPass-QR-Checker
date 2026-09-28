package com.festpass.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI festPassOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FestPass — College Fest Ticketing with QR Check-in API")
                        .description("Backend REST API for creating college fest events, issuing digital tickets with unique QR codes, " +
                                "validating one-time entry check-in, and monitoring attendance against venue capacity.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("FestPass Engineering Team")
                                .email("festpass@college.edu")));
    }
}
