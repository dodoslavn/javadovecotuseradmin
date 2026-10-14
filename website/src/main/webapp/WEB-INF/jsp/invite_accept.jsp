<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("invite.accept.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<div class="login-wrap">
    <div class="login-outer">
        <div class="login-header">
            <img src="${pageContext.request.contextPath}/assets/logo.svg" alt="logo" height="48">
            <div class="login-app-name"><%= msgs.get("app.title") %></div>
        </div>
        <div class="login-box">
            <% if (request.getAttribute("invalid") != null) { %>
                <h1><%= msgs.get("invite.accept.heading") %></h1>
                <div class="flash flash-error"><%= msgs.get("invite.invalid") %></div>
                <a class="btn" href="${pageContext.request.contextPath}/login" style="justify-content:center;"><%= msgs.get("login.heading") %></a>
            <% } else { %>
                <h1><%= msgs.get("invite.accept.heading") %></h1>
                <p class="muted"><%= msgs.get("invite.accept.intro") %></p>
                <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>
                <form method="post" action="${pageContext.request.contextPath}/invite">
                    <input type="hidden" name="token" value="<%= request.getAttribute("token") %>">
                    <label><%= msgs.get("login.username") %>
                        <input type="text" name="username" autocomplete="username" value="<%= request.getAttribute("suggestedUsername") != null ? request.getAttribute("suggestedUsername") : "" %>" required autofocus></label>
                    <label><%= msgs.get("login.password") %>
                        <input type="password" name="password" autocomplete="new-password" minlength="8" required></label>
                    <button type="submit"><%= msgs.get("invite.accept.submit") %></button>
                </form>
            <% } %>
        </div>
    </div>
</div>
</body>
</html>
