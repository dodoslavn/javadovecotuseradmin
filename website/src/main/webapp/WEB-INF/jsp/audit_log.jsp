<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.AuditLogEntry, eu.fordo.mailadmin.service.Messages, eu.fordo.mailadmin.service.DateFormatter" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("auditLog.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("auditLog.heading") %></h2>
    <div class="section">
        <div class="table-wrap">
        <table>
            <thead>
            <tr>
                <th><%= msgs.get("auditLog.col.when") %></th>
                <th><%= msgs.get("auditLog.col.actor") %></th>
                <th><%= msgs.get("auditLog.col.action") %></th>
                <th><%= msgs.get("auditLog.col.target") %></th>
                <th><%= msgs.get("auditLog.col.detail") %></th>
            </tr>
            </thead>
            <tbody>
            <%
                List<AuditLogEntry> entries = (List<AuditLogEntry>) request.getAttribute("entries");
                for (AuditLogEntry e : entries) {
            %>
            <tr>
                <td data-label="<%= msgs.get("auditLog.col.when") %>"><%= DateFormatter.format(e.getAt()) %></td>
                <td data-label="<%= msgs.get("auditLog.col.actor") %>"><%= e.getActorUsername() %></td>
                <td data-label="<%= msgs.get("auditLog.col.action") %>"><%= e.getAction() %></td>
                <td data-label="<%= msgs.get("auditLog.col.target") %>"><%= e.getTargetType() %> #<%= e.getTargetId() %></td>
                <td data-label="<%= msgs.get("auditLog.col.detail") %>"><%= e.getDetail() %></td>
            </tr>
            <% } %>
            </tbody>
        </table>
        </div>
    </div>
</main>
<%@ include file="footer.jspf" %>
