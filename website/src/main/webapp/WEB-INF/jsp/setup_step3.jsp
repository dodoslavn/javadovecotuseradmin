<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("setup.step3.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="wizard-wrap">
    <div class="wizard-box">
        <img src="${pageContext.request.contextPath}/assets/logo.svg" alt="logo" height="40">
        <h1><%= msgs.get("setup.step3.heading") %></h1>
        <% if (request.getAttribute("error") != null) { %><div class="flash flash-error"><%= request.getAttribute("error") %></div><% } %>
        <div class="flash flash-success"><%= msgs.get("setup.step3.intro") %></div>
        <form method="post" action="${pageContext.request.contextPath}/setup">
            <input type="hidden" name="action" value="createAdmin">
            <input type="hidden" name="dbName" value="<%= request.getAttribute("dbName") %>">
            <input type="hidden" name="host" value="<%= request.getAttribute("host") %>">
            <input type="hidden" name="port" value="<%= request.getAttribute("port") %>">
            <input type="hidden" name="username" value="<%= request.getAttribute("username") %>">
            <input type="hidden" name="password" value="<%= request.getAttribute("password") %>">
            <label><%= msgs.get("setup.step3.username") %>
                <input type="text" name="adminUsername" required autofocus></label>
            <label><%= msgs.get("setup.step3.password") %>
                <input type="password" name="adminPassword" required></label>
            <button type="submit"><%= msgs.get("setup.step3.finish") %></button>
        </form>
    </div>
</div>
</body>
</html>
