<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.DovecotAccount, eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("myAccounts.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("myAccounts.heading") %></h2>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>
    <%
        List<DovecotAccount> accounts = (List<DovecotAccount>) request.getAttribute("accounts");
        if (accounts == null || accounts.isEmpty()) {
    %>
        <div class="flash flash-info"><%= msgs.get("myAccounts.empty") %></div>
    <% }
        for (DovecotAccount a : accounts) {
            boolean active = "active".equals(a.getStatus());
    %>
        <div class="section">
            <h3><%= a.getUsername() %> (<%= a.getDomain() %>) <span class="badge <%= active ? "badge-ok" : "badge-off" %>"><%= a.getStatus() %></span></h3>

            <form method="post" action="${pageContext.request.contextPath}/my-accounts" class="form-grid">
                <input type="hidden" name="dovecotAccountId" value="<%= a.getId() %>">
                <label><%= msgs.get("myAccounts.newPassword") %>
                    <input type="password" name="newPassword" required></label>
                <button type="submit"><%= msgs.get("myAccounts.changePassword") %></button>
            </form>

            <form method="post" action="${pageContext.request.contextPath}/my-accounts" style="margin-top:.75rem;">
                <input type="hidden" name="dovecotAccountId" value="<%= a.getId() %>">
                <% if (active) { %>
                    <input type="hidden" name="action" value="disable">
                    <button type="submit" class="btn-danger btn-sm"><%= msgs.get("myAccounts.disable") %></button>
                <% } else { %>
                    <input type="hidden" name="action" value="enable">
                    <button type="submit" class="btn-sm"><%= msgs.get("myAccounts.enable") %></button>
                <% } %>
            </form>
        </div>
    <% } %>
</main>
<%@ include file="footer.jspf" %>
