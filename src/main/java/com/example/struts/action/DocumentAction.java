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

import com.example.struts.db.DocumentDao;
import com.example.struts.form.DocumentForm;
import com.example.struts.model.Document;

public class DocumentAction extends Action{
    private final DocumentDao dao = new DocumentDao();

    @Override
    public ActionForward execute(ActionMapping mapping,
                                ActionForm form,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        DocumentForm documentForm = (DocumentForm) form;
        String action = documentForm.getAction();

        if(action == null || action.trim().isEmpty()){
            action = "list";
        }

        if("save".equalsIgnoreCase(action)){
            ActionErrors errors = documentForm.validateFields();
            boolean isEdit = "true".equals(documentForm.getEditMode());

            if(errors.isEmpty() && !isEdit){
                String kodeBaru = documentForm.getKodeDokumen().trim();
                if (dao.findById(kodeBaru) != null){
                    errors.add("kodeDokumen", new ActionMessage("errors.duplicate", "Kode Dokumen"));
                }
            }

            if(!errors.isEmpty()) {
                saveErrors(request, errors);
                request.setAttribute("documentList", dao.list());
                return mapping.findForward("list");
            }

            Document doc = new Document(
                documentForm.getKodeDokumen().trim(),
                documentForm.getNamaDokumen().trim(),
                documentForm.getKategoriDokumen(),
                documentForm.getTipeDosir(),
                documentForm.getAktif());
            
            ActionMessages messages = new ActionMessages();
            if(isEdit) {
                dao.update(doc);
                messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("document.success.updated"));

            }else{
                dao.insert(doc);
                messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("document.success.added"));
            }

            saveMessages(request, messages);

            documentForm.reset(mapping, request);
        } else if("delete".equalsIgnoreCase(action)){
            String kode = request.getParameter("kodeDokumen");
            if(kode != null){
                dao.delete(kode);
                ActionMessages messages = new ActionMessages();
                messages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("document.success.deleted"));
                saveMessages(request, messages);
            }
            documentForm.reset(mapping, request);
        } else if("edit".equalsIgnoreCase(action)){
            String kode = request.getParameter("kodeDokumen");
            if(kode != null){
                Document d = dao.findById(kode);
                if(d != null){
                    documentForm.setKodeDokumen(d.getKodeDokumen());
                    documentForm.setNamaDokumen(d.getNamaDokumen());
                    documentForm.setKategoriDokumen(d.getKategoriDokumen());
                    documentForm.setTipeDosir(d.getTipeDosir());
                    documentForm.setAktif(d.isAktif());
                    documentForm.setEditMode("true");
                }
            }
        }


        request.setAttribute("documentList", dao.list());
        return mapping.findForward("list");
        
    }
    
}
