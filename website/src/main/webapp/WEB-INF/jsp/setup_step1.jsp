<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("setup.step1.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<div class="wizard-wrap">
    <div class="wizard-box">
        <img src="${pageContext.request.contextPath}/assets/logo.svg" alt="logo" height="40">
        <h1><%= msgs.get("setup.step1.heading") %></h1>
        <p><%= msgs.get("setup.step1.intro") %></p>
        <% if (request.getAttribute("error") != null) { %><div class="flash flash-error"><%= request.getAttribute("error") %></div><% } %>
        <form method="post" action="${pageContext.request.contextPath}/setup">
            <input type="hidden" name="action" value="step1">
            <label><%= msgs.get("setup.step1.dbSystem") %>
                <select name="dbSystem">
                    <option value="mariadb">MySQL / MariaDB</option>
                </select>
            </label>
            <label><%= msgs.get("setup.step1.dbName") %>
                <input type="text" name="dbName" value="javadovecotadmin" required></label>
            <button type="submit"><%= msgs.get("setup.step1.continue") %></button>
        </form>
    </div>
</div>
</body>
</html>
