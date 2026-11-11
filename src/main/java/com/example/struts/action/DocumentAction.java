package com.example.struts.action;

import java.util.List;

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
                kirimData(request);
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


        kirimData(request);
        return mapping.findForward("list");
        
    }

    private static final int UKURAN_HALAMAN = 5;

    private void kirimData(HttpServletRequest request) {
        int halaman = 1;
        String param = request.getParameter("page");
        if(param != null){
            try{
                halaman = Integer.parseInt(param.trim());
            } catch (NumberFormatException e){
                halaman = 1;
            }
        }
        if(halaman < 1) halaman = 1;

        int totalData = dao.countAll();
        
        int totalHalaman = (totalData + UKURAN_HALAMAN - 1) / UKURAN_HALAMAN;
        if(totalHalaman < 1) totalHalaman = 1;
        if(halaman > totalHalaman) halaman = totalHalaman;

        int offset = (halaman - 1) * UKURAN_HALAMAN;

        List<Document> sebagianData = dao.list(UKURAN_HALAMAN, offset);

        request.setAttribute("documentList", sebagianData);
        request.setAttribute("halaman", halaman);
        request.setAttribute("totalHalaman", totalHalaman);
        request.setAttribute("totalData", totalData);
        request.setAttribute("hasPrev", halaman > 1);
        request.setAttribute("hasNext", halaman < totalHalaman);
        request.setAttribute("prevPage", halaman - 1);
        request.setAttribute("nextPage", halaman + 1);
    }
    
}
