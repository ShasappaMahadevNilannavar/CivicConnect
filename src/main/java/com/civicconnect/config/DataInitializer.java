package com.civicconnect.config;

import com.civicconnect.model.Category;
import com.civicconnect.model.Complaint;
import com.civicconnect.model.Department;
import com.civicconnect.model.StatusHistory;
import com.civicconnect.model.User;
import com.civicconnect.model.enums.ComplaintStatus;
import com.civicconnect.model.enums.Priority;
import com.civicconnect.model.enums.Role;
import com.civicconnect.repository.CategoryRepository;
import com.civicconnect.repository.ComplaintRepository;
import com.civicconnect.repository.DepartmentRepository;
import com.civicconnect.repository.StatusHistoryRepository;
import com.civicconnect.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DepartmentRepository departmentRepository;
    private final ComplaintRepository complaintRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email:admin@civicconnect.com}")
    private String adminEmail;

    @Value("${app.admin.default-password:Admin@123}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           DepartmentRepository departmentRepository,
                           ComplaintRepository complaintRepository,
                           StatusHistoryRepository statusHistoryRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.departmentRepository = departmentRepository;
        this.complaintRepository = complaintRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        logger.info("Initializing CivicConnect default seed data...");

        seedCategories();
        seedDepartments();
        User admin = seedAdminUser();
        User citizen = seedCitizenUser();
        seedSampleComplaints(citizen, admin);

        logger.info("CivicConnect data initialization completed successfully.");
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> categories = List.of(
                    new Category("Road Damage", "Potholes, cracks, and road surface hazards", "fas fa-road"),
                    new Category("Streetlight", "Non-functional, broken, or flickering street lights", "fas fa-lightbulb"),
                    new Category("Garbage", "Garbage overflow, uncollected trash, and waste accumulation", "fas fa-trash"),
                    new Category("Water Leakage", "Main pipe leaks, burst lines, and clean water wastage", "fas fa-tint"),
                    new Category("Drainage", "Blocked storm drains, sewage overflow, and waterlogging", "fas fa-water"),
                    new Category("Public Safety", "Open manholes, fallen electrical lines, and hazards", "fas fa-shield-alt"),
                    new Category("Other", "General community issues and municipal queries", "fas fa-ellipsis-h")
            );
            categoryRepository.saveAll(categories);
            logger.info("Seeded {} default categories.", categories.size());
        }
    }

    private void seedDepartments() {
        if (departmentRepository.count() == 0) {
            List<Department> departments = List.of(
                    new Department("Public Works", "Roads, pavements, bridges, and infrastructure repair", "publicworks@civicconnect.com"),
                    new Department("Electrical Services", "Street illumination, power lines, and electrical safety", "electrical@civicconnect.com"),
                    new Department("Sanitation & Waste", "Garbage disposal, landfill management, and public hygiene", "sanitation@civicconnect.com"),
                    new Department("Water & Sewerage", "Water supply, leak fixing, and storm water drainage", "water@civicconnect.com"),
                    new Department("Public Safety & Traffic", "Emergency municipal hazards and traffic obstruction", "safety@civicconnect.com")
            );
            departmentRepository.saveAll(departments);
            logger.info("Seeded {} default departments.", departments.size());
        }
    }

    private User seedAdminUser() {
        return userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User admin = new User();
            admin.setName("System Administrator");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setPhone("+1-555-0100");
            admin.setAddress("Municipal Civic Center, Suite 101");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            User saved = userRepository.save(admin);
            logger.info("Default Admin account seeded: {}", adminEmail);
            return saved;
        });
    }

    private User seedCitizenUser() {
        String citizenEmail = "citizen@civicconnect.com";
        return userRepository.findByEmail(citizenEmail).orElseGet(() -> {
            User citizen = new User();
            citizen.setName("John Citizen");
            citizen.setEmail(citizenEmail);
            citizen.setPassword(passwordEncoder.encode("Citizen@123"));
            citizen.setPhone("+1-555-0199");
            citizen.setAddress("42 Greenfield Avenue, Ward 4");
            citizen.setRole(Role.CITIZEN);
            citizen.setEnabled(true);
            User saved = userRepository.save(citizen);
            logger.info("Default sample Citizen account seeded: {}", citizenEmail);
            return saved;
        });
    }

    private void seedSampleComplaints(User citizen, User admin) {
        if (complaintRepository.count() == 0 && citizen != null) {
            Category roadCat = categoryRepository.findByName("Road Damage").orElse(null);
            Category garbageCat = categoryRepository.findByName("Garbage").orElse(null);
            Category waterCat = categoryRepository.findByName("Water Leakage").orElse(null);
            Category lightCat = categoryRepository.findByName("Streetlight").orElse(null);

            if (roadCat != null) {
                Complaint c1 = new Complaint();
                c1.setReferenceId("CC-20261101-A101");
                c1.setTitle("Massive Pothole on Oak Street Junction");
                c1.setDescription("Deep pothole causing traffic obstruction and near-accidents for two-wheelers during evening hours.");
                c1.setUser(citizen);
                c1.setCategory(roadCat);
                c1.setStatus(ComplaintStatus.IN_PROGRESS);
                c1.setPriority(Priority.HIGH);
                c1.setLatitude(12.9716);
                c1.setLongitude(77.5946);
                c1.setLocationDescription("Corner of Oak Street & 5th Cross, near Central Park entrance");
                c1 = complaintRepository.save(c1);

                StatusHistory h1 = new StatusHistory(c1, null, ComplaintStatus.SUBMITTED, "Initial citizen submission", citizen);
                StatusHistory h2 = new StatusHistory(c1, ComplaintStatus.SUBMITTED, ComplaintStatus.UNDER_REVIEW, "Assigned for field review", admin);
                StatusHistory h3 = new StatusHistory(c1, ComplaintStatus.UNDER_REVIEW, ComplaintStatus.APPROVED, "Approved for emergency asphalt resurfacing", admin);
                StatusHistory h4 = new StatusHistory(c1, ComplaintStatus.APPROVED, ComplaintStatus.IN_PROGRESS, "Road repair crew dispatched with asphalt mixer", admin);
                statusHistoryRepository.saveAll(List.of(h1, h2, h3, h4));
            }

            if (garbageCat != null) {
                Complaint c2 = new Complaint();
                c2.setReferenceId("CC-20261102-B202");
                c2.setTitle("Overflowing Garbage Dumpster behind City Market");
                c2.setDescription("Commercial waste not collected for 4 days. Strong odor spreading and stray animals scattering debris.");
                c2.setUser(citizen);
                c2.setCategory(garbageCat);
                c2.setStatus(ComplaintStatus.RESOLVED_PENDING_VERIFICATION);
                c2.setPriority(Priority.CRITICAL);
                c2.setLatitude(12.9750);
                c2.setLongitude(77.5990);
                c2.setLocationDescription("Rear service lane of City Vegetable Market");
                c2 = complaintRepository.save(c2);

                StatusHistory h1 = new StatusHistory(c2, null, ComplaintStatus.SUBMITTED, "Complaint submitted", citizen);
                StatusHistory h2 = new StatusHistory(c2, ComplaintStatus.SUBMITTED, ComplaintStatus.IN_PROGRESS, "Sanitation team dispatched with compactor truck", admin);
                StatusHistory h3 = new StatusHistory(c2, ComplaintStatus.IN_PROGRESS, ComplaintStatus.RESOLVED_PENDING_VERIFICATION, "Garbage cleared and area disinfected with bleaching powder. Awaiting citizen confirmation.", admin);
                statusHistoryRepository.saveAll(List.of(h1, h2, h3));
            }

            if (waterCat != null) {
                Complaint c3 = new Complaint();
                c3.setReferenceId("CC-20261103-C303");
                c3.setTitle("Drinking Water Pipeline Leakage");
                c3.setDescription("Fresh clean water gushing from underground pipeline crack on pavement. Thousands of liters lost.");
                c3.setUser(citizen);
                c3.setCategory(waterCat);
                c3.setStatus(ComplaintStatus.UNDER_REVIEW);
                c3.setPriority(Priority.HIGH);
                c3.setLatitude(12.9690);
                c3.setLongitude(77.6010);
                c3.setLocationDescription("Opposite Government High School, Main Gate");
                c3 = complaintRepository.save(c3);

                StatusHistory h1 = new StatusHistory(c3, null, ComplaintStatus.SUBMITTED, "Complaint lodged by citizen", citizen);
                StatusHistory h2 = new StatusHistory(c3, ComplaintStatus.SUBMITTED, ComplaintStatus.UNDER_REVIEW, "Forwarded to Water & Sewerage Board engineers", admin);
                statusHistoryRepository.saveAll(List.of(h1, h2));
            }

            if (lightCat != null) {
                Complaint c4 = new Complaint();
                c4.setReferenceId("CC-20261104-D404");
                c4.setTitle("Broken Streetlight Pole outside Community Health Clinic");
                c4.setDescription("Entire lane pitch dark at night. Safety hazard for patients, doctors, and pedestrians.");
                c4.setUser(citizen);
                c4.setCategory(lightCat);
                c4.setStatus(ComplaintStatus.SUBMITTED);
                c4.setPriority(Priority.MEDIUM);
                c4.setLatitude(12.9665);
                c4.setLongitude(77.5920);
                c4.setLocationDescription("Pole #EL-412, Lane 2, Health Clinic stretch");
                c4 = complaintRepository.save(c4);

                StatusHistory h1 = new StatusHistory(c4, null, ComplaintStatus.SUBMITTED, "Complaint submitted by citizen", citizen);
                statusHistoryRepository.save(h1);
            }

            logger.info("Seeded initial sample complaints with complete workflow history.");
        }
    }
}
