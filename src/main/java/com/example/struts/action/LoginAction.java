package com.example.struts.action;

import com.example.struts.form.LoginForm;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginAction extends Action {

    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        LoginForm loginForm = (LoginForm) form;
        String username = loginForm.getUsername();
        String password = loginForm.getPassword();

        // Validasi credential (demo: admin / admin123)
        if ("admin".equalsIgnoreCase(username) && "admin123".equals(password)) {
            HttpSession session = request.getSession(true);
            session.setAttribute("user", username);
            session.setAttribute("role", "ADMINISTRATOR");
            return mapping.findForward("success");
        }

        // Kredensial tidak valid
        ActionMessages errors = new ActionMessages();
        errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("login.error.invalid"));
        saveErrors(request, errors);

        return mapping.findForward("failure");
    }
}
