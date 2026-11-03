package com.example.struts.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import com.example.struts.db.CustomerDao;
import com.example.struts.form.CustomerForm;
import com.example.struts.model.Customer;

public class CustomerAction extends Action {

    private final CustomerDao dao = new CustomerDao();

    @Override
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        CustomerForm customerForm = (CustomerForm) form;
        String action = customerForm != null ? customerForm.getAction() : "list";
        if (action == null || action.trim().isEmpty()) {
            action = "list";
        }

        if ("save".equalsIgnoreCase(action) || "add".equalsIgnoreCase(action)) {
            ActionErrors errors = customerForm.validateFields();
            if (!errors.isEmpty()) {
                saveErrors(request, errors);
                request.setAttribute("customerList", dao.list());
                return mapping.findForward("list");
            }

            String id = customerForm.getId();
            if (id != null && !id.trim().isEmpty()) {
                // Edit existing
                dao.update(new Customer(id.trim(), customerForm.getName(), customerForm.getEmail(),
                        customerForm.getPhone(), customerForm.getCity()));
            } else {
                // Add new
                String newId = dao.nextId();
                dao.insert(new Customer(newId, customerForm.getName(), customerForm.getEmail(),
                        customerForm.getPhone(), customerForm.getCity()));
            }

            ActionMessages messages = new ActionMessages();
            messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("customer.success.added"));
            saveMessages(request, messages);

            // Reset form
            customerForm.reset(mapping, request);

        } else if ("delete".equalsIgnoreCase(action)) {
            String deleteId = request.getParameter("id");
            if (deleteId != null) {
                dao.delete(deleteId);
                ActionMessages messages = new ActionMessages();
                messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("customer.success.deleted"));
                saveMessages(request, messages);
            }

        } else if ("edit".equalsIgnoreCase(action)) {
            String editId = request.getParameter("id");
            if (editId != null) {
                Customer c = dao.findById(editId);
                if (c != null) {
                    customerForm.setId(c.getId());
                    customerForm.setName(c.getName());
                    customerForm.setEmail(c.getEmail());
                    customerForm.setPhone(c.getPhone());
                    customerForm.setCity(c.getCity());
                }
            }
        }

        request.setAttribute("customerList", dao.list());
        return mapping.findForward("list");
    }
}
