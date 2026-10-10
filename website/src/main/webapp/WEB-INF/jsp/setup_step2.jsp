<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("setup.step2.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="wizard-wrap">
    <div class="wizard-box">
        <img src="${pageContext.request.contextPath}/assets/logo.svg" alt="logo" height="40">
        <h1><%= msgs.get("setup.step2.heading") %></h1>
        <% if (request.getAttribute("error") != null) { %><div class="flash flash-error"><%= request.getAttribute("error") %></div><% } %>

        <p><%= msgs.get("setup.step2.intro") %></p>
        <ul class="progress-log"><li><%= ((String) request.getAttribute("commands")).replace("\n", "</li><li>") %></li></ul>

        <p><%= msgs.get("setup.step2.formIntro") %></p>
        <form method="post" action="${pageContext.request.contextPath}/setup">
            <input type="hidden" name="action" value="install">
            <input type="hidden" name="dbName" value="<%= request.getAttribute("dbName") %>">
            <label><%= msgs.get("setup.step2.host") %>
                <input type="text" name="host" value="<%= request.getAttribute("host") %>" required></label>
            <label><%= msgs.get("setup.step2.port") %>
                <input type="number" name="port" value="<%= request.getAttribute("port") %>" required></label>
            <label><%= msgs.get("setup.step2.username") %>
                <input type="text" name="username" value="<%= request.getAttribute("username") %>" required></label>
            <label><%= msgs.get("setup.step2.password") %>
                <input type="text" name="password" value="<%= request.getAttribute("password") %>" required></label>
            <button type="submit"><%= msgs.get("setup.step2.continue") %></button>
        </form>
    </div>
</div>
</body>
</html>
