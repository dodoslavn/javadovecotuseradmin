<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.WebAccount, eu.fordo.mailadmin.model.Role, eu.fordo.mailadmin.service.Messages, eu.fordo.mailadmin.service.DateFormatter" %>
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
        <a class="btn" href="${pageContext.request.contextPath}/admin/web-accounts/new"><%= msgs.get("webAccounts.action.addNew") %></a>
    </div>
    <p class="page-intro"><%= msgs.get("webAccounts.intro") %></p>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="account-list">
        <%
            List<WebAccount> accounts = (List<WebAccount>) request.getAttribute("accounts");
            for (WebAccount a : accounts) {
        %>
        <div class="account-card<%= a.isEnabled() ? "" : " row-disabled" %>">
            <div class="account-card-main">
                <div class="account-card-id">
                    <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/web-accounts">
                        <input type="hidden" name="action" value="rename">
                        <input type="hidden" name="id" value="<%= a.getId() %>">
                        <input type="text" name="newUsername" value="<%= a.getUsername() %>">
                        <button type="submit" class="btn-sm"><%= msgs.get("webAccounts.action.rename") %></button>
                    </form>
                </div>
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/web-accounts">
                    <input type="hidden" name="action" value="changeRole">
                    <input type="hidden" name="id" value="<%= a.getId() %>">
                    <select name="role" onchange="this.form.submit()">
                        <option value="USER" <%= a.getRole() == Role.USER ? "selected" : "" %>>USER</option>
                        <option value="ADMIN" <%= a.getRole() == Role.ADMIN ? "selected" : "" %>>ADMIN</option>
                    </select>
                </form>
                <span class="badge <%= a.isEnabled() ? "badge-ok" : "badge-off" %>"><%= a.isEnabled() %></span>
                <span class="account-card-meta"><%= DateFormatter.format(a.getCreatedAt()) %></span>
            </div>
            <div class="account-card-actions">
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/web-accounts">
                    <input type="hidden" name="id" value="<%= a.getId() %>">
                    <% if (a.isEnabled()) { %>
                        <input type="hidden" name="action" value="disable">
                        <button type="submit" class="btn-sm btn-danger"><%= msgs.get("webAccounts.action.disable") %></button>
                    <% } else { %>
                        <input type="hidden" name="action" value="enable">
                        <button type="submit" class="btn-sm"><%= msgs.get("webAccounts.action.enable") %></button>
                    <% } %>
                </form>
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/web-accounts">
                    <input type="hidden" name="action" value="resetPassword">
                    <input type="hidden" name="id" value="<%= a.getId() %>">
                    <input type="password" name="newPassword" placeholder="<%= msgs.get("webAccounts.newPassword.placeholder") %>" minlength="8">
                    <button type="submit" class="btn-sm"><%= msgs.get("webAccounts.action.resetPassword") %></button>
                </form>
            </div>
        </div>
        <% } %>
    </div>
</main>
<%@ include file="footer.jspf" %>
