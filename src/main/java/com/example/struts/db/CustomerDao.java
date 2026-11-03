package com.example.struts.db;

import com.example.struts.model.Customer;
import com.ibatis.sqlmap.client.SqlMapClient;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class CustomerDao {

    private final SqlMapClient sqlMap = SqlMapClientFactory.getSqlMapClient();

    @SuppressWarnings("unchecked")
    public List<Customer> list() {
        try {
            return sqlMap.queryForList("selectAllCustomers");
        } catch (SQLException e) {
            throw new DataAccessException("list failed", e);
        }
    }

    public Customer findById(String id) {
        try {
            return (Customer) sqlMap.queryForObject("selectCustomerById", id);
        } catch (SQLException e) {
            throw new DataAccessException("findById failed", e);
        }
    }

    public void insert(Customer c) {
        try {
            sqlMap.insert("insertCustomer", c);
        } catch (SQLException e) {
            throw new DataAccessException("insert failed", e);
        }
    }

    public void update(Customer c) {
        try {
            sqlMap.update("updateCustomer", c);
        } catch (SQLException e) {
            throw new DataAccessException("update failed", e);
        }
    }

    public void delete(String id) {
        try {
            sqlMap.delete("deleteCustomer", id);
        } catch (SQLException e) {
            throw new DataAccessException("delete failed", e);
        }
    }

    /** Idempotent schema creation + conditional demo seed. Call once at startup. */
    public void initSchemaAndSeed() {
        try (Connection conn = sqlMap.getDataSource().getConnection();
             Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS CUSTOMER (" +
                "  id    VARCHAR(32) PRIMARY KEY," +
                "  name  VARCHAR(255)," +
                "  email VARCHAR(255)," +
                "  phone VARCHAR(64)," +
                "  city  VARCHAR(255))");
        } catch (SQLException e) {
            throw new DataAccessException("schema init failed", e);
        }

        if (list().isEmpty()) {
            insert(new Customer("CUST-001", "Budi Santoso",
                    "budi@example.com", "08123456789", "Jakarta"));
            insert(new Customer("CUST-002", "Siti Aminah",
                    "siti@example.com", "08234567890", "Surabaya"));
        }
    }

    /** Next id from the max existing numeric suffix + 1, collision-safe after deletes. */
    public String nextId() {
        int max = 0;
        for (Customer c : list()) {
            String id = c.getId();
            if (id != null && id.startsWith("CUST-")) {
                try {
                    max = Math.max(max, Integer.parseInt(id.substring(5)));
                } catch (NumberFormatException ignore) {
                    // skip non-conforming ids
                }
            }
        }
        return String.format("CUST-%03d", max + 1);
    }
}
