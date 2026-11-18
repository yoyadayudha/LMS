package com.example.struts.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.example.struts.model.User;
import com.ibatis.sqlmap.client.SqlMapClient;

public class UserDao {

    private final SqlMapClient sqlMap = SqlMapClientFactory.getSqlMapClient();

    public User findByUsername(String username){
        try{
            return (User) sqlMap.queryForObject("selectUserByUsername", username);
        } catch (SQLException e){
            throw new DataAccessException("cari user gagal", e);
        }
    }

    public void initSchemaAndSeed(){
        try (Connection conn = sqlMap.getDataSource().getConnection();
            Statement st = conn.createStatement()){
            st.execute(
                "CREATE TABLE IF NOT EXISTS USERS ("
                + " username VARCHAR(50) PRIMARY KEY,"
                + " password VARCHAR(50) NOT NULL,"
                + " nama_lengkap VARCHAR(100))");
            st.execute("MERGE INTO USERS (username, password, nama_lengkap) KEY(username) "
                    + "VALUES ('admin', 'user123', 'User Satu')");
            conn.commit();
        } catch (SQLException e){
            throw new DataAccessException("buat tabel users gagal", e);
        }
    }
}
