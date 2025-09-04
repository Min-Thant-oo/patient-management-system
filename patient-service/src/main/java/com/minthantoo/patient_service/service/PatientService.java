package com.minthantoo.patient_service.service;

import com.minthantoo.patient_service.dto.PagedPatientResponseDTO;
import com.minthantoo.patient_service.dto.PatientRequestDTO;
import com.minthantoo.patient_service.dto.PatientResponseDTO;
import com.minthantoo.patient_service.exception.EmailAlreadyExistsException;
import com.minthantoo.patient_service.exception.PatientNotFoundException;
import com.minthantoo.patient_service.grpc.BillingServiceGrpcClient;
import com.minthantoo.patient_service.kafka.kafkaProducer;
import com.minthantoo.patient_service.mapper.PatientMapper;
import com.minthantoo.patient_service.model.Patient;
import com.minthantoo.patient_service.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {
    private static final Logger log = LoggerFactory.getLogger(PatientService.class);
    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;
    private final kafkaProducer kafkaProducer;

    public PatientService(PatientRepository patientRepository,
                           BillingServiceGrpcClient billingServiceGrpcClient,
                           kafkaProducer kafkaProducer) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
        this.kafkaProducer = kafkaProducer;
    }

    // The flow is we get the patients from database.
    // Got a patientPage object _ this has patients + pagination info.
    // we take just the patients out of that patientPage object and turn them into DTOs meaning without pagination info(not to expose entities)
    // Finally, we put those DTOs back together with the pagination info into our own response object (PagedPatientResponseDTO) and return it.
    @Cacheable(
            value = "patients",
            key = "#page + '-' + #size + '-' + #sort + '-' + #sortField",
            condition = "#searchValue == ''" // only cache response when searchValue is empty string
    )
    public PagedPatientResponseDTO getPatients(int page, int size, String sort, String sortField, String searchValue) {
        log.info("[REDIS]: Cache miss - fetching from DB");

        try {
            Thread.sleep(2000);
        }  catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        // zero-based, so page 0 = first page
        // request -> page = 1
        // pageable -> page = 0
        Pageable pageable = PageRequest.of(page -1, size,
                sort.equalsIgnoreCase("desc") // "asc" or "desc"
                        ? Sort.by(sortField).descending()
                        : Sort.by(sortField).ascending());

        Page<Patient> patientPage;

        // Got a patientPage object _ this has patients + pagination info.
        if(searchValue == null || searchValue.isBlank()) {
            patientPage = patientRepository.findAll(pageable);
        } else {
            patientPage = patientRepository.findByNameContainingIgnoreCase(searchValue, pageable);
        }

        // take just the patients content out of that patientPage object and turn them into DTOs
        // getContent() -> the list of the actual objects for the current page
        List<PatientResponseDTO> patientResponseDtos = patientPage.getContent().stream().map(PatientMapper::toDTO).toList();

        return new PagedPatientResponseDTO(
                patientResponseDtos,
                // these methods are from Spring Data’s Page interface.
                patientPage.getNumber() +1,  // the current page number (zero-based, so page 0 = first page, +1 so that the client won't get confused)
                patientPage.getSize(), // he size of the page (how many records per page)
                patientPage.getTotalPages(), // the total number of pages available
                (int)patientPage.getTotalElements() // the total number of records across all pages
        );
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if(patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with this email " + "already exists" + patientRequestDTO.getEmail());
        }

        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

        // calling billing-service grpc client to create account for them
        billingServiceGrpcClient.createBillingAccount(newPatient.getId().toString(), newPatient.getName(), newPatient.getEmail());

        // calling kafka
        kafkaProducer.sendEvent(newPatient);
        return PatientMapper.toDTO(newPatient);
    }

    public PatientResponseDTO updatePatient(UUID id, PatientRequestDTO patientRequestDTO) {
        Patient patient = patientRepository.findById(id).orElseThrow(
                () -> new PatientNotFoundException("Patient not found with ID: " + id));

        if(patientRepository.existsByEmailAndIdNot(patientRequestDTO.getEmail(), id)) {
            throw new EmailAlreadyExistsException("A patient with this email " + "already exists" + patientRequestDTO.getEmail());
        }

        patient.setName(patientRequestDTO.getName());
        patient.setAddress(patientRequestDTO.getAddress());
        patient.setEmail(patientRequestDTO.getEmail());
        patient.setDateOfBirth(LocalDate.parse(patientRequestDTO.getDateOfBirth()));

        Patient updatedPatient = patientRepository.save(patient);

        return PatientMapper.toDTO(updatedPatient);
    }

    public void deletePatient(UUID id) {
        patientRepository.deleteById(id);
    }
}
