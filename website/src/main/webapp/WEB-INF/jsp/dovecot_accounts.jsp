<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, java.util.Map, eu.fordo.mailadmin.model.DovecotAccount, eu.fordo.mailadmin.model.WebAccount, eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("mailAccounts.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <div class="page-header">
        <h2><%= msgs.get("mailAccounts.heading") %></h2>
        <a class="btn" href="${pageContext.request.contextPath}/admin/dovecot-accounts/new"><%= msgs.get("mailAccounts.action.addNew") %></a>
    </div>
    <p class="page-intro"><%= msgs.get("mailAccounts.intro") %></p>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="account-list">
        <%
            List<DovecotAccount> accounts = (List<DovecotAccount>) request.getAttribute("accounts");
            List<WebAccount> webAccounts = (List<WebAccount>) request.getAttribute("webAccounts");
            Map<Long, Long> pairings = (Map<Long, Long>) request.getAttribute("pairings");
            for (DovecotAccount a : accounts) {
                boolean active = "active".equals(a.getStatus());
                Long pairedWebAccountId = pairings.get(a.getId());
        %>
        <div class="account-card<%= active ? "" : " row-disabled" %>">
            <form id="ma-delete-<%= a.getId() %>" method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts"
                  onsubmit="return confirm('<%= msgs.get("mailAccounts.confirm.delete") %>')">
                <input type="hidden" name="action" value="delete">
                <input type="hidden" name="id" value="<%= a.getId() %>">
            </form>
            <form method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts" class="form-grid">
                <input type="hidden" name="action" value="update">
                <input type="hidden" name="id" value="<%= a.getId() %>">

                <label><%= msgs.get("mailAccounts.field.email") %>
                    <input type="text" value="<%= a.getUsername() %>" readonly></label>

                <label><%= msgs.get("mailAccounts.field.managedBy") %>
                    <select name="webAccountId">
                        <option value=""><%= msgs.get("mailAccounts.pairedWith.none") %></option>
                        <% for (WebAccount w : webAccounts) { %>
                            <option value="<%= w.getId() %>" <%= w.getId() == (pairedWebAccountId == null ? -1 : pairedWebAccountId) ? "selected" : "" %>><%= w.getUsername() %></option>
                        <% } %>
                    </select></label>

                <label><%= msgs.get("mailAccounts.field.folder") %>
                    <input type="text" name="mailFolder" value="<%= a.getMailFolder() == null ? "" : a.getMailFolder() %>" placeholder="<%= msgs.get("mailAccounts.folder.placeholder") %>"></label>

                <div class="account-card-footer">
                    <div class="checkbox-row">
                        <input type="checkbox" id="ma-active-<%= a.getId() %>" name="enabled" value="true" <%= active ? "checked" : "" %>>
                        <label for="ma-active-<%= a.getId() %>"><%= msgs.get("mailAccounts.field.active") %></label>
                    </div>
                    <div class="card-footer-actions">
                        <button type="submit"><%= msgs.get("mailAccounts.action.save") %></button>
                        <button type="submit" form="ma-delete-<%= a.getId() %>" class="btn-danger"><%= msgs.get("mailAccounts.action.delete") %></button>
                    </div>
                </div>
            </form>
        </div>
        <% } %>
    </div>
</main>
<%@ include file="footer.jspf" %>
