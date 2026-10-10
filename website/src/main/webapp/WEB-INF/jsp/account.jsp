<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("account.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("account.heading") %></h2>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>
    <% if (request.getAttribute("success") != null) { %><div class="flash flash-success"><%= msgs.get("account.success") %></div><% } %>

    <div class="section">
        <p class="muted"><%= msgs.get("account.username") %>: <strong>${sessionScope.webAccountUsername}</strong></p>
        <form method="post" action="${pageContext.request.contextPath}/account" class="form-grid">
            <label><%= msgs.get("account.newPassword") %>
                <input type="password" name="newPassword" minlength="8" required></label>
            <button type="submit"><%= msgs.get("account.changePassword") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
