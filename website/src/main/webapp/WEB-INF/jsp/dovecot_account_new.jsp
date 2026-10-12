<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.WebAccount, eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("mailAccounts.create.legend") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <a class="back-link" href="${pageContext.request.contextPath}/admin/dovecot-accounts"><%= msgs.get("mailAccounts.action.back") %></a>
    <h2><%= msgs.get("mailAccounts.create.legend") %></h2>
    <p class="page-intro"><%= msgs.get("mailAccounts.create.intro") %></p>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="section">
        <form method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts" class="form-grid">
            <input type="hidden" name="action" value="create">
            <label><%= msgs.get("mailAccounts.create.username") %>
                <input type="text" name="username" placeholder="user@example.com" value="<%= request.getAttribute("submittedUsername") != null ? request.getAttribute("submittedUsername") : "" %>" required autofocus></label>
            <label><%= msgs.get("mailAccounts.create.password") %>
                <input type="password" name="password" required></label>
            <label><%= msgs.get("mailAccounts.create.managedBy") %>
                <select name="webAccountId">
                    <option value=""><%= msgs.get("mailAccounts.pairedWith.none") %></option>
                    <%
                        List<WebAccount> webAccounts = (List<WebAccount>) request.getAttribute("webAccounts");
                        if (webAccounts != null) {
                            for (WebAccount w : webAccounts) {
                    %>
                        <option value="<%= w.getId() %>"><%= w.getUsername() %></option>
                    <% } } %>
                </select></label>
            <div class="checkbox-row">
                <input type="checkbox" id="createDisabled" name="createDisabled" value="true">
                <label for="createDisabled"><%= msgs.get("mailAccounts.create.disabled") %></label>
            </div>
            <button type="submit"><%= msgs.get("mailAccounts.create.submit") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
