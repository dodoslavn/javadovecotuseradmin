<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("login.heading") %> — <%= msgs.get("app.title") %></title>
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
            <h1><%= msgs.get("login.heading") %></h1>
            <% if ("1".equals(request.getParameter("invited"))) { %><div class="flash flash-success"><%= msgs.get("login.invited") %></div><% } %>
            <% if (request.getAttribute("error") != null) { %><div class="flash flash-error"><%= msgs.get("login.error") %></div><% } %>
            <form method="post" action="${pageContext.request.contextPath}/login">
                <label><%= msgs.get("login.username") %>
                    <input type="text" name="username" autocomplete="username" required autofocus></label>
                <label><%= msgs.get("login.password") %>
                    <input type="password" name="password" autocomplete="current-password" required></label>
                <button type="submit"><%= msgs.get("login.submit") %></button>
            </form>
        </div>
    </div>
</div>
</body>
</html>
