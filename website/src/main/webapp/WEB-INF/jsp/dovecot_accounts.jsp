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
            <div class="account-card-id">
                <span><%= a.getUsername() %></span>
                <span class="badge <%= active ? "badge-ok" : "badge-off" %>"><%= a.getStatus() %></span>
            </div>
            <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts">
                <input type="hidden" name="action" value="pair">
                <input type="hidden" name="dovecotAccountId" value="<%= a.getId() %>">
                <select name="webAccountId" onchange="this.form.submit()">
                    <option value=""><%= msgs.get("mailAccounts.pairedWith.none") %></option>
                    <% for (WebAccount w : webAccounts) { %>
                        <option value="<%= w.getId() %>" <%= w.getId() == (pairedWebAccountId == null ? -1 : pairedWebAccountId) ? "selected" : "" %>><%= w.getUsername() %></option>
                    <% } %>
                </select>
            </form>
            <div class="account-card-actions">
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts">
                    <input type="hidden" name="id" value="<%= a.getId() %>">
                    <% if (active) { %>
                        <input type="hidden" name="action" value="disable">
                        <button type="submit" class="btn-sm btn-danger"><%= msgs.get("mailAccounts.action.disable") %></button>
                    <% } else { %>
                        <input type="hidden" name="action" value="enable">
                        <button type="submit" class="btn-sm"><%= msgs.get("mailAccounts.action.enable") %></button>
                    <% } %>
                </form>
                <form class="inline-form" method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts"
                      onsubmit="return confirm('<%= msgs.get("mailAccounts.confirm.delete") %>')">
                    <input type="hidden" name="action" value="delete">
                    <input type="hidden" name="id" value="<%= a.getId() %>">
                    <button type="submit" class="btn-sm btn-danger"><%= msgs.get("mailAccounts.action.delete") %></button>
                </form>
            </div>
        </div>
        <% } %>
    </div>
</main>
<%@ include file="footer.jspf" %>
