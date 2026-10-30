<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title><bean:message key="welcome.title"/></title>
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
        <div class="card">
            <h1 id="welcome-heading"><bean:message key="welcome.heading"/></h1>
            <p><bean:message key="welcome.message"/></p>

            <logic:present name="user" scope="session">
                <div style="background-color: #ecfdf5; border: 1px solid #a7f3d0; padding: 1rem; border-radius: 6px; margin: 1.5rem 0;">
                    <p style="margin: 0;">
                        👤 Anda sedang login sebagai:
                        <strong id="session-username"><bean:write name="user" scope="session"/></strong>
                        <span class="badge badge-admin"><bean:write name="role" scope="session"/></span>
                    </p>
                </div>
                <div style="margin-top: 1.5rem; display: flex; gap: 1rem;">
                    <a href="<%=request.getContextPath()%>/customer.do" id="btn-go-customer" class="btn btn-primary">Buka Manajemen Pelanggan</a>
                    <a href="<%=request.getContextPath()%>/logout.do" id="btn-logout" class="btn btn-secondary">Logout</a>
                </div>
            </logic:present>

            <logic:notPresent name="user" scope="session">
                <p>Status: <span class="badge">Tamu (Guest)</span></p>
                <div style="margin-top: 1.5rem;">
                    <a href="<%=request.getContextPath()%>/showLogin.do" id="btn-go-login" class="btn btn-primary">Login Sekarang</a>
                </div>
            </logic:notPresent>
        </div>
    </div>
</body>
</html>
