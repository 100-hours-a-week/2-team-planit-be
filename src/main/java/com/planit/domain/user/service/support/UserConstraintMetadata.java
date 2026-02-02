package com.planit.domain.user.service.support;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class UserConstraintMetadata {

    private static final String TABLE_NAME = "users";

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, String> constraintColumnMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (!StringUtils.hasText(schema)) {
            return;
        }
        jdbcTemplate.query(
            """
            SELECT CONSTRAINT_NAME, COLUMN_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = ?
              AND TABLE_NAME = ?
              AND CONSTRAINT_NAME <> 'PRIMARY'
            """,
            new Object[]{schema, TABLE_NAME},
            (rs) -> {
                while (rs.next()) {
                    constraintColumnMap.put(rs.getString("CONSTRAINT_NAME"), rs.getString("COLUMN_NAME"));
                }
                return null;
            }
        );
    }

    public Optional<String> findColumnByConstraint(String constraintName) {
        if (!StringUtils.hasText(constraintName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(constraintColumnMap.get(constraintName));
    }
}
