<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.WebAccount, eu.fordo.mailadmin.model.Role, eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("webAccounts.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <div class="page-header">
        <h2><%= msgs.get("webAccounts.heading") %></h2>
        <div class="page-header-actions">
            <a class="btn btn-secondary" href="${pageContext.request.contextPath}/admin/web-accounts/invite"><%= msgs.get("invite.action.addNew") %></a>
            <a class="btn" href="${pageContext.request.contextPath}/admin/web-accounts/new"><%= msgs.get("webAccounts.action.addNew") %></a>
        </div>
    </div>
    <p class="page-intro"><%= msgs.get("webAccounts.intro") %></p>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="account-list">
        <%
            List<WebAccount> accounts = (List<WebAccount>) request.getAttribute("accounts");
            for (WebAccount a : accounts) {
        %>
        <div class="account-card<%= a.isEnabled() ? "" : " row-disabled" %>">
            <form method="post" action="${pageContext.request.contextPath}/admin/web-accounts" class="form-grid">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="id" value="<%= a.getId() %>">

                <label><%= msgs.get("webAccounts.field.name") %>
                    <input type="text" name="newUsername" value="<%= a.getUsername() %>"></label>

                <label><%= msgs.get("webAccounts.field.password") %>
                    <input type="password" name="newPassword" placeholder="<%= msgs.get("webAccounts.newPassword.placeholder") %>" minlength="8"></label>

                <div class="checkbox-row">
                    <input type="checkbox" id="wa-admin-<%= a.getId() %>" name="role" value="ADMIN" <%= a.getRole() == Role.ADMIN ? "checked" : "" %>>
                    <label for="wa-admin-<%= a.getId() %>"><%= msgs.get("webAccounts.field.role") %></label>
                </div>

                <div class="account-card-footer">
                    <div class="checkbox-row">
                        <input type="checkbox" id="wa-active-<%= a.getId() %>" name="enabled" value="true" <%= a.isEnabled() ? "checked" : "" %>>
                        <label for="wa-active-<%= a.getId() %>"><%= msgs.get("webAccounts.field.active") %></label>
                    </div>
                    <button type="submit"><%= msgs.get("webAccounts.action.save") %></button>
                </div>
            </form>
        </div>
        <% } %>
    </div>
</main>
<%@ include file="footer.jspf" %>
