package com.example.struts.form;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

public class DocumentForm extends ActionForm{
    private static final long serialVersionUID = 1L;

    private String kodeDokumen;
    private String namaDokumen;
    private String kategoriDokumen;
    private String tipeDosir;
    private boolean aktif;
    private String editMode;
    private String action = "list";

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

    public boolean getAktif() {
        return aktif;
    }
    public void setAktif(boolean aktif) {
        this.aktif = aktif;
    }
    public String getEditMode() {
        return editMode;
    }
    public void setEditMode(String editMode) {
        this.editMode = editMode;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request){
        this.kodeDokumen = null;
        this.namaDokumen = null;
        this.kategoriDokumen = null;
        this.tipeDosir = null;
        this.aktif = false;
        this.editMode = null;
        this.action = "list";
    }
    
    public ActionErrors validateFields(){
        ActionErrors errors = new ActionErrors();

        if(kodeDokumen == null || kodeDokumen.trim().isEmpty()){
            errors.add("kodeDokumen", new ActionMessage("errors.required", "Kode Dokumen"));
        } else if (kodeDokumen.trim().length() > 32){
            errors.add("kodeDokumen", new ActionMessage("errors.maxlength", "Kode Dokumen", "32"));
        }

        if(namaDokumen == null || namaDokumen.trim().isEmpty()){
            errors.add("namaDokumen", new ActionMessage("errors.required", "Nama Dokumen"));
        } else if (namaDokumen.trim().length() > 255){
            errors.add("namaDokumen", new ActionMessage("errors.maxlength", "Nama Dokumen", "255"));
        }

        if(kategoriDokumen == null || kategoriDokumen.trim().isEmpty()){
            errors.add("kategoriDokumen", new ActionMessage("errors.required", "Kategori Dokumen"));
        }

        if(tipeDosir == null || tipeDosir.trim().isEmpty()){
            errors.add("tipeDosir", new ActionMessage("errors.required", "Tipe Dosir"));
        }

        return errors;
    }
    

}
