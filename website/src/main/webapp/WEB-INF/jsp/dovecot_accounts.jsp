<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.DovecotAccount, eu.fordo.mailadmin.service.Messages" %>
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
            <tr>
                <th><%= msgs.get("mailAccounts.col.domain") %></th>
                <th><%= msgs.get("mailAccounts.col.username") %></th>
                <th><%= msgs.get("mailAccounts.col.uid") %></th>
                <th><%= msgs.get("mailAccounts.col.gid") %></th>
                <th><%= msgs.get("mailAccounts.col.home") %></th>
                <th><%= msgs.get("mailAccounts.col.status") %></th>
                <th><%= msgs.get("mailAccounts.col.actions") %></th>
            </tr>
            <%
                List<DovecotAccount> accounts = (List<DovecotAccount>) request.getAttribute("accounts");
                for (DovecotAccount a : accounts) {
                    boolean active = "active".equals(a.getStatus());
            %>
            <tr<%= active ? "" : " class=\"row-disabled\"" %>>
                <td><%= a.getDomain() %></td>
                <td><%= a.getUsername() %></td>
                <td><%= a.getUid() %></td>
                <td><%= a.getGid() %></td>
                <td><%= a.getHomeDir() %></td>
                <td><span class="badge <%= active ? "badge-ok" : "badge-off" %>"><%= a.getStatus() %></span></td>
                <td>
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
                </td>
            </tr>
            <% } %>
        </table>
        </div>
    </div>

    <div class="section">
        <h3><%= msgs.get("mailAccounts.create.legend") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/dovecot-accounts" class="form-grid">
            <input type="hidden" name="action" value="create">
            <label><%= msgs.get("mailAccounts.create.domain") %>
                <input type="text" name="domain" placeholder="mail-host-1" required></label>
            <label><%= msgs.get("mailAccounts.create.username") %>
                <input type="text" name="username" placeholder="user@example.com" required></label>
            <label><%= msgs.get("mailAccounts.create.password") %>
                <input type="password" name="password" required></label>
            <label><%= msgs.get("mailAccounts.create.uid") %>
                <input type="number" name="uid" value="5000" required></label>
            <label><%= msgs.get("mailAccounts.create.gid") %>
                <input type="number" name="gid" value="5000" required></label>
            <label><%= msgs.get("mailAccounts.create.homeDir") %>
                <input type="text" name="homeDir" placeholder="/var/mail/vhosts/example.com/user" required></label>
            <button type="submit"><%= msgs.get("mailAccounts.create.submit") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
