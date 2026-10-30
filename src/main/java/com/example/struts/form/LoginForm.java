package com.example.struts.form;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

import javax.servlet.http.HttpServletRequest;

public class LoginForm extends ActionForm {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;

    public LoginForm() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.username = null;
        this.password = null;
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();

        if (username == null || username.trim().isEmpty()) {
            errors.add("username", new ActionMessage("errors.required", "Nama Pengguna"));
        }

        if (password == null || password.trim().isEmpty()) {
            errors.add("password", new ActionMessage("errors.required", "Kata Sandi"));
        }

        return errors;
    }
}
