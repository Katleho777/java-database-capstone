package com.project.back_end.services;

import com.clinic.model.Appointment;
import com.clinic.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public Appointment createAppointment(Appointment appointment) {
        appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        return appointmentRepository.save(appointment);
    }

    public Optional<Appointment> updateAppointment(Long id, Appointment updated) {
        return appointmentRepository.findById(id).map(existing -> {
            if (updated.getStatus() != null) {
                existing.setStatus(updated.getStatus());
            }
            if (updated.getNotes() != null) {
                existing.setNotes(updated.getNotes());
            }
            if (updated.getAppointmentDate() != null) {
                existing.setAppointmentDate(updated.getAppointmentDate());
            }
            if (updated.getAppointmentTime() != null) {
                existing.setAppointmentTime(updated.getAppointmentTime());
            }
            return appointmentRepository.save(existing);
        });
    }

    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    public List<Appointment> getAppointmentsByPatientId(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getAppointmentsByDoctorId(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public List<Appointment> getAppointmentsByStatus(Appointment.AppointmentStatus status) {
        return appointmentRepository.findByStatus(status);
    }

    public long countByStatus(Appointment.AppointmentStatus status) {
        return appointmentRepository.countByStatus(status);
    }

    public long countAll() {
        return appointmentRepository.count();
    }
}
