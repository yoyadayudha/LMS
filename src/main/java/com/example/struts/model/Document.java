package com.example.struts.model;

import java.io.Serializable;

public class Document implements Serializable{
    private static final long serialVersionUID = 1L;

    private String kodeDokumen;
    private String namaDokumen;
    private String kategoriDokumen;
    private String tipeDosir;
    private boolean aktif;

    public Document(){
    }

    public Document(String kodeDokumen, String namaDokumen, String kategoriDokumen, String tipeDosir, boolean aktif) {
        this.kodeDokumen = kodeDokumen;
        this.namaDokumen = namaDokumen;
        this.kategoriDokumen = kategoriDokumen;
        this.tipeDosir = tipeDosir;
        this.aktif = aktif;
    }
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    public String getKodeDokumen() {
        return kodeDokumen;
    }
    public void setKodeDokumen(String kodeDokumen) {
        this.kodeDokumen = kodeDokumen;
    }
    public String getNamaDokumen() {
        return namaDokumen;
    }
    public void setNamaDokumen(String namaDokumen) {
        this.namaDokumen = namaDokumen;
    }
    public String getKategoriDokumen() {
        return kategoriDokumen;
    }
    public void setKategoriDokumen(String kategoriDokumen) {
        this.kategoriDokumen = kategoriDokumen;
    }
    public String getTipeDosir() {
        return tipeDosir;
    }
    public void setTipeDosir(String tipeDosir) {
        this.tipeDosir = tipeDosir;
    }
    public boolean isAktif() {
        return aktif;
    }
    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }

    
}
