package com.example.struts.action;

import com.example.struts.form.CustomerForm;
import com.example.struts.model.Customer;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class CustomerAction extends Action {

    private static final List<Customer> CUSTOMERS = new CopyOnWriteArrayList<>();

    static {
        // Data inisial untuk demo
        CUSTOMERS.add(new Customer("CUST-001", "Budi Santoso", "budi@example.com", "08123456789", "Jakarta"));
        CUSTOMERS.add(new Customer("CUST-002", "Siti Aminah", "siti@example.com", "08234567890", "Surabaya"));
    }

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
                request.setAttribute("customerList", CUSTOMERS);
                return mapping.findForward("list");
            }

            String id = customerForm.getId();
            if (id != null && !id.trim().isEmpty()) {
                // Edit existing
                for (Customer c : CUSTOMERS) {
                    if (c.getId().equals(id.trim())) {
                        c.setName(customerForm.getName());
                        c.setEmail(customerForm.getEmail());
                        c.setPhone(customerForm.getPhone());
                        c.setCity(customerForm.getCity());
                        break;
                    }
                }
            } else {
                // Add new
                String newId = "CUST-" + String.format("%03d", CUSTOMERS.size() + 1);
                Customer newCust = new Customer(newId, customerForm.getName(), customerForm.getEmail(),
                        customerForm.getPhone(), customerForm.getCity());
                CUSTOMERS.add(newCust);
            }

            ActionMessages messages = new ActionMessages();
            messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("customer.success.added"));
            saveMessages(request, messages);

            // Reset form
            customerForm.reset(mapping, request);

        } else if ("delete".equalsIgnoreCase(action)) {
            String deleteId = request.getParameter("id");
            if (deleteId != null) {
                CUSTOMERS.removeIf(c -> c.getId().equals(deleteId));
                ActionMessages messages = new ActionMessages();
                messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("customer.success.deleted"));
                saveMessages(request, messages);
            }

        } else if ("edit".equalsIgnoreCase(action)) {
            String editId = request.getParameter("id");
            if (editId != null) {
                for (Customer c : CUSTOMERS) {
                    if (c.getId().equals(editId)) {
                        customerForm.setId(c.getId());
                        customerForm.setName(c.getName());
                        customerForm.setEmail(c.getEmail());
                        customerForm.setPhone(c.getPhone());
                        customerForm.setCity(c.getCity());
                        break;
                    }
                }
            }
        }

        request.setAttribute("customerList", CUSTOMERS);
        return mapping.findForward("list");
    }
}
