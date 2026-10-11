<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("webAccounts.create.legend") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <a class="back-link" href="${pageContext.request.contextPath}/admin/web-accounts"><%= msgs.get("webAccounts.action.back") %></a>
    <h2><%= msgs.get("webAccounts.create.legend") %></h2>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="section">
        <form method="post" action="${pageContext.request.contextPath}/admin/web-accounts" class="form-grid">
            <input type="hidden" name="action" value="create">
            <label><%= msgs.get("webAccounts.create.username") %>
                <input type="text" name="username" value="<%= request.getAttribute("submittedUsername") != null ? request.getAttribute("submittedUsername") : "" %>" required autofocus></label>
            <label><%= msgs.get("webAccounts.create.password") %>
                <input type="password" name="password" required></label>
            <label><%= msgs.get("webAccounts.create.role") %>
                <select name="role">
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                </select>
            </label>
            <button type="submit"><%= msgs.get("webAccounts.create.submit") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
