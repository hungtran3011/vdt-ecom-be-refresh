package com.hungng3011.vdtecomberefresh.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "vdt-ecom.mail")
public class MailProperties {
    
    private String from;
    private boolean enabled = true;
    private boolean async = true;
    private Map<String, String> templates;
    private Map<String, String> subjects;
}
