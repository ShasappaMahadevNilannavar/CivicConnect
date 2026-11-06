# 🏛️ CivicConnect — Community Problem Reporting & Resolution Platform

**Hackathon Problem Statement 14 (PS-14)**: *Community Problem Reporting and Resolution Platform*

CivicConnect empowers citizens to report everyday municipal hazards (potholes, broken streetlights, overflowing garbage, clean water leaks, and drainage issues) with photographic evidence and geographic coordinates. City administrators review complaints, evaluate potential duplicate reports, assign specialized municipal departments, track progress with immutable audit notes, and provide verified photographic evidence upon repair. Citizens verify or dispute the resolution.

---

## 🌟 Key Features

### For Citizens
* **Secure Registration & Login**: BCrypt password hashing, session-based role authorization (`ROLE_CITIZEN`).
* **Citizen Dashboard**: Live metrics for submitted, pending review, in-progress, and resolved complaints.
* **Photo & Geo Reporting**: Upload up to 5 photos with client and server-side MIME-type and size validation (max 5MB each). Drop pins directly on **OpenStreetMap via Leaflet** with optional geolocation.
* **Unique Reference IDs**: Standardized tracking codes (`CC-YYYYMMDD-XXXXX`) generated upon filing.
* **My Complaints Page**: Full-text search by title or Reference ID, status filtering, and pagination.
* **Interactive Status Timeline**: Visual history of every transition with notes, timestamps, and staff signatures.
* **Citizen Resolution Verification**: When marked resolved by administrators, citizens can confirm the fix (`VERIFIED_RESOLVED`) or dispute an inadequate repair (`IN_PROGRESS`).
* **Real-time In-App Notifications**: Alerts when reports change state or require feedback.

### Advanced Innovations
* **Multilingual Engine (All 22 Indian Scheduled Languages)**: Complete localization supporting Assamese, Bengali, Bodo, Dogri, Gujarati, Hindi, Kannada, Kashmiri, Konkani, Maithili, Malayalam, Manipuri, Marathi, Nepali, Odia, Punjabi, Sanskrit, Santali, Sindhi, Tamil, Telugu, and Urdu + English with instant live switching, native script rendering, and searchable language dropdown.
* **Intelligent AI Chatbot (CivicBot)**: 3D floating orb launcher, full conversation history, suggested prompt pills, typing animation, municipal problem reporting guidance, and secure backend routing supporting Gemini API with an automatic offline knowledge engine fallback.
* **Google Maps 3D Civic Explorer (`/map/3d`)**: High-resolution Google Satellite 3D hybrid view, 60° camera tilt, compass heading dial, 3D extruded city buildings, live civic markers with radar rings, and quick fly-to views for 10 major Indian metros.

### For Administrators
* **Executive Metrics Dashboard**: Real-time counts for Total, Submitted, Under Review, Approved, In Progress, Resolved Pending, Verified Resolved, and Rejected.
* **Visual Analytics**: Interactive **Chart.js** status distribution doughnut chart and category bar chart.
* **Reports Management Queue**: Filter by keyword search, status, problem category, and priority (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
* **Smart Duplicate Detection Engine**: Automatic text similarity matching (Jaro-Winkler algorithm) and geographic proximity (Haversine formula within configurable radius). Duplicates are displayed for manual verification without auto-deleting citizen reports.
* **Department Dispatch**: Assign reports to municipal departments (Public Works, Electrical, Sanitation, Water & Sewerage, Public Safety) and designated staff.
* **Mandatory Resolution Evidence**: Before completing a fix, administrators must attach photographic evidence and detailed repair notes.
* **Immutable Audit Trail**: All status transitions, assignments, and evidence uploads are recorded in `audit_logs` and `status_history`.

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Backend Framework** | Java 17+, Spring Boot 3.4.1 |
| **Security** | Spring Security 6, BCrypt Password Encoder, CSRF Protection |
| **Persistence / ORM** | Spring Data JPA, Hibernate, MySQL Connector/J |
| **Database** | MySQL 8.0 (Normalized 10-table relational schema) |
| **Frontend UI** | Thymeleaf 3, Bootstrap 5.3.2, Font Awesome 6.5 |
| **Maps & Geolocation**| Leaflet.js 1.9.4, OpenStreetMap (Zero external API keys required) |
| **Data Visualizations**| Chart.js 4.4.1 (Responsive canvas charts) |
| **Duplicate Engine** | Apache Commons Text (Jaro-Winkler) & Haversine Geodesic Distance |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, In-Memory H2 |

---

## 📁 Project Structure

```
CivicConnect/
├── pom.xml                                  # Maven build descriptor
├── .env.example                             # Environment variable template
├── .gitignore                               # Production gitignore
├── README.md                                # Comprehensive documentation
├── API_DOCUMENTATION.md                     # REST API reference guide
├── src/
│   ├── main/
│   │   ├── java/com/civicconnect/
│   │   │   ├── CivicConnectApplication.java # Spring Boot main entrypoint
│   │   │   ├── config/                      # Security, Web, JPA, DataInitializer
│   │   │   ├── controller/                  # Auth, Citizen, Admin, Map, API controllers
│   │   │   ├── dto/                         # Request & response transfer objects
│   │   │   ├── model/                       # JPA entities & enums (Role, Status, Priority)
│   │   │   ├── repository/                  # Spring Data JPA repositories
│   │   │   ├── security/                    # CustomUserDetails & UserDetailsService
│   │   │   └── service/                     # Business logic, Duplicates, Files, Auditing
│   │   └── resources/
│   │       ├── application.properties       # Spring configuration with env variables
│   │       ├── schema.sql                   # MySQL 8 normalized DDL script
│   │       ├── data.sql                     # Seed categories and departments
│   │       ├── static/
│   │       │   ├── css/style.css            # Custom responsive styling & design system
│   │       │   └── js/app.js                # Leaflet maps, notifications, charts, previews
│   │       └── templates/                   # Thymeleaf templates
│   │           ├── index.html               # Landing page
│   │           ├── map.html                 # Public OpenStreetMap view
│   │           ├── fragments/               # Header, footer fragments
│   │           ├── auth/                    # Login, Register
│   │           ├── citizen/                 # Dashboard, New Report, Complaints, Details, Profile
│   │           ├── admin/                   # Dashboard, Queue, Details, Duplicate Review
│   │           └── error/                   # 404, 500 error pages
│   └── test/                                # JUnit 5 and Mockito test suites
```

---

## 🚀 Quick Start & Installation

### Prerequisites
1. **Java Development Kit (JDK 17 or higher)**
2. **Apache Maven 3.8+**
3. **MySQL Server 8.0+** running locally or via Docker

### 1. Database Setup
Launch MySQL and create the database:
```sql
CREATE DATABASE civicconnect CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Configure Environment Variables
Copy `.env.example` to your environment or configure `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/civicconnect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=your_mysql_password
```

### 3. Build & Run Application
```bash
# Compile and package application
mvn clean package -DskipTests

# Run Spring Boot application
mvn spring-boot:run
```
Alternatively, run the packaged JAR:
```bash
java -jar target/civicconnect-1.0.0.jar
```

The application will start on: **`http://localhost:8080`**

---

## ☁️ Live Cloud Deployment Guide (Public HTTPS URL)

CivicConnect is pre-configured for **1-click cloud deployment** on platforms like **Render**, **Railway**, and **AWS**.

### Deploying to Render.com (Recommended — 100% Free with HTTPS)

1. **Sign Up**: Go to [Render.com](https://render.com) and sign in using your GitHub account (`ShasappaMahadevNilannavar`).
2. **Create New Web Service**:
   - In the Render Dashboard, click **New +** → **Web Service**.
   - Select **Build and deploy from a Git repository**.
   - Choose your repository: `ShasappaMahadevNilannavar/CivicConnect`.
3. **Configure Settings**:
   - **Name**: `civicconnect` (or any custom name)
   - **Region**: Choose the closest region (e.g., *Singapore* or *Oregon*)
   - **Branch**: `main`
   - **Runtime**: **Docker** (Render will automatically detect the production `Dockerfile`)
   - **Instance Type**: **Free** ($0/month)
4. **Environment Variables**:
   Click **Advanced** → **Add Environment Variable**:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   *(Optional for Cloud MySQL: Add `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` if connecting to external MySQL; otherwise, the built-in resilient production database runs automatically).*
5. **Deploy**:
   - Click **Create Web Service**.
   - Render will build the Docker container, run Spring Boot, and assign a live public HTTPS address:
     `https://civicconnect.onrender.com`

---

## 🔑 Demo Test Accounts (Safe Seed Data)

On first startup, `DataInitializer` seeds default categories, departments, and safe demo credentials:

| Role | Email | Password | Pre-seeded Features |
|---|---|---|---|
| **Administrator** | `admin@civicconnect.com` | `Admin@123` | Full access to review queue, analytics, status transitions, duplicate checks |
| **Citizen** | `citizen@civicconnect.com` | `Citizen@123` | Pre-loaded complaints with photos, GPS coordinates, and timeline history |

---

## 🛡️ Security Architecture

1. **Password Protection**: BCrypt password encoding with random salting. Plaintext passwords are never stored.
2. **Role-Based Authorization**:
   - `/admin/**` strictly enforced by Spring Security with `hasRole('ADMIN')`.
   - `/citizen/**` strictly restricted to `hasRole('CITIZEN')`.
   - Citizens can only view and verify complaints owned by their user ID.
3. **Input Sanitization & Validation**:
   - Jakarta Validation (`@NotBlank`, `@Email`, `@Size`) prevents malformed inputs.
   - Spring Data JPA parameterized queries prevent SQL injection.
   - Thymeleaf context escaping mitigates Cross-Site Scripting (XSS).
4. **File Upload Hardening**:
   - Enforces strict whitelist of MIME types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`.
   - Files are saved with randomly generated UUIDs; original client filenames are never used as filesystem paths.
   - Maximum upload size capped at 5MB per file.
5. **CSRF Protection**: Spring Security CSRF tokens are integrated into all forms and AJAX calls.

---

## 🧪 Automated Testing

Run the test suite:
```bash
mvn test
```

The test suite covers:
* **`UserServiceTest`**: Registration validation, email uniqueness, password hashing, and role assignment.
* **`ComplaintServiceTest`**: Creation, reference ID generation, valid vs invalid status transitions, resolution evidence requirement, feedback disputes, and non-owner access rejection.
* **`DuplicateDetectionServiceTest`**: Jaro-Winkler description matching, Haversine geodesic distance, and exclusion of rejected reports.
* **`CivicConnectApplicationTests`**: Spring context and bean wiring verification.

---

## 📄 License & Hackathon Attribution

Developed for **Problem Statement 14 (PS-14): Community Problem Reporting and Resolution Platform**.
Open source under the MIT License.
