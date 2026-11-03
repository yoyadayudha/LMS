package com.example.struts.db;

import java.sql.Statement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.example.struts.model.Document;
import com.ibatis.sqlmap.client.SqlMapClient;

public class DocumentDao {

    private final SqlMapClient sqlMap = SqlMapClientFactory.getSqlMapClient();

    @SuppressWarnings ("unchecked")
    public List<Document> list(){
        try{
            return sqlMap.queryForList("selectAllDocuments");
        } catch (SQLException e){
            throw new DataAccessException("list dokumen gagal", e);
        }
    }

    public Document findById(String kodeDokumen){
        try{
            return (Document) sqlMap.queryForObject("selectDocumentById", kodeDokumen);
        } catch (SQLException e){
            throw new DataAccessException("cari dokumen gagal", e);
        }
    }

    public void insert(Document d){
        try{
            sqlMap.insert("insertDocument", d);
        } catch (SQLException e){
            throw new DataAccessException("tambah dokumen gagal", e);
        }
    }

    public void update(Document d){
        try{
            sqlMap.update("updateDocument", d);
        } catch (SQLException e){
            throw new DataAccessException("ubah dokumen gagal", e);
        }
    }

    public void delete(String kodeDokumen){
        try{
            sqlMap.delete("deleteDocument", kodeDokumen);
        } catch (SQLException e){
            throw new DataAccessException("hapus dokumen gagal", e);
        }
    }

    public void initSchemaAndSeed(){
        try (Connection conn = sqlMap.getDataSource().getConnection();
            Statement st = conn.createStatement()){
            st.execute(
                "CREATE TABLE IF NOT EXISTS DOCUMENT ("
                + " kode_dokumen VARCHAR(32) PRIMARY KEY,"
                + " nama_dokumen VARCHAR(255),"
                + " kategori_dokumen VARCHAR(100),"
                + " tipe_dosir VARCHAR(100),"
                + " aktif BOOLEAN)");
        } catch (SQLException e){
            throw new DataAccessException("buat tabel dokumen gagal", e);
        }
    }

}
