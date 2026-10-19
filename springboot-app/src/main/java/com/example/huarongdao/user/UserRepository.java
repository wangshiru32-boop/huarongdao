package com.example.huarongdao.user;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** 用户表 JDBC 仓库：SQL 集中在这里，业务层不直接拼 SQL。 */
@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> findByUsername(String username) {
        List<UserAccount> rows = jdbc.query(
                "SELECT id, username, password_hash FROM users WHERE username = ?",
                (result, row) -> new UserAccount(
                        result.getLong("id"),
                        result.getString("username"),
                        result.getString("password_hash")),
                username);
        return rows.stream().findFirst();
    }

    public UserAccount create(String username, String passwordHash) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                    new String[]{"id"});
            statement.setString(1, username);
            statement.setString(2, passwordHash);
            return statement;
        }, keys);
        Number id = keys.getKey();
        if (id == null) throw new IllegalStateException("创建用户后没有获得主键");
        return new UserAccount(id.longValue(), username, passwordHash);
    }
}
