<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title><bean:message key="document.title"/></title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
    <div class="navbar">
        <strong>Struts 1 + Java 21 Demo</strong>
        <div>
            <a href="<%=request.getContextPath()%>/welcome.do">Home</a>
            <a href="<%=request.getContextPath()%>/document.do" id="nav-documents">Tipe Dokumen</a>
            <logic:present name="user" scope="session">
                <a href="<%=request.getContextPath()%>/logout.do" id="nav-logout"><bean:message key="login.logout"/></a>
            </logic:present>
            <logic:notPresent name="user" scope="session">
                <a href="<%=request.getContextPath()%>/showLogin.do" id="nav-login">Login</a>
            </logic:notPresent>
        </div>
    </div>

        
    <div class="container">
        <logic:messagesPresent>
            <div id="error-messages">
                <html:errors/>
            </div>
        </logic:messagesPresent>

        <div class="card">
            <h3>
                <logic:equal name="documentForm" property="editMode" value="true">Edit Dokumen</logic:equal>
                <logic:notEqual name="documentForm" property="editMode" value="true">Tambah Dokumen</logic:notEqual>
            </h3>
            <html:form action="/document.do" method="post">
                <html:hidden property="action" value="save"/>
                <html:hidden property="editMode" />
                <input type="hidden" name="page" value="<bean:write name="backPage"/>">
                <input type="hidden" name="q" value="<bean:write name="q"/>">

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label for="doc-kode"><bean:message key="document.kode"/> *</label>
                        <logic:equal name="documentForm" property="editMode" value="true">
                            <html:text property="kodeDokumen" styleId="doc-kode" styleClass="form-control" readonly="true" />
                        </logic:equal>
                        <logic:notEqual name="documentForm" property="editMode" value="true">
                            <html:text property="kodeDokumen" styleId="doc-kode" styleClass="form-control" maxlength="32" />
                        </logic:notEqual>
                    </div>

                    <div class="form-group">
                        <label for="doc-nama"><bean:message key="document.nama"/> *</label>
                        <html:text property="namaDokumen" styleId="doc-nama" styleClass="form-control" maxlength="255" />
                    </div>

                    <div class="form-group">
                        <label for="doc-kategori"><bean:message key="document.kategori"/> *</label>
                        <html:select property="kategoriDokumen" styleId="doc-kategori" styleClass="form-control">
                            <html:option value=""><bean:message key="document.pilih"/></html:option>
                            <html:option value="Dokumen Pengikatan Kredit">Dokumen Pengikatan Kredit</html:option>
                            <html:option value="Dokumen Pendukung Jaminan">Dokumen Pendukung Jaminan</html:option>
                            <html:option value="Dokumen Bukti Kepemilikan">Dokumen Bukti Kepemilikan</html:option>
                        </html:select>
                    </div>

                    <div class="form-group">
                        <label for="doc-tipe"><bean:message key="document.tipeDosir"/> *</label>
                        <html:select property="tipeDosir" styleId="doc-tipe" styleClass="form-control">
                            <html:option value=""><bean:message key="document.pilih"/></html:option>
                            <html:option value="Dosir Asli Perkreditan">Dosir Asli Perkreditan</html:option>
                            <html:option value="Dosir Asli Jaminan">Dosir Asli Jaminan</html:option>
                            <html:option value="Dosir Harian">Dosir Harian</html:option>
                        </html:select>
                    </div>
                </div>

                <div class="form-group" style="margin-top: 1rem;">
                    <label for="doc-aktif">
                        <html:checkbox property="aktif" styleId="doc-aktif" />
                        <bean:message key="document.aktif"/>
                    </label>
                </div>

                <div style="margin-top: 1rem; display: flex; gap: 0.5rem;">
                    <button type="submit" id="btn-save-document" class="btn btn-primary">
                        <bean:message key="document.submit"/>
                    </button>
                    <a href="<%=request.getContextPath()%>/document.do?page=<bean:write name="backPage"/>&q=<bean:write name="qEnc"/>"
                        id="btn-cancel" class="btn btn-secondary">
                        Batal
                    </a>
                </div>
            </html:form>
        </div>
        
    </div>
</body>
</html>
