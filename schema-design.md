# Smart Clinic Management System — Database Schema Design

## Overview

The database is designed for a Smart Clinic Management System with three user roles: **Admin**, **Doctor**, and **Patient**. The schema follows a normalized relational structure using PostgreSQL.

---

## Entity Relationship Summary

```
users (1) ──── (1) doctors ──── (many) appointments ──── (many) prescriptions
users (1) ──── (1) patients ──── (many) appointments
```

---

## Tables

### 1. `users`

Central identity table for all system users (admins, doctors, patients).

```sql
CREATE TABLE users (
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   TEXT NOT NULL,
    role       VARCHAR(50) NOT NULL DEFAULT 'patient'
                 CHECK (role IN ('admin', 'doctor', 'patient')),
    created_at TIMESTAMP DEFAULT NOW()
);
```

| Column      | Type         | Constraints              |
|-------------|--------------|--------------------------|
| id          | SERIAL       | PRIMARY KEY              |
| name        | VARCHAR(255) | NOT NULL                 |
| email       | VARCHAR(255) | NOT NULL, UNIQUE         |
| password    | TEXT         | NOT NULL                 |
| role        | VARCHAR(50)  | NOT NULL, CHECK (enum)   |
| created_at  | TIMESTAMP    | DEFAULT NOW()            |

---

### 2. `doctors`

Extends `users` with medical professional details.

```sql
CREATE TABLE doctors (
    id             SERIAL PRIMARY KEY,
    user_id        INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    speciality     VARCHAR(255) NOT NULL,
    phone          VARCHAR(50),
    available_from VARCHAR(50),
    available_to   VARCHAR(50),
    bio            TEXT,
    created_at     TIMESTAMP DEFAULT NOW()
);
```

| Column         | Type         | Constraints                          |
|----------------|--------------|--------------------------------------|
| id             | SERIAL       | PRIMARY KEY                          |
| user_id        | INTEGER      | NOT NULL, FK → users(id) CASCADE     |
| speciality     | VARCHAR(255) | NOT NULL                             |
| phone          | VARCHAR(50)  |                                      |
| available_from | VARCHAR(50)  | e.g. "09:00"                        |
| available_to   | VARCHAR(50)  | e.g. "17:00"                        |
| bio            | TEXT         |                                      |
| created_at     | TIMESTAMP    | DEFAULT NOW()                        |

---

### 3. `patients`

Extends `users` with patient health profile information.

```sql
CREATE TABLE patients (
    id            SERIAL PRIMARY KEY,
    user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    phone         VARCHAR(50),
    date_of_birth VARCHAR(50),
    gender        VARCHAR(20),
    address       VARCHAR(500),
    created_at    TIMESTAMP DEFAULT NOW()
);
```

| Column        | Type         | Constraints                      |
|---------------|--------------|----------------------------------|
| id            | SERIAL       | PRIMARY KEY                      |
| user_id       | INTEGER      | NOT NULL, FK → users(id) CASCADE |
| phone         | VARCHAR(50)  |                                  |
| date_of_birth | VARCHAR(50)  | ISO format: YYYY-MM-DD           |
| gender        | VARCHAR(20)  |                                  |
| address       | VARCHAR(500) |                                  |
| created_at    | TIMESTAMP    | DEFAULT NOW()                    |

---

### 4. `appointments`

Records all scheduled, completed, and cancelled clinic visits.

```sql
CREATE TABLE appointments (
    id               SERIAL PRIMARY KEY,
    patient_id       INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id        INTEGER NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_date VARCHAR(50) NOT NULL,
    appointment_time VARCHAR(50) NOT NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'scheduled'
                       CHECK (status IN ('scheduled', 'completed', 'cancelled')),
    notes            TEXT,
    created_at       TIMESTAMP DEFAULT NOW()
);
```

| Column           | Type        | Constraints                           |
|------------------|-------------|---------------------------------------|
| id               | SERIAL      | PRIMARY KEY                           |
| patient_id       | INTEGER     | NOT NULL, FK → patients(id) CASCADE   |
| doctor_id        | INTEGER     | NOT NULL, FK → doctors(id) CASCADE    |
| appointment_date | VARCHAR(50) | NOT NULL, format: YYYY-MM-DD          |
| appointment_time | VARCHAR(50) | NOT NULL, format: HH:MM               |
| status           | VARCHAR(50) | NOT NULL, CHECK (enum)                |
| notes            | TEXT        |                                       |
| created_at       | TIMESTAMP   | DEFAULT NOW()                         |

---

### 5. `prescriptions`

Medical prescriptions issued by doctors after completed appointments.

```sql
CREATE TABLE prescriptions (
    id             SERIAL PRIMARY KEY,
    appointment_id INTEGER NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    patient_id     INTEGER NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id      INTEGER NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    medication     VARCHAR(500) NOT NULL,
    dosage         VARCHAR(255) NOT NULL,
    instructions   TEXT,
    issued_at      TIMESTAMP DEFAULT NOW()
);
```

| Column         | Type         | Constraints                               |
|----------------|--------------|-------------------------------------------|
| id             | SERIAL       | PRIMARY KEY                               |
| appointment_id | INTEGER      | NOT NULL, FK → appointments(id) CASCADE   |
| patient_id     | INTEGER      | NOT NULL, FK → patients(id) CASCADE       |
| doctor_id      | INTEGER      | NOT NULL, FK → doctors(id) CASCADE        |
| medication     | VARCHAR(500) | NOT NULL                                  |
| dosage         | VARCHAR(255) | NOT NULL                                  |
| instructions   | TEXT         |                                           |
| issued_at      | TIMESTAMP    | DEFAULT NOW()                             |

---

## Stored Procedures

### `GetDailyAppointmentReportByDoctor`

Returns the number of appointments per doctor for a given date.

```sql
DELIMITER $$
CREATE PROCEDURE GetDailyAppointmentReportByDoctor(IN report_date DATE)
BEGIN
    SELECT
        d.id AS doctor_id,
        u.name AS doctor_name,
        d.speciality,
        COUNT(a.id) AS appointment_count,
        report_date AS date
    FROM doctors d
    JOIN users u ON d.user_id = u.id
    LEFT JOIN appointments a ON a.doctor_id = d.id
        AND a.appointment_date = DATE_FORMAT(report_date, '%Y-%m-%d')
    GROUP BY d.id, u.name, d.speciality
    ORDER BY appointment_count DESC;
END $$
DELIMITER ;
```

### `GetDoctorWithMostPatientsByMonth`

Returns the doctor with the highest number of appointments in a given month/year.

```sql
DELIMITER $$
CREATE PROCEDURE GetDoctorWithMostPatientsByMonth(IN p_month INT, IN p_year INT)
BEGIN
    SELECT
        d.id AS doctor_id,
        u.name AS doctor_name,
        d.speciality,
        COUNT(a.id) AS patient_count,
        CONCAT(p_year, '-', LPAD(p_month, 2, '0')) AS period
    FROM appointments a
    JOIN doctors d ON a.doctor_id = d.id
    JOIN users u ON d.user_id = u.id
    WHERE MONTH(STR_TO_DATE(a.appointment_date, '%Y-%m-%d')) = p_month
      AND YEAR(STR_TO_DATE(a.appointment_date, '%Y-%m-%d')) = p_year
    GROUP BY d.id, u.name, d.speciality
    ORDER BY patient_count DESC
    LIMIT 1;
END $$
DELIMITER ;
```

### `GetDoctorWithMostPatientsByYear`

Returns the doctor with the highest number of appointments in a given year.

```sql
DELIMITER $$
CREATE PROCEDURE GetDoctorWithMostPatientsByYear(IN p_year INT)
BEGIN
    SELECT
        d.id AS doctor_id,
        u.name AS doctor_name,
        d.speciality,
        COUNT(a.id) AS patient_count,
        CAST(p_year AS CHAR) AS period
    FROM appointments a
    JOIN doctors d ON a.doctor_id = d.id
    JOIN users u ON d.user_id = u.id
    WHERE YEAR(STR_TO_DATE(a.appointment_date, '%Y-%m-%d')) = p_year
    GROUP BY d.id, u.name, d.speciality
    ORDER BY patient_count DESC
    LIMIT 1;
END $$
DELIMITER ;
```

---

## Indexes

```sql
-- Performance indexes
CREATE INDEX idx_appointments_doctor_id ON appointments(doctor_id);
CREATE INDEX idx_appointments_patient_id ON appointments(patient_id);
CREATE INDEX idx_appointments_date ON appointments(appointment_date);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_prescriptions_patient_id ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor_id ON prescriptions(doctor_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_doctors_speciality ON doctors(speciality);
```

---

## Design Decisions

1. **Separate users table**: A single `users` table stores identity/auth data. `doctors` and `patients` extend via foreign keys. This avoids duplication and simplifies authentication.

2. **Soft enum via CHECK constraints**: Status fields use `VARCHAR` with CHECK constraints for portability. In Spring Boot, mapped via `@Enumerated(EnumType.STRING)`.

3. **Date stored as VARCHAR**: `appointment_date` is stored as `VARCHAR('YYYY-MM-DD')` for MySQL/PostgreSQL compatibility and easy string comparisons.

4. **Cascading deletes**: All child records cascade on parent deletion to maintain referential integrity.

5. **Stored procedures for reporting**: Complex analytics (daily/monthly/yearly reports) use stored procedures to offload computation to the database layer, improving performance at scale.
