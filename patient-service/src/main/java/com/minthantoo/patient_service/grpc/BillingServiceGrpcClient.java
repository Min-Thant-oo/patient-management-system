package com.minthantoo.patient_service.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import com.minthantoo.patient_service.kafka.kafkaProducer;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BillingServiceGrpcClient {
    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;
    private final kafkaProducer kafkaProducer;

    // constructor
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort,
            kafkaProducer kafkaProducer) {
        log.info("Connecting to Billing Service Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(serverAddress, serverPort)
                .usePlaintext()
                .build();

        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
        this.kafkaProducer = kafkaProducer;
    }

    // use circuit breaker in case billing-service fails
    @CircuitBreaker(name = "billingService", fallbackMethod = "billingFallback")
    @Retry(name = "billingRetry") // @Retry will retry 3 times  before circuit breaker kicks in
    public BillingResponse createBillingAccount(String patientId, String name, String email) {
        BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId).setName(name).setEmail(email).build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        log.info("Received response from billing service via GRPC: {}", response);
        return response;
    }

    public BillingResponse billingFallback(String patientId, String name, String email, Throwable t) {
        log.warn("[CIRCUIT BREAKER]: Billing service is unavailable. Triggered " + "fallback: {}", t.getMessage());

        // saving patient information in kafka so that kafka can send it back to createBillingAccount method when billing-service works again
        kafkaProducer.sendBillingAccountEvent(patientId, name, email);

        return BillingResponse.newBuilder()
                .setAccountId("")
                .setStatus("PENDING")
                .build();
    }
}


