package com.ciaranchaney.mcpdb.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
public class Repositories {

    private final JdbcTemplate jdbc;
    public Repositories(JdbcTemplate jdbc){ this.jdbc = jdbc; }

    public Map<String,Object> getCustomerByEmail(String email) {
        return jdbc.query("select id, email, name from customers where email = ? limit 1",
                ps -> ps.setString(1, email),
                rs -> rs.next() ? Map.of(
                        "id", rs.getLong("id"),
                        "email", rs.getString("email"),
                        "name", rs.getString("name")
                ) : Map.of());
    }

    public List<Map<String,Object>> exportOrders(LocalDate from, LocalDate to, int limit){
        final String sql = """
        select id, customer_id, created_at, total_cents
        from public.orders
        where created_at >= ? and created_at < ?
        order by created_at asc
        limit cast(? as integer)
    """;
        return jdbc.query(sql, ps -> {
            ps.setObject(1, from);
            ps.setObject(2, to);
            ps.setInt(3, limit);
        }, (rs, rowNum) -> Map.of(
                "id", rs.getLong("id"),
                "customer_id", rs.getLong("customer_id"),
                "created_at", rs.getTimestamp("created_at").toInstant(),
                "total_cents", rs.getLong("total_cents")
        ));
    }

}
