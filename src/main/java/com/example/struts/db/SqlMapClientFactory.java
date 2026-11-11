package com.example.struts.db;

import com.ibatis.common.resources.Resources;
import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

import java.io.Reader;

public final class SqlMapClientFactory {

    private static volatile SqlMapClient instance;

    private SqlMapClientFactory() {
    }

    public static SqlMapClient getSqlMapClient() {
        if (instance == null) {
            synchronized (SqlMapClientFactory.class) {
                if (instance == null) {
                    try (Reader     reader = Resources.getResourceAsReader("SqlMapConfig.xml")) {
                        instance = SqlMapClientBuilder.buildSqlMapClient(reader);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to initialize iBATIS SqlMapClient", e);
                    }
                }
            }
        }
        return instance;
    }
}
