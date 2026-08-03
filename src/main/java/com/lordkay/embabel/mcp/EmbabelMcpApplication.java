package com.lordkay.embabel.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.lordkay.embabel.mcp.config.BioInsightHitlProperties;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.KgRagProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.config.OntoHarnessProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    BioInsightProperties.class,
    BioInsightHitlProperties.class,
    KgRagProperties.class,
    McpContextProperties.class,
    OntoHarnessProperties.class
})
public class EmbabelMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbabelMcpApplication.class, args);
    }
}
