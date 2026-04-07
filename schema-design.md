## MySQL Database Design

### TABLE users (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('patient','doctor','admin') NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
Patients
### TABLE patients (
  patient_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  date_of_birth DATE,
  gender ENUM('male','female','other'),
  phone VARCHAR(50),
  mobile VARCHAR(50),
  address TEXT,
  city VARCHAR(100),
  state VARCHAR(100),
  postal_code VARCHAR(20),
  insurance_provider VARCHAR(100),
  insurance_number VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_patient_user (user_id)
);
Doctors
### TABLE doctors (
  doctor_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  specialty VARCHAR(100),
  license_number VARCHAR(80),
  department VARCHAR(100),
  phone VARCHAR(50),
  email VARCHAR(255),
  office_location VARCHAR(255),
  bio TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
  INDEX idx_doctor_user (user_id),
  INDEX idx_doctor_license (license_number)
);
Appointments
### TABLE appointments (
  appointment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  status ENUM('scheduled','confirmed','completed','cancelled','no_show') NOT NULL,
  location VARCHAR(255),
  notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
  FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
  INDEX idx_appointment_doctor_time (doctor_id, start_time),
  INDEX idx_appointment_patient_time (patient_id, start_time)
);
Prescriptions
### TABLE prescriptions (
  prescription_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  appointment_id BIGINT NOT NULL,
  doctor_id BIGINT NOT NULL,
  patient_id BIGINT NOT NULL,
  medication_name VARCHAR(255) NOT NULL,
  dosage VARCHAR(100),
  route VARCHAR(50),
  frequency VARCHAR(100),
  duration VARCHAR(100),
  instructions TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE SET NULL,
  FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
  FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
  INDEX idx_prescription_appointment (appointment_id),
  INDEX idx_prescription_patient (patient_id)
);
Doctor Availability (recurring weekly hours)
### TABLE doctor_availability (
  availability_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  day_of_week TINYINT NOT NULL, -- 0=Mon ... 6=Sun
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  is_recurring BOOLEAN DEFAULT TRUE,
  timezone VARCHAR(64) DEFAULT 'UTC',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
  INDEX idx_doctor_day (doctor_id, day_of_week)
);
Doctor Availability Exceptions (per-date overrides)
### TABLE doctor_availability_exceptions (
  exception_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doctor_id BIGINT NOT NULL,
  date DATE NOT NULL,
  start_time TIME,
  end_time TIME,
  status ENUM('open','closed') NOT NULL,
  reason VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE CASCADE,
  INDEX idx_doctor_exception (doctor_id, date)
);
Notes
### TABLE notes (
  note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  doctor_id BIGINT,
  appointment_id BIGINT,
  content TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
  FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id) ON DELETE SET NULL,
  FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE SET NULL
);
Chat messages (optional)
### TABLE chat_messages (
  message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id BIGINT NOT NULL,
  recipient_id BIGINT NOT NULL,
  appointment_id BIGINT,
  content TEXT NOT NULL,
  sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (recipient_id) REFERENCES users(user_id) ON DELETE CASCADE,
  FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
);
Payments
### TABLE payments (
  payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  patient_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  currency VARCHAR(3) DEFAULT 'USD',
  status ENUM('pending','completed','failed') NOT NULL,
  method VARCHAR(50),
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  appointment_id BIGINT,
  FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
  FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id)
);
Documents (uploads)
### TABLE documents (
  document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  owner_type ENUM('patient','doctor','appointment','clinic') NOT NULL,
  owner_id BIGINT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  mime_type VARCHAR(100),
  size BIGINT,
  storage_path VARCHAR(512),  -- e.g., S3 key, GridFS id, or local path
  uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES 
    CASE owner_type
      WHEN 'patient' THEN patients(patient_id)
      WHEN 'doctor' THEN doctors(doctor_id)
      WHEN 'appointment' THEN appointments(appointment_id)
      WHEN 'clinic' THEN NULL
    END
);

## MongoDB Collection Deesign

### Collection: prescriptions
```json
{
  _id: ObjectId("..."),
  email: "alice@example.com",
  password_hash: "…",
  role: "patient" | "doctor" | "admin",
  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
### Collection: patients
```json
Purpose: patient profile; can embed quick references and optional medical history
{
  _id: ObjectId("..."),
  user_id: ObjectId("..."), // reference to users._id
  first_name: "Alice",
  last_name: "Smith",
  date_of_birth: ISODate("1985-04-12"),
  gender: "female",
  contact: {
    phone: "+1-555-1234",
    email: "alice@example.com",
    address: "123 Main St",
    city: "City",
    state: "State",
    postal_code: "12345"
  },
  insurance: {
    provider: "Acme Insurance",
    number: "INS-123456"
  },
  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
### Collection: doctors
```json
Purpose: doctor profile with embedded availability (recurring) and optional overrides
{
  _id: ObjectId("..."),
  user_id: ObjectId("..."),
  first_name: "Dr. John",
  last_name: "Doe",
  specialty: "Cardiology",
  license_number: "LIC-9876",
  department: "Cardiology",
  contact: {
    phone: "+1-555-5678",
    email: "doctor@example.com"
  },
  office_location: "Building A, Room 101",
  bio: "Board-certified cardiologist with 15 years of experience.",
  availability: [
    { day_of_week: 1, start_time: "09:00", end_time: "12:00", timezone: "UTC" },
    { day_of_week: 3, start_time: "13:00", end_time: "17:00", timezone: "UTC" }
  ],
  // Optional override-by-date (can be a separate collection if heavy)
  // availability_exceptions: [ ... ]
  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
### Collection: appointments
```json
Purpose: schedule and track visits
{
  _id: ObjectId("..."),
  patient_id: ObjectId("..."),
  doctor_id: ObjectId("..."),
  start_time: ISODate("2026-04-20T09:00:00Z"),
  end_time: ISODate("2026-04-20T09:30:00Z"),
  status: "scheduled" | "confirmed" | "completed" | "cancelled" | "no_show",
  location: "Office 202",
  notes: "Initial consultation",
  created_at: ISODate("..."),
  updated_at: ISODate("...")
}
### Collection: prescriptions
```json
Purpose: linked to appointment/patient/doctor
{
  _id: ObjectId("..."),
  appointment_id: ObjectId("..."),
  doctor_id: ObjectId("..."),
  patient_id: ObjectId("..."),
  medication_name: "Drug A",
  dosage: "10 mg",
  route: "oral",
  frequency: "once daily",
  duration: "7 days",
  instructions: "Take after meals",
  created_at: ISODate("...")
}
### Collection: notes
```json
Purpose: notes or feedback from doctors to patients or appointments
{
  _id: ObjectId("..."),
  patient_id: ObjectId("..."),
  doctor_id: ObjectId("..."),
  appointment_id: ObjectId("..."),
  content: "Patient shows improvement. Continue current plan.",
  created_at: ISODate("...")
}
### Collection: chat_messages
```json
Purpose: optional chat between users
{
  _id: ObjectId("..."),
  from_user_id: ObjectId("..."),
  to_user_id: ObjectId("..."),
  appointment_id: ObjectId("..."), // optional
  content: "Hello, please remember to take meds.",
  timestamp: ISODate("..."),
  attachments: [
    { filename: "lab.png", gridfs_id: ObjectId("...") }
  ]
}
### Collection: payments
```json
Purpose: payment history
{
  _id: ObjectId("..."),
  patient_id: ObjectId("..."),
  amount: 100.00,
  currency: "USD",
  status: "completed",
  method: "card",
  timestamp: ISODate("..."),
  appointment_id: ObjectId("...")
}
### Collection: documents
```json
Purpose: uploaded files (medical records, scans, etc.)
{
  _id: ObjectId("..."),
  owner_type: "patient" | "doctor" | "appointment" | "clinic",
  owner_id: ObjectId("..."),
  filename: "scan1.png",
  mime_type: "image/png",
  size: 204800,
  storage_ref: "gridfs/abc123" // or s3/key/reference
  uploaded_at: ISODate("...")
}
