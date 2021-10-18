package uk.santander;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final String SERVICE1 = "http://localhost:8080";
    private static final String SERVICE2 = "http://localhost:8085";
    private static final int NUMBER_OF_THREADS = 2000;

    @Autowired
    private WebClient webClient1;

    @Autowired
    private WebClient webClient2;

    private ConcurrentMap<String, ResponseEntity<Account>> createdAccounts = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        ConfigurableApplicationContext context = application.run(args);
        context.close();
    }

    @Override
    public void run(String... args) throws Exception {
        long t0 = System.currentTimeMillis();
        initialLoad();
        log.info("Initial load duration: {}", System.currentTimeMillis()-t0);
        process(SERVICE1);
        process(SERVICE2);
        log.info("Tiempo total de ejecución: {}", System.currentTimeMillis()- t0);
    }

    private void initialLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2*NUMBER_OF_THREADS);

        Stream.concat(createCallers(SERVICE1, "Pepe"), createCallers(SERVICE2, "Juan"))
            .forEach(task -> {
                executor.submit(task);
            });
        executor.shutdown();
        final boolean allRequestsFinished = executor.awaitTermination(30, TimeUnit.SECONDS);
        log.info("AllRequestsFinished: {}", allRequestsFinished);
    }

    private Stream<Runnable> createCallers(String host, String ownerName) {
        return IntStream.range(0, NUMBER_OF_THREADS).parallel()
                .mapToObj(n -> () -> {
                    webClient1.post()
                            .uri(host)
                            .bodyValue(Account.builder().owner(ownerName + getRandom()).value((double) n).build())
                            .retrieve()
                            .toEntity(Account.class)
                            .map(peekedAccount -> {
                                createdAccounts.put(ownerName+n, peekedAccount);
                                return peekedAccount;
                            })
                            .block();
                });
    }

    private double getRandom() {
        Random rn = new Random();
        return rn.nextInt(5) + 1;
    }

    private void process(String host) {
        final List<Flux<Account>> accountMonoList = createdAccounts.keySet().stream()
                .parallel()
                .map(owner -> webClient1.get()
                        .uri(host + "?owner=" + owner)
                        .retrieve()
                        .bodyToFlux(Account.class)
                        .flatMap(account -> webClient1.post().uri(host) //todo loguear
                                .bodyValue(account.toBuilder().owner(account.getOwner() + "new"+host).build()).retrieve().bodyToMono(Account.class)))
                .collect(Collectors.toList());
        Flux.fromIterable(accountMonoList).flatMap(Function.identity()).blockLast();
    }
}
