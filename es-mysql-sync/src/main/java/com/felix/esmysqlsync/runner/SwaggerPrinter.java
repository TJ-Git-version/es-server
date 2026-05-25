package com.felix.esmysqlsync.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@Slf4j
public class SwaggerPrinter implements ApplicationRunner {

    @Value("${server.port:8080}")
    private int port;

    @GetMapping("/swagger-url")
    public String swaggerUrl() {
        return getSwaggerUrls();
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("\n{}\n", getSwaggerUrls());
    }

    private String getSwaggerUrls() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            host = "localhost";
        }

        return """
                ╔══════════════════════════════════════════════════════════════╗
                ║                    Swagger UI 访问地址                        ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  Knife4j:  http://%s:%d/doc.html                            ║
                ║  Swagger:  http://%s:%d/swagger-ui.html                     ║
                ║  API Docs: http://%s:%d/v3/api-docs                        ║
                ╚══════════════════════════════════════════════════════════════╝
                """.formatted(host, port, host, port, host, port);
    }
}