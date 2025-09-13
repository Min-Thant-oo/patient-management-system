package com.minthantoo.patient_service.kafka;

import billing.events.BillingAccountEvent;
import com.minthantoo.patient_service.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

@Service
public class kafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(kafkaProducer.class);
    // sending key(string) - value(byte array) pair
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public kafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // sending patient's info
    // call this method in PatientService.java
    public void sendPatientCreatedEvent(Patient patient) {
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .build();

        try {
            kafkaTemplate.send("patient.created", event.toByteArray()); // converted to byte array to keep the size of the event down
        } catch (Exception e) {
            log.error("Error sending PatientCreated event: {}", event);
        }
    }

    public void sendBillingAccountEvent(String patientId, String name, String email) {
        BillingAccountEvent event = BillingAccountEvent.newBuilder()
                .setPatientId(patientId)
                .setName(name)
                .setEmail(email)
                .setEventType("BILLING_ACCOUNT_CREATE_REQUESTED")
                .build();

        try {
            kafkaTemplate.send("billing-account", event.toByteArray());
        }  catch (Exception e) {
            log.error("Error sending BillingAccountCreated event: {}", e.getMessage());
        }
    }

}
