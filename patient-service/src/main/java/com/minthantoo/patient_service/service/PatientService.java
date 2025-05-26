package com.minthantoo.patient_service.service;

import com.minthantoo.patient_service.dto.PatientRequestDTO;
import com.minthantoo.patient_service.dto.PatientResponseDTO;
import com.minthantoo.patient_service.exception.EmailAlreadyExistsException;
import com.minthantoo.patient_service.mapper.PatientMapper;
import com.minthantoo.patient_service.model.Patient;
import com.minthantoo.patient_service.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PatientService {
    private final PatientRepository patientRepository;

    private PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<PatientResponseDTO> getPatients() {
        List<Patient> patients = patientRepository.findAll();

//        List<PatientResponseDTO> patientResponseDTOs = patients.stream()
//                .map(patient -> PatientMapper.toDTO(patient)).toList();
//        return patientResponseDTOs;

//        List<PatientResponseDTO> patientResponseDTOs = patients.stream()
//                .map(PatientMapper::toDTO).toList();
//        return patientResponseDTOs;

        return patients.stream()
                .map(PatientMapper::toDTO).toList();
    }

    public PatientResponseDTO createPatient(PatientRequestDTO patientRequestDTO) {
        if(patientRepository.existsByEmail(patientRequestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("A patient with this email " + "already exists" + patientRequestDTO.getEmail());
        }

        Patient newPatient = patientRepository.save(PatientMapper.toModel(patientRequestDTO));

        return PatientMapper.toDTO(newPatient);
    }
}
