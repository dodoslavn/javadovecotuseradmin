<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.WebAccount, eu.fordo.mailadmin.service.Messages, eu.fordo.mailadmin.service.DateFormatter" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("webAccounts.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("webAccounts.heading") %></h2>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>

    <div class="section">
        <div class="table-wrap">
        <table>
            <tr>
                <th><%= msgs.get("webAccounts.col.username") %></th>
                <th><%= msgs.get("webAccounts.col.role") %></th>
                <th><%= msgs.get("webAccounts.col.enabled") %></th>
                <th><%= msgs.get("webAccounts.col.created") %></th>
                <th><%= msgs.get("webAccounts.col.actions") %></th>
            </tr>
            <%
                List<WebAccount> accounts = (List<WebAccount>) request.getAttribute("accounts");
                for (WebAccount a : accounts) {
            %>
            <tr<%= a.isEnabled() ? "" : " class=\"row-disabled\"" %>>
                <td><%= a.getUsername() %></td>
                <td><span class="badge"><%= a.getRole() %></span></td>
                <td><span class="badge <%= a.isEnabled() ? "badge-ok" : "badge-off" %>"><%= a.isEnabled() %></span></td>
                <td><%= DateFormatter.format(a.getCreatedAt()) %></td>
                <td>
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
                </td>
            </tr>
            <% } %>
        </table>
        </div>
    </div>

    <div class="section">
        <h3><%= msgs.get("webAccounts.create.legend") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/web-accounts" class="form-grid">
            <input type="hidden" name="action" value="create">
            <label><%= msgs.get("webAccounts.create.username") %>
                <input type="text" name="username" required></label>
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
