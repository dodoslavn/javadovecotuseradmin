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
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("mailAccounts.heading") %></h2>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="section">
        <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th><%= msgs.get("mailAccounts.col.username") %></th>
                <th><%= msgs.get("mailAccounts.col.status") %></th>
                <th><%= msgs.get("mailAccounts.col.pairedWith") %></th>
                <th><%= msgs.get("mailAccounts.col.actions") %></th>
            </tr>
            </thead>
            <tbody>
            <%
                List<DovecotAccount> accounts = (List<DovecotAccount>) request.getAttribute("accounts");
                List<WebAccount> webAccounts = (List<WebAccount>) request.getAttribute("webAccounts");
                Map<Long, Long> pairings = (Map<Long, Long>) request.getAttribute("pairings");
                for (DovecotAccount a : accounts) {
                    boolean active = "active".equals(a.getStatus());
                    Long pairedWebAccountId = pairings.get(a.getId());
            %>
            <tr<%= active ? "" : " class=\"row-disabled\"" %>>
                <td data-label="<%= msgs.get("mailAccounts.col.username") %>"><%= a.getUsername() %></td>
                <td data-label="<%= msgs.get("mailAccounts.col.status") %>"><span class="badge <%= active ? "badge-ok" : "badge-off" %>"><%= a.getStatus() %></span></td>
                <td data-label="<%= msgs.get("mailAccounts.col.pairedWith") %>">
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
                </td>
                <td data-label="<%= msgs.get("mailAccounts.col.actions") %>">
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
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    </div>

    <div class="section">
        <h3><%= msgs.get("mailAccounts.create.legend") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts" class="form-grid">
            <input type="hidden" name="action" value="create">
            <label><%= msgs.get("mailAccounts.create.username") %>
                <input type="text" name="username" placeholder="user@example.com" required></label>
            <label><%= msgs.get("mailAccounts.create.password") %>
                <input type="password" name="password" required></label>
            <button type="submit"><%= msgs.get("mailAccounts.create.submit") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
