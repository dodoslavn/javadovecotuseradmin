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
<main class="config-page">
    <h2><%= msgs.get("serverConfig.heading") %></h2>
    <p class="page-intro"><%= msgs.get("serverConfig.pageIntro") %></p>
    <% if (!(Boolean) request.getAttribute("configFilePresent")) { %>
        <div class="flash flash-warning">
            <%= msgs.get("serverConfig.intro") %>
            <code><%= (String) request.getAttribute("configFilePath") %></code>
            — <strong><%= msgs.get("serverConfig.fileMissing") %></strong>
        </div>
    <% } %>

    <div class="section">
        <h3><%= msgs.get("serverConfig.database") %> <span class="muted">(<%= request.getAttribute("dbSystemName") %>)</span></h3>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.dbUrl") %></div>
            <div class="setting-control"><span class="setting-value"><%= request.getAttribute("dbUrl") %></span></div>
        </div>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.dbUser") %></div>
            <div class="setting-control"><span class="setting-value"><%= request.getAttribute("dbUser") %></span></div>
        </div>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.password") %></div>
            <div class="setting-control"><span class="badge"><%= ((Boolean) request.getAttribute("dbPasswordSet")) ? msgs.get("serverConfig.passwordSet") : msgs.get("serverConfig.passwordNotSet") %></span></div>
        </div>
        <p class="muted" style="margin-top:1rem;"><%= msgs.get("serverConfig.mysqlReadOnlyHint") %> <code><%= (String) request.getAttribute("configFilePath") %></code></p>
    </div>

    <% boolean manageMailboxes = (Boolean) request.getAttribute("manageMailboxes"); %>
    <div class="section">
        <h3><%= msgs.get("serverConfig.mailbox") %></h3>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.mailbox.manage") %></div>
            <div class="setting-control"><span class="badge"><%= manageMailboxes ? msgs.get("serverConfig.mailbox.on") : msgs.get("serverConfig.mailbox.off") %></span></div>
        </div>
        <% if (manageMailboxes) { %>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.mailbox.base") %></div>
            <div class="setting-control"><span class="setting-value"><%= request.getAttribute("mailFolderBase") %></span></div>
        </div>
        <% } %>
        <p class="muted" style="margin-top:1rem;"><%= msgs.get("serverConfig.mailbox.hint") %> <code><%= (String) request.getAttribute("configFilePath") %></code></p>
    </div>

    <div class="section">
        <div class="section-head">
            <h3><%= msgs.get("serverConfig.rabbitmq") %></h3>
            <span class="conn-status"><%= msgs.get("serverConfig.rabbitmq.connection") %>:
                <% if ((Boolean) request.getAttribute("rmqConnected")) { %>
                    <span class="badge badge-ok"><%= msgs.get("serverConfig.rabbitmq.connected") %></span>
                <% } else { %>
                    <span class="badge badge-off"><%= msgs.get("serverConfig.rabbitmq.disconnected") %></span>
                <% } %>
            </span>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/admin/server-config">
            <input type="hidden" name="action" value="rabbitmq">
            <div class="setting">
                <div class="setting-label"><label for="rmq-host"><%= msgs.get("rabbitmq.host") %></label></div>
                <div class="setting-control"><input type="text" id="rmq-host" name="host" value="<%= request.getAttribute("rmqHost") %>"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-port"><%= msgs.get("rabbitmq.port") %></label></div>
                <div class="setting-control"><input type="number" id="rmq-port" name="port" value="<%= request.getAttribute("rmqPort") %>"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-vhost"><%= msgs.get("rabbitmq.vhost") %></label></div>
                <div class="setting-control"><input type="text" id="rmq-vhost" name="vhost" value="<%= request.getAttribute("rmqVhost") %>"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-username"><%= msgs.get("rabbitmq.username") %></label></div>
                <div class="setting-control"><input type="text" id="rmq-username" name="username" value="<%= request.getAttribute("rmqUsername") %>"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-password"><%= msgs.get("rabbitmq.password") %></label>
                    <span class="inline-note"><%= ((Boolean) request.getAttribute("rmqPasswordSet")) ? msgs.get("serverConfig.passwordSet") : msgs.get("serverConfig.passwordNotSet") %></span></div>
                <div class="setting-control"><input type="password" id="rmq-password" name="password" placeholder="<%= msgs.get("serverConfig.passwordBlankHint") %>"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-exchange"><%= msgs.get("rabbitmq.exchange") %></label><span class="help-tip" tabindex="0" title="<%= msgs.get("rabbitmq.exchange.hint") %>">?</span></div>
                <div class="setting-control"><input type="text" id="rmq-exchange" name="exchange" value="<%= request.getAttribute("rmqExchange") %>"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-routingkey"><%= msgs.get("rabbitmq.routingKey") %></label><span class="help-tip" tabindex="0" title="<%= msgs.get("rabbitmq.routingKey.hint") %>">?</span></div>
                <div class="setting-control"><input type="text" id="rmq-routingkey" name="routingKey" value="<%= request.getAttribute("rmqRoutingKey") %>" placeholder="account.changed"></div>
            </div>
            <div class="setting">
                <div class="setting-label"><label for="rmq-queue"><%= msgs.get("rabbitmq.queue") %></label><span class="help-tip" tabindex="0" title="<%= msgs.get("rabbitmq.queue.hint") %>">?</span></div>
                <div class="setting-control"><input type="text" id="rmq-queue" name="queue" value="<%= request.getAttribute("rmqQueue") %>" placeholder="mailadmin.sync-status"></div>
            </div>
            <div class="config-actions"><button type="submit"><%= msgs.get("serverConfig.save") %></button></div>
        </form>
    </div>

    <div class="section">
        <h3><%= msgs.get("serverConfig.sentry") %></h3>
        <p class="muted" style="margin-bottom:.5rem;"><%= msgs.get("serverConfig.sentry.hint") %></p>
        <form method="post" action="${pageContext.request.contextPath}/admin/server-config">
            <input type="hidden" name="action" value="sentry">
            <div class="setting">
                <div class="setting-label"><label for="sentry-dsn"><%= msgs.get("serverConfig.sentry.dsn") %></label></div>
                <div class="setting-control setting-control--wide"><input type="text" id="sentry-dsn" name="dsn" value="<%= request.getAttribute("sentryDsn") %>" placeholder="https://...@...ingest.sentry.io/..."></div>
            </div>
            <div class="config-actions"><button type="submit"><%= msgs.get("serverConfig.save") %></button></div>
        </form>
    </div>

    <div class="section">
        <%
            SyncStatus syncStatus = (SyncStatus) request.getAttribute("syncStatus");
            String forceSyncResult = (String) request.getAttribute("forceSyncResult");
            boolean syncReported = syncStatus != null && syncStatus.getStatus() != null;
            boolean syncOk = syncReported && "ok".equals(syncStatus.getStatus());
        %>
        <div class="section-head">
            <h3><%= msgs.get("serverConfig.sync") %></h3>
            <% if (syncReported) { %>
                <span class="conn-status">
                    <span class="badge <%= syncOk ? "badge-ok" : "badge-off" %>"><%= syncOk ? msgs.get("serverConfig.sync.status.ok") : msgs.get("serverConfig.sync.status.error") %></span>
                </span>
            <% } %>
        </div>
        <p class="muted" style="margin-bottom:.5rem;"><%= msgs.get("serverConfig.sync.hint") %></p>
        <% if ("success".equals(forceSyncResult)) { %>
            <div class="flash flash-success"><%= msgs.get("serverConfig.sync.forceSyncSuccess") %></div>
        <% } else if ("error".equals(forceSyncResult)) { %>
            <div class="flash flash-error"><%= msgs.get("serverConfig.sync.forceSyncError") %></div>
        <% } %>
        <div class="setting">
            <div class="setting-label"><label for="autosync-enabled"><%= msgs.get("serverConfig.sync.autoSync") %></label><span class="help-tip" tabindex="0" title="<%= msgs.get("serverConfig.sync.autoSync.hint") %>">?</span></div>
            <div class="setting-control">
                <form method="post" action="${pageContext.request.contextPath}/admin/server-config">
                    <input type="hidden" name="action" value="autosync">
                    <input type="checkbox" id="autosync-enabled" name="enabled" value="true"
                           <%= ((Boolean) request.getAttribute("autoSyncEnabled")) ? "checked" : "" %>
                           onchange="this.form.submit()">
                </form>
            </div>
        </div>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.sync.lastRequestSent") %></div>
            <div class="setting-control"><span class="setting-value">
                <% if (syncStatus != null && syncStatus.getLastRequestSentAt() != null) { %>
                    <%= DateFormatter.format(syncStatus.getLastRequestSentAt()) %>
                <% } else { %>
                    <span class="muted"><%= msgs.get("serverConfig.sync.never") %></span>
                <% } %>
            </span></div>
        </div>
        <div class="setting">
            <div class="setting-label"><%= msgs.get("serverConfig.sync.lastSync") %></div>
            <div class="setting-control"><span class="setting-value">
                <% if (syncStatus != null && syncStatus.getLastSyncAt() != null) { %>
                    <%= DateFormatter.format(syncStatus.getLastSyncAt()) %>
                <% } else { %>
                    <span class="muted"><%= msgs.get("serverConfig.sync.never") %></span>
                <% } %>
            </span></div>
        </div>
        <%-- Only shown on failure — the agent's own success detail (e.g. an account count)
             isn't something the website can verify independently, so it's not worth
             displaying as if it were; a failure's detail text is genuinely diagnostic. --%>
        <% if (syncStatus != null && "error".equals(syncStatus.getStatus()) && syncStatus.getDetail() != null && !syncStatus.getDetail().isBlank()) { %>
        <div class="setting setting--top">
            <div class="setting-label"><%= msgs.get("serverConfig.sync.detail") %></div>
            <div class="setting-control"><span class="setting-value"><%= syncStatus.getDetail() %></span></div>
        </div>
        <% } %>
        <div class="config-actions">
            <form method="post" action="${pageContext.request.contextPath}/admin/server-config">
                <input type="hidden" name="action" value="forceSync">
                <button type="submit"><%= msgs.get("serverConfig.sync.forceSync") %></button>
            </form>
        </div>
    </div>
</main>
<%@ include file="footer.jspf" %>
