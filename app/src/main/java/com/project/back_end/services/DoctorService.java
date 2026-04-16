package com.project.back_end.services;

package com.clinic.service;

import com.clinic.model.Appointment;
import com.clinic.model.Doctor;
import com.clinic.model.User;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    public List<Doctor> getAllDoctors(String speciality, String name, String availableTime) {
        List<Doctor> doctors = doctorRepository.findAll();

        if (speciality != null && !speciality.isEmpty()) {
            doctors = doctors.stream()
                    .filter(d -> d.getSpeciality().toLowerCase().contains(speciality.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (name != null && !name.isEmpty()) {
            doctors = doctors.stream()
                    .filter(d -> d.getUser().getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return doctors;
    }

    public Optional<Doctor> getDoctorById(Long id) {
        return doctorRepository.findById(id);
    }

    @Transactional
    public Doctor createDoctor(Doctor doctor) {
        User user = doctor.getUser();
        user.setRole(User.UserRole.DOCTOR);
        userRepository.save(user);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Optional<Doctor> updateDoctor(Long id, Doctor updated) {
        return doctorRepository.findById(id).map(existing -> {
            if (updated.getSpeciality() != null) existing.setSpeciality(updated.getSpeciality());
            if (updated.getPhone() != null) existing.setPhone(updated.getPhone());
            if (updated.getAvailableFrom() != null) existing.setAvailableFrom(updated.getAvailableFrom());
            if (updated.getAvailableTo() != null) existing.setAvailableTo(updated.getAvailableTo());
            if (updated.getBio() != null) existing.setBio(updated.getBio());

            if (updated.getUser() != null) {
                User user = existing.getUser();
                if (updated.getUser().getName() != null) user.setName(updated.getUser().getName());
                if (updated.getUser().getEmail() != null) user.setEmail(updated.getUser().getEmail());
                userRepository.save(user);
            }

            return doctorRepository.save(existing);
        });
    }

    @Transactional
    public void deleteDoctor(Long id) {
        doctorRepository.findById(id).ifPresent(doctor -> {
            Long userId = doctor.getUser().getId();
            doctorRepository.deleteById(id);
            userRepository.deleteById(userId);
        });
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }
}
