package com.samjay.ai_service.configurations;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Configuration
public class McpClientConfiguration {

    @Bean
    public List<McpSyncClient> mcpSyncClients() {

        McpSyncClient walletClient = buildMcpClient("http://localhost:8084");

        McpSyncClient orderClient = buildMcpClient("http://localhost:8087");

        McpSyncClient driverClient = buildMcpClient("http://localhost:8086");

        walletClient.initialize();

        orderClient.initialize();

        driverClient.initialize();

        return List.of(walletClient, orderClient, driverClient);
    }


    @Bean
    public ToolCallbackProvider toolCallbackProvider(List<McpSyncClient> mcpSyncClients) {

        return SyncMcpToolCallbackProvider
                .builder()
                .mcpClients(mcpSyncClients)
                .build();
    }

    private McpSyncClient buildMcpClient(String baseUrl) {

        McpSyncHttpClientRequestCustomizer requestCustomizer =
                (requestBuilder, method, uri, body, context) -> {

                    var authentication =
                            SecurityContextHolder
                                    .getContext()
                                    .getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {

                        requestBuilder.header(
                                "Authorization",
                                "Bearer " +
                                        jwtAuth.getToken().getTokenValue()
                        );
                    }
                };

        HttpClientStreamableHttpTransport transport =
                HttpClientStreamableHttpTransport
                        .builder(baseUrl)
                        .endpoint("/mcp")
                        .clientBuilder(
                                HttpClient.newBuilder()
                                        .connectTimeout(Duration.ofSeconds(60))
                                        .version(HttpClient.Version.HTTP_1_1)
                                        .followRedirects(HttpClient.Redirect.NORMAL)
                        )
                        .httpRequestCustomizer(requestCustomizer)
                        .connectTimeout(Duration.ofSeconds(60))
                        .build();


        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(60))
                .build();
    }
}