package uk.santander;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Clock;

import static java.time.ZoneId.of;

@Configuration
public class AppConfig {

    /*@Bean
    public WebClient webClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("myConnectionPool")
//                .maxConnections(<your_desired_max_connections>)
        .pendingAcquireMaxCount(2000)
        .build();
        ReactorClientHttpConnector clientHttpConnector = new ReactorClientHttpConnector(HttpClient.create(connectionProvider));
        return WebClient.builder()
                .clientConnector(clientHttpConnector)
                .build();
    }*/

    @Bean
    public WebClient webClient1(){
        return WebClient.create();
    }

    @Bean
    public WebClient webClient2(){
        return WebClient.create();
    }
}
