<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <title><bean:message key="login.title"/></title>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/css/style.css">
</head>
<body>
    <div class="navbar">
        <strong>Struts 1 + Java 21 Demo</strong>
        <div>
            <a href="<%=request.getContextPath()%>/welcome.do">Home</a>
        </div>
    </div>

    <div class="container" style="max-width: 450px;">
        <div class="card">
            <h2><bean:message key="login.heading"/></h2>

            <!-- Pesan Error Validasi Struts -->
            <logic:messagesPresent>
                <div id="error-messages">
                    <html:errors/>
                </div>
            </logic:messagesPresent>

            <html:form action="/login.do" method="post">
                <div class="form-group">
                    <label for="username"><bean:message key="login.username"/></label>
                    <html:text property="username" styleId="username" styleClass="form-control" />
                </div>

                <div class="form-group">
                    <label for="password"><bean:message key="login.password"/></label>
                    <html:password property="password" styleId="password" styleClass="form-control" />
                </div>

                <div class="form-group" style="margin-top: 1.5rem;">
                    <button type="submit" id="btn-login" class="btn btn-primary" style="width: 100%;">
                        <bean:message key="login.submit"/>
                    </button>
                </div>
            </html:form>

            <div style="margin-top: 1.5rem; font-size: 0.85rem; color: #64748b; background: #f1f5f9; padding: 0.75rem; border-radius: 6px;">
                💡 <strong>Petunjuk Kredensial:</strong><br>
                Username: <code>admin</code><br>
                Password: <code>admin123</code>
            </div>
        </div>
    </div>
</body>
</html>
