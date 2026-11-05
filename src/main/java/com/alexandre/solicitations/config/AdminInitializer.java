package com.alexandre.solicitations.config;

import com.alexandre.solicitations.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Only an ADMIN creates internal users, so the first one comes from ADMIN_EMAIL / ADMIN_PASSWORD. */
@Component
@RequiredArgsConstructor
class AdminInitializer implements ApplicationRunner {

    private final UserService userService;

    @Value("${admin.email}")
    private String email;

    @Value("${admin.password}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        userService.createAdminIfMissing(email, password);
    }
}
