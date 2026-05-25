package com.felix.esmysqlsync.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ElasticsearchConfig {

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        return new ElasticsearchClient(
                new RestClientTransport(
                        RestClient.builder(
                                new HttpHost("100.64.0.8", 9200, "http")).build(), new JacksonJsonpMapper()
                )
        );
    }

    /**
     * 配置线程池
     * 类型：IO密集型
     */
    @Bean(name = "esThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor esThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setPrestartAllCoreThreads(true);
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setKeepAliveSeconds(60);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("ES-");
        executor.initialize();
        return executor;
    }


}
