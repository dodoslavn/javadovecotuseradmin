<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages, eu.fordo.mailadmin.service.DateFormatter, eu.fordo.mailadmin.model.SyncStatus" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("serverConfig.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("serverConfig.heading") %></h2>
    <div class="flash flash-info">
        <%= msgs.get("serverConfig.intro") %>
        <code><%= (String) request.getAttribute("configFilePath") %></code>
        <% if (!(Boolean) request.getAttribute("configFilePresent")) { %>
            — <strong><%= msgs.get("serverConfig.fileMissing") %></strong>
        <% } %>
    </div>

    <div class="section">
        <h3><%= msgs.get("serverConfig.mysql") %></h3>
        <p class="muted"><%= msgs.get("serverConfig.mysqlReadOnlyHint") %></p>
        <div class="table-wrap">
        <table>
            <tr><th><%= msgs.get("serverConfig.dbUrl") %></th><td><%= request.getAttribute("dbUrl") %></td></tr>
            <tr><th><%= msgs.get("serverConfig.dbUser") %></th><td><%= request.getAttribute("dbUser") %></td></tr>
            <tr><th><%= msgs.get("serverConfig.password") %></th><td>
                <span class="badge"><%= ((Boolean) request.getAttribute("dbPasswordSet")) ? msgs.get("serverConfig.passwordSet") : msgs.get("serverConfig.passwordNotSet") %></span>
            </td></tr>
        </table>
        </div>
    </div>

    <div class="section">
        <h3><%= msgs.get("serverConfig.rabbitmq") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/server-config" class="form-grid">
            <input type="hidden" name="action" value="rabbitmq">
            <label><%= msgs.get("rabbitmq.host") %>
                <input type="text" name="host" value="<%= request.getAttribute("rmqHost") %>"></label>
            <label><%= msgs.get("rabbitmq.port") %>
                <input type="number" name="port" value="<%= request.getAttribute("rmqPort") %>"></label>
            <label><%= msgs.get("rabbitmq.vhost") %>
                <input type="text" name="vhost" value="<%= request.getAttribute("rmqVhost") %>"></label>
            <label><%= msgs.get("rabbitmq.username") %>
                <input type="text" name="username" value="<%= request.getAttribute("rmqUsername") %>"></label>
            <label><%= msgs.get("rabbitmq.password") %>
                <span class="badge"><%= ((Boolean) request.getAttribute("rmqPasswordSet")) ? msgs.get("serverConfig.passwordSet") : msgs.get("serverConfig.passwordNotSet") %></span>
                <input type="password" name="password" placeholder="<%= msgs.get("serverConfig.passwordBlankHint") %>"></label>
            <label><%= msgs.get("rabbitmq.exchange") %>
                <input type="text" name="exchange" value="<%= request.getAttribute("rmqExchange") %>"></label>
            <button type="submit"><%= msgs.get("serverConfig.save") %></button>
        </form>
    </div>

    <div class="section">
        <h3><%= msgs.get("serverConfig.sentry") %></h3>
        <form method="post" action="${pageContext.request.contextPath}/admin/server-config" class="form-grid">
            <input type="hidden" name="action" value="sentry">
            <label><%= msgs.get("serverConfig.sentry.dsn") %>
                <input type="text" name="dsn" value="<%= request.getAttribute("sentryDsn") %>" placeholder="https://...@...ingest.sentry.io/..."></label>
            <button type="submit"><%= msgs.get("serverConfig.save") %></button>
        </form>
        <p class="muted"><%= msgs.get("serverConfig.sentry.hint") %></p>
    </div>

    <div class="section">
        <h3><%= msgs.get("serverConfig.sync") %></h3>
        <%
            SyncStatus syncStatus = (SyncStatus) request.getAttribute("syncStatus");
            String newSyncToken = (String) request.getAttribute("newSyncToken");
        %>
        <div class="table-wrap">
        <table>
            <tr><th><%= msgs.get("serverConfig.sync.lastSync") %></th><td>
                <% if (syncStatus != null && syncStatus.getLastSyncAt() != null) { %>
                    <%= DateFormatter.format(syncStatus.getLastSyncAt()) %>
                    <span class="badge <%= "ok".equals(syncStatus.getStatus()) ? "badge-ok" : "badge-off" %>">
                        <%= "ok".equals(syncStatus.getStatus()) ? msgs.get("serverConfig.sync.status.ok") : msgs.get("serverConfig.sync.status.error") %>
                    </span>
                <% } else { %>
                    <span class="muted"><%= msgs.get("serverConfig.sync.never") %></span>
                <% } %>
            </td></tr>
            <% if (syncStatus != null && syncStatus.getDetail() != null && !syncStatus.getDetail().isBlank()) { %>
            <tr><th><%= msgs.get("serverConfig.sync.detail") %></th><td><%= syncStatus.getDetail() %></td></tr>
            <% } %>
            <tr><th><%= msgs.get("serverConfig.sync.token") %></th><td>
                <span class="badge"><%= ((Boolean) request.getAttribute("syncTokenSet")) ? msgs.get("serverConfig.passwordSet") : msgs.get("serverConfig.passwordNotSet") %></span>
            </td></tr>
        </table>
        </div>

        <% if (newSyncToken != null) { %>
            <div class="flash flash-success"><%= msgs.get("serverConfig.sync.newTokenShown") %><br><code><%= newSyncToken %></code></div>
        <% } %>

        <p class="muted"><%= msgs.get("serverConfig.sync.tokenHint") %></p>
        <form method="post" action="${pageContext.request.contextPath}/admin/server-config">
            <input type="hidden" name="action" value="regenerateSyncToken">
            <button type="submit"><%= msgs.get("serverConfig.sync.regenerate") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
