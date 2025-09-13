package com.minthantoo.appointmentservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent; // comes from generated-sources from maven clean and compile

@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumer.class);

    // two event topics for creating patient and updating patient
    // used service name as groupId
    @KafkaListener(topics = {"patient.created", "patient.updated"},groupId = "appointment-service")
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);

            log.info("Received Patient Event {}", patientEvent.toString());

        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing Patient Event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error consuming Patient Event: {}", e.getMessage());
        }
    }

}
