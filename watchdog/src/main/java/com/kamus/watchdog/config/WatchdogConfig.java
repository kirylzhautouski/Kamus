package com.kamus.watchdog.config;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.Properties;

@Configuration
@PropertySource("classpath:secrets.properties")
public class WatchdogConfig {

    private static final String GITHUB_OAUTH_PROPERTY = "oauth";

    @Value("${github.api.bearer}")
    private String githubToken;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public GitHub github() throws IOException {
        Properties properties = new Properties();
        properties.put(GITHUB_OAUTH_PROPERTY, githubToken);

        return GitHubBuilder.fromProperties(properties).build();
    }

}