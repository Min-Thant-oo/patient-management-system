package com.minthantoo.patient_service.aspects;

import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PatientServiceMetrics {
    private final MeterRegistry meterRegistry;

    public PatientServiceMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* com/minthantoo/patient_service/service/PatientService.getPatients(..)")
    // Run this method before running getPatients method in PatientService.java after checking cache
    // Check cache > aspect method(monitorGetPatients) > getPatients method
    public Object monitorGetPatients(ProceedingJoinPoint joinPoint) throws Throwable {
        // Log cache miss entry into actuator metrics
        meterRegistry.counter("custom.redis.cache.miss", "cache", "patients").increment();
        // proceeds back to the getPatients method
        Object result = joinPoint.proceed();
        return result;
    }
}
