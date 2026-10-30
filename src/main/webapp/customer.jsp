<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title><bean:message key="customer.title"/></title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
    <div class="navbar">
        <strong>Struts 1 + Java 21 Demo</strong>
        <div>
            <a href="<%=request.getContextPath()%>/welcome.do">Home</a>
            <a href="<%=request.getContextPath()%>/customer.do" id="nav-customers">Manajemen Pelanggan</a>
            <logic:present name="user" scope="session">
                <a href="<%=request.getContextPath()%>/logout.do" id="nav-logout"><bean:message key="login.logout"/></a>
            </logic:present>
            <logic:notPresent name="user" scope="session">
                <a href="<%=request.getContextPath()%>/showLogin.do" id="nav-login">Login</a>
            </logic:notPresent>
        </div>
    </div>

    <div class="container">
        <!-- Notifikasi Sukses -->
        <logic:messagesPresent message="true">
            <div id="success-notification" class="success-message">
                <html:messages id="msg" message="true">
                    <div><bean:write name="msg"/></div>
                </html:messages>
            </div>
        </logic:messagesPresent>

        <!-- Pesan Error Validasi -->
        <logic:messagesPresent>
            <div id="error-messages">
                <html:errors/>
            </div>
        </logic:messagesPresent>

        <!-- Form Tambah / Edit Pelanggan -->
        <div class="card">
            <h3><bean:message key="customer.form.heading"/></h3>
            <html:form action="/customer.do" method="post">
                <html:hidden property="action" value="save" />
                <html:hidden property="id" styleId="cust-id" />

                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;">
                    <div class="form-group">
                        <label for="cust-name"><bean:message key="customer.name"/> *</label>
                        <html:text property="name" styleId="cust-name" styleClass="form-control" />
                    </div>

                    <div class="form-group">
                        <label for="cust-email"><bean:message key="customer.email"/> *</label>
                        <html:text property="email" styleId="cust-email" styleClass="form-control" />
                    </div>

                    <div class="form-group">
                        <label for="cust-phone"><bean:message key="customer.phone"/> *</label>
                        <html:text property="phone" styleId="cust-phone" styleClass="form-control" />
                    </div>

                    <div class="form-group">
                        <label for="cust-city"><bean:message key="customer.city"/></label>
                        <html:text property="city" styleId="cust-city" styleClass="form-control" />
                    </div>
                </div>

                <div style="margin-top: 1rem; display: flex; gap: 0.5rem;">
                    <button type="submit" id="btn-save-customer" class="btn btn-primary">
                        <bean:message key="customer.submit"/>
                    </button>
                    <a href="<%=request.getContextPath()%>/customer.do" id="btn-cancel" class="btn btn-secondary">
                        <bean:message key="customer.reset"/>
                    </a>
                </div>
            </html:form>
        </div>

        <!-- Tabel Daftar Pelanggan -->
        <div class="card">
            <h3><bean:message key="customer.heading"/></h3>
            <table id="customer-table">
                <thead>
                    <tr>
                        <th><bean:message key="customer.id"/></th>
                        <th><bean:message key="customer.name"/></th>
                        <th><bean:message key="customer.email"/></th>
                        <th><bean:message key="customer.phone"/></th>
                        <th><bean:message key="customer.city"/></th>
                        <th><bean:message key="customer.actions"/></th>
                    </tr>
                </thead>
                <tbody>
                    <logic:notEmpty name="customerList">
                        <logic:iterate name="customerList" id="cust">
                            <tr id="customer-row-<bean:write name="cust" property="id"/>">
                                <td><span class="badge"><bean:write name="cust" property="id"/></span></td>
                                <td class="cust-name"><bean:write name="cust" property="name"/></td>
                                <td class="cust-email"><bean:write name="cust" property="email"/></td>
                                <td class="cust-phone"><bean:write name="cust" property="phone"/></td>
                                <td class="cust-city"><bean:write name="cust" property="city"/></td>
                                <td>
                                    <a href="<%=request.getContextPath()%>/customer.do?action=edit&amp;id=<bean:write name="cust" property="id"/>"
                                       class="btn btn-secondary" style="padding: 0.3rem 0.6rem; font-size: 0.8rem;"
                                       id="btn-edit-<bean:write name="cust" property="id"/>">Edit</a>

                                    <a href="<%=request.getContextPath()%>/customer.do?action=delete&amp;id=<bean:write name="cust" property="id"/>"
                                       class="btn btn-danger"
                                       id="btn-delete-<bean:write name="cust" property="id"/>">Hapus</a>
                                </td>
                            </tr>
                        </logic:iterate>
                    </logic:notEmpty>
                    <logic:empty name="customerList">
                        <tr>
                            <td colspan="6" style="text-align: center; color: #64748b;" id="empty-message">
                                <bean:message key="customer.empty"/>
                            </td>
                        </tr>
                    </logic:empty>
                </tbody>
            </table>
        </div>
    </div>
</body>
</html>
