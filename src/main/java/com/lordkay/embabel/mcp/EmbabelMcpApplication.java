package com.lordkay.embabel.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.KgRagProperties;

@SpringBootApplication
@EnableConfigurationProperties({BioInsightProperties.class, KgRagProperties.class})
public class EmbabelMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbabelMcpApplication.class, args);
    }
}
