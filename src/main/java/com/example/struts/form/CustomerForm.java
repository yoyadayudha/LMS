package com.example.struts.form;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import javax.servlet.http.HttpServletRequest;

public class CustomerForm extends ActionForm {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String action = "list"; // default action

    public CustomerForm() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.id = null;
        this.name = null;
        this.email = null;
        this.phone = null;
        this.city = null;
        this.action = "list";
    }

    public ActionErrors validateFields() {
        ActionErrors errors = new ActionErrors();

        if (name == null || name.trim().isEmpty()) {
            errors.add("name", new ActionMessage("errors.required", "Nama Lengkap"));
        }

        if (email == null || email.trim().isEmpty()) {
            errors.add("email", new ActionMessage("errors.required", "Email"));
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("email", new ActionMessage("errors.email", "Email"));
        }

        if (phone == null || phone.trim().isEmpty()) {
            errors.add("phone", new ActionMessage("errors.required", "Nomor Telepon"));
        }

        return errors;
    }
}
