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
            <a href="<%=request.getContextPath()%>/customer.do">Manajemen Pelanggan</a>
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         <logic:present name="user" scope="session">
                <a href="<%=request.getContextPath()%>/logout.do" id="nav-logout"><bean:message key="login.logout"/></a>
            </logic:present>
            <logic:notPresent name="user" scope="session">
                <a href="<%=request.getContextPath()%>/showLogin.do" id="nav-login">Login</a>
            </logic:notPresent>
        </div>
    </div>

    <div class="container">
        <logic:messagesPresent message="true">
            <div id="success-notification" class="success-message">
                <html:messages id="msg" message="true">
                    <div><bean:write name="msg"/></div>
                </html:messages>
            </div>
        </logic:messagesPresent>

        <logic:messagesPresent>
            <div id="error-messages">
                <html:errors/>
            </div>
        </logic:messagesPresent>
        <div class="card">
            <h3><bean:message key="document.form.heading"/></h3>
            <html:form action="/document.do" method="post">
                <html:hidden property="action" value="save" />
                <html:hidden property="editMode" />

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
                    <a href="<%=request.getContextPath()%>/document.do" id="btn-cancel" class="btn btn-secondary">
                        <bean:message key="document.reset"/>
                    </a>
                </div>
            </html:form>
        </div>
        <div class="card">
            <h3><bean:message key="document.heading"/></h3>
            <table id="document-table">
                <thead>
                    <tr>
                        <th><bean:message key="document.kode"/></th>
                        <th><bean:message key="document.nama"/></th>
                        <th><bean:message key="document.kategori"/></th>
                        <th><bean:message key="document.tipeDosir"/></th>
                        <th>Status</th>
                        <th><bean:message key="document.actions"/></th>
                    </tr>
                </thead>
                <tbody>
                    <logic:notEmpty name="documentList">
                        <logic:iterate name="documentList" id="doc">
                            <tr id="document-row-<bean:write name="doc" property="kodeDokumen"/>">
                                <td><span class="badge"><bean:write name="doc" property="kodeDokumen"/></span></td>
                                <td><bean:write name="doc" property="namaDokumen"/></td>
                                <td><bean:write name="doc" property="kategoriDokumen"/></td>
                                <td><bean:write name="doc" property="tipeDosir"/></td>
                                <td>
                                    <logic:equal name="doc" property="aktif" value="true">
                                        <bean:message key="document.status.aktif"/>
                                    </logic:equal>
                                    <logic:notEqual name="doc" property="aktif" value="true">
                                        <bean:message key="document.status.nonaktif"/>
                                    </logic:notEqual>
                                </td>
                                <td>
                                    <a href="<%=request.getContextPath()%>/document.do?action=edit&kodeDokumen=<bean:write name="doc" property="kodeDokumen"/>"
                                       class="btn btn-secondary" style="padding: 0.3rem 0.6rem; font-size: 0.8rem;">Edit</a>

                                    <a href="<%=request.getContextPath()%>/document.do?action=delete&kodeDokumen=<bean:write name="doc" property="kodeDokumen"/>"
                                       class="btn btn-danger"
                                       onclick="return confirm('Yakin hapus dokumen ini?');">Hapus</a>
                                </td>
                            </tr>
                        </logic:iterate>
                    </logic:notEmpty>
                    <logic:empty name="documentList">
                        <tr>
                            <td colspan="6" style="text-align: center; color: #64748b;" id="empty-message">
                                <bean:message key="document.empty"/>
                            </td>
                        </tr>
                    </logic:empty>
                </tbody>
            </table>

            <div id="pagination" style="margin-top: 1rem; display: flex; gap: 0.5rem; align-items: center;">
                <logic:equal name="hasPrev" value="true">
                    <a id="btn-prev" class="btn btn-secondary"
                    href="<%=request.getContextPath()%>/document.do?page=<bean:write name="prevPage"/>"><-</a>
                </logic:equal>
                <logic:notEqual name="hasPrev" value="true">
                    <span id="btn-prev" class="btn btn-secondary" style="opacity:0.5; pointer-events:none;"><-</span>
                </logic:notEqual>

                <span id="page-info">
                    Halaman <bean:write name="halaman"/> dari <bean:write name="totalHalaman"/>
                    (<bean:write name="totalData"/> data)
                </span>

                <logic:equal name="hasNext" value="true">
                    <a id="btn-next" class="btn btn-secondary"
                    href="<%=request.getContextPath()%>/document.do?page=<bean:write name="nextPage"/>">-></a>
                </logic:equal>
                <logic:notEqual name="hasNext" value="true">
                    <span id="btn-next" class="btn btn-secondary" style="opacity:0.5; pointer-events:none;">-></span>
                </logic:notEqual>
            </div>
            
        </div>
    </div>
</body>
</html>
