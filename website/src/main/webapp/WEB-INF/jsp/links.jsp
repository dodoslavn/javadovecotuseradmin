<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.WebAccount, eu.fordo.mailadmin.model.DovecotAccount, eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("links.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("links.heading") %></h2>
    <div class="flash flash-info"><%= msgs.get("links.intro") %></div>

    <%
        List<WebAccount> webAccounts = (List<WebAccount>) request.getAttribute("webAccounts");
        List<DovecotAccount> dovecotAccounts = (List<DovecotAccount>) request.getAttribute("dovecotAccounts");
    %>

    <div class="section">
        <h3><%= msgs.get("links.create.legend") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/links" class="form-grid">
            <input type="hidden" name="action" value="link">
            <label><%= msgs.get("links.webAccount") %>
                <select name="webAccountId">
                    <% for (WebAccount w : webAccounts) { %>
                        <option value="<%= w.getId() %>"><%= w.getUsername() %> (<%= w.getRole() %>)</option>
                    <% } %>
                </select>
            </label>
            <label><%= msgs.get("links.mailAccount") %>
                <select name="dovecotAccountId">
                    <% for (DovecotAccount d : dovecotAccounts) { %>
                        <option value="<%= d.getId() %>"><%= d.getUsername() %> (<%= d.getDomain() %>)</option>
                    <% } %>
                </select>
            </label>
            <button type="submit"><%= msgs.get("links.submit.link") %></button>
        </form>
    </div>

    <div class="section">
        <h3><%= msgs.get("links.remove.legend") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/links" class="form-grid">
            <input type="hidden" name="action" value="unlink">
            <label><%= msgs.get("links.webAccount") %>
                <select name="webAccountId">
                    <% for (WebAccount w : webAccounts) { %>
                        <option value="<%= w.getId() %>"><%= w.getUsername() %> (<%= w.getRole() %>)</option>
                    <% } %>
                </select>
            </label>
            <label><%= msgs.get("links.mailAccount") %>
                <select name="dovecotAccountId">
                    <% for (DovecotAccount d : dovecotAccounts) { %>
                        <option value="<%= d.getId() %>"><%= d.getUsername() %> (<%= d.getDomain() %>)</option>
                    <% } %>
                </select>
            </label>
            <button type="submit" class="btn-danger"><%= msgs.get("links.submit.unlink") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
