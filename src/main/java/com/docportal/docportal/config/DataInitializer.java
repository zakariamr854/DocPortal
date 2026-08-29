package com.docportal.docportal.config;

import com.docportal.docportal.entities.Category;
import com.docportal.docportal.entities.User;
import com.docportal.docportal.enums.Role;
import com.docportal.docportal.repositories.CategoryRepository;
import com.docportal.docportal.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    public CommandLineRunner seedUsers() {
        return args -> {

            jdbcTemplate.update("UPDATE document SET visibility = 'PRIVATE' WHERE visibility = 'RESTRICTED'");



            jdbcTemplate.execute("ALTER TABLE document_history DROP CONSTRAINT IF EXISTS document_history_action_check");

            createIfMissing("admin", "admin123", "Administrateur DocPortal", "admin@docportal.com", Role.ADMIN);
            createIfMissing("user", "user123", "Utilisateur DocPortal", "user@docportal.com", Role.USER);
            createIfMissing("sara", "sara123", "Sara Benali", "sara@docportal.com", Role.USER);
            createIfMissing("karim", "karim123", "Karim El Amrani", "karim@docportal.com", Role.USER);
            createIfMissing("yasmine", "yasmine123", "Yasmine Tazi", "yasmine@docportal.com", Role.USER);
            createIfMissing("mehdi", "mehdi123", "Mehdi Alaoui", "mehdi@docportal.com", Role.USER);
            createIfMissing("nadia", "nadia123", "Nadia Chraibi", "nadia@docportal.com", Role.USER);
            createIfMissing("viewer", "viewer123", "Lecteur DocPortal", "viewer@docportal.com", Role.VIEWER);

            createCategoryIfMissing("Contrats", "Contrats et accords");
            createCategoryIfMissing("Factures", "Factures fournisseurs et clients");
            createCategoryIfMissing("Rapports", "Rapports internes et externes");
            createCategoryIfMissing("Pièces jointes", "Pièces jointes diverses");
            createCategoryIfMissing("Documents administratifs", "Documents RH et administratifs");
            createCategoryIfMissing("Autres", "Documents non classés");
        };
    }

    private void createCategoryIfMissing(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            return;
        }
        categoryRepository.save(Category.builder()
                .name(name)
                .description(description)
                .active(true)
                .build());
    }

    private void createIfMissing(String username, String rawPassword, String fullName, String email, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .email(email)
                .role(role)
                .active(true)
                .build());
    }
}
