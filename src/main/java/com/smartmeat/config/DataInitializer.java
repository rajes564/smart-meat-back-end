package com.smartmeat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.smartmeat.entity.User;
import com.smartmeat.enums.Role;
import com.smartmeat.repository.UserRepository;

/**
 * Seeds default ADMIN on first run.
 * Password stored as SHA256("Admin@123") via Sha256Util.hashForStorage().
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository staffRepo;

    @Override
    public void run(String... args) {
        if (!staffRepo.existsByMobile("9000474104")) {
            User admin = User.builder()
                .name("Rajesh")
                .password("$2a$12$NKyQ5yMDwWSov4ifGxKJEezNcRDNB9EQ/M7TWa.AxTCjgg3tDAcWm")
                .mobile("9000474104")
                .email("admin@royalmeatmart.com")
                .role(Role.ADMIN)
                .active(true)
                .build();
            staffRepo.save(admin);
            System.out.println("✅ Default admin seeded → 9000474104 / Admin@123");
        }
        if (!staffRepo.existsByMobile("9000474103")) {
        	User seller = User.builder()
        			.name("Rajesh")
        			.password("$2a$12$NKyQ5yMDwWSov4ifGxKJEezNcRDNB9EQ/M7TWa.AxTCjgg3tDAcWm")
        			.mobile("9000474103")
        			.email("admin@royalmeatmart.com")
        			.role(Role.SELLER)
        			.active(true)
        			.build();
        	staffRepo.save(seller);
        	System.out.println("✅ Default Seller seeded → 9000474103 / Admin@123");
        }
    }
}