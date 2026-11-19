package com.example.struts.action;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionRedirect;

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

        HttpSession session = request.getSession(false);
        if(session == null || session.getAttribute("user") == null){
            return mapping.findForward("login");
        }
        DocumentForm documentForm = (DocumentForm) form;
        String action = documentForm.getAction();

        if(action == null || action.trim().isEmpty()){
            action = "list";
        }

        String q = bacaQ(request);
        int halamanAsal = bacaHalaman(request);
        request.setAttribute("q", q);
        request.setAttribute("qEnc", URLEncoder.encode(q, StandardCharsets.UTF_8));
        request.setAttribute("backPage", halamanAsal);

        if("add".equalsIgnoreCase(action)){
            documentForm.reset(mapping, request);
            return mapping.findForward("form");
        }

        if("edit".equalsIgnoreCase(action)){
            String kode = request.getParameter("kodeDokumen");
            Document d = (kode == null) ? null : dao.findById(kode);
                if(d == null)
                    return redirectKeList(mapping, halamanAsal, q);

                    documentForm.setKodeDokumen(d.getKodeDokumen());
                    documentForm.setNamaDokumen(d.getNamaDokumen());
                    documentForm.setKategoriDokumen(d.getKategoriDokumen());
                    documentForm.setTipeDosir(d.getTipeDosir());
                    documentForm.setAktif(d.isAktif());
                    documentForm.setEditMode("true");
                    return mapping.findForward("form");
        }

        if ("save".equalsIgnoreCase(action)){
            ActionErrors errors = documentForm.validateFields();
            boolean isEdit = "true".equals(documentForm.getEditMode());

            if(errors.isEmpty() && !isEdit){
                String kodeBaru = documentForm.getKodeDokumen().trim();
                if(dao.findById(kodeBaru) != null){
                    errors.add("kodeDokumen", new ActionMessage("errors.duplicate", "Kode Dokumen"));
                }
            }

            if(!errors.isEmpty()){
                saveErrors(request, errors);
                return mapping.findForward("form");
            }

            Document doc = new Document(
                documentForm.getKodeDokumen().trim(),
                documentForm.getNamaDokumen().trim(),
                documentForm.getKategoriDokumen(),
                documentForm.getTipeDosir(),
                documentForm.getAktif());

            if(isEdit){
                dao.update(doc);
                session.setAttribute("flash", "updated");
            }else{
                dao.insert(doc);
                session.setAttribute("flash", "added");
            }

            return redirectKeList(mapping, halamanAsal, q);
        }

        if("delete".equalsIgnoreCase(action)){
            String kode = request.getParameter("kodeDokumen");
            if(kode!=null){
                dao.delete(kode);
                session.setAttribute("flash", "deleted");
            }

            return redirectKeList(mapping,halamanAsal,q);
        }


        Object flash = session.getAttribute("flash");
        if(flash!=null){
            request.setAttribute("flash", flash);
            session.removeAttribute("flash");
        }
        kirimData(request,q,halamanAsal);
        return mapping.findForward("list");
    }

        private ActionForward redirectKeList(ActionMapping mapping, int page, String q){
            ActionRedirect redirect = new ActionRedirect(mapping.findForward("redirectList"));
            redirect.addParameter("page", page);
            if(!q.isEmpty()){
                redirect.addParameter("q", q);
            }
            return redirect;
        }

    private static final int UKURAN_HALAMAN = 5;

    private String bacaQ(HttpServletRequest request){
        String q = request.getParameter("q");
        if(q == null){
            return "";
        }

        q = q.trim();
        if(q.length() > 100){
            q = q.substring(0,100);
        }

        return q;

    }

    private int bacaHalaman(HttpServletRequest request){
        int halaman = 1;
        String param = request.getParameter("page");

        if(param != null){
            try{
                halaman = Integer.parseInt(param.trim());
            }catch(NumberFormatException e){
                halaman = 1;
            }
        }

        return halaman < 1 ? 1 : halaman;
    }

    private void kirimData(HttpServletRequest request, String q, int halaman){

        int totalData = dao.countAll(q);

        int totalHalaman = (totalData + UKURAN_HALAMAN - 1) / UKURAN_HALAMAN;
        if(totalHalaman < 1) totalHalaman = 1;
        if(halaman > totalHalaman) halaman = totalHalaman;

        int offset = (halaman - 1) * UKURAN_HALAMAN;

        List<Document> sebagianData = dao.list(q, UKURAN_HALAMAN, offset);

        request.setAttribute("documentList", sebagianData);
        request.setAttribute("q", q);
        request.setAttribute("qEnc", URLEncoder.encode(q, StandardCharsets.UTF_8));
        request.setAttribute("halaman", halaman);
        request.setAttribute("totalHalaman", totalHalaman);
        request.setAttribute("totalData", totalData);
        request.setAttribute("hasPrev", halaman > 1);
        request.setAttribute("hasNext", halaman < totalHalaman);
        request.setAttribute("prevPage", halaman - 1);
        request.setAttribute("nextPage", halaman + 1);
    }

    
}
