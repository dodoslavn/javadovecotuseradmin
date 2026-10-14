<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.List, eu.fordo.mailadmin.model.WebAccountInvite, eu.fordo.mailadmin.service.Messages, eu.fordo.mailadmin.service.DateFormatter" %>
<% Messages msgs = Messages.of(request); %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("invite.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <a class="back-link" href="${pageContext.request.contextPath}/admin/web-accounts"><%= msgs.get("webAccounts.action.back") %></a>
    <h2><%= msgs.get("invite.heading") %></h2>
    <p class="page-intro"><%= msgs.get("invite.intro") %></p>

    <% String generatedLink = (String) request.getAttribute("generatedLink"); %>
    <% if (generatedLink != null) { %>
        <div class="flash flash-success">
            <strong><%= msgs.get("invite.created") %></strong>
            <div class="copy-row"><input type="text" readonly value="<%= generatedLink %>" onclick="this.select()"></div>
            <small><%= msgs.get("invite.createdHint") %></small>
        </div>
    <% } %>

    <div class="section" style="max-width:480px;">
        <form method="post" action="${pageContext.request.contextPath}/admin/web-accounts/invite" class="form-grid">
            <label><%= msgs.get("invite.suggestedUsername") %>
                <input type="text" name="suggestedUsername"></label>
            <label><%= msgs.get("invite.expiryDays") %>
                <input type="number" name="expiryDays" value="7" min="1" max="90"></label>
            <div class="checkbox-row">
                <input type="checkbox" id="inv-admin" name="role" value="ADMIN">
                <label for="inv-admin"><%= msgs.get("invite.adminPrivileges") %></label>
            </div>
            <button type="submit"><%= msgs.get("invite.generate") %></button>
        </form>
    </div>

    <%
        List<WebAccountInvite> invites = (List<WebAccountInvite>) request.getAttribute("invites");
        String inviteBaseUrl = (String) request.getAttribute("inviteBaseUrl");
        if (invites != null && !invites.isEmpty()) {
    %>
    <h3 style="margin-top:1.5rem;"><%= msgs.get("invite.pending") %></h3>
    <div class="account-list">
        <% for (WebAccountInvite inv : invites) { %>
        <div class="account-card">
            <div class="account-card-main">
                <div class="account-card-id">
                    <span class="badge"><%= inv.getRole() %></span>
                    <span><%= inv.getSuggestedUsername() != null ? inv.getSuggestedUsername() : "—" %></span>
                </div>
                <span class="account-card-meta"><%= msgs.get("invite.expires") %>: <%= DateFormatter.format(inv.getExpiresAt()) %></span>
            </div>
            <div class="account-card-footer">
                <div class="copy-row" style="flex:1 1 auto;"><input type="text" readonly value="<%= inviteBaseUrl %>?token=<%= inv.getToken() %>" onclick="this.select()"></div>
                <form method="post" action="${pageContext.request.contextPath}/admin/web-accounts/invite">
                    <input type="hidden" name="action" value="revoke">
                    <input type="hidden" name="id" value="<%= inv.getId() %>">
                    <button type="submit" class="btn-sm btn-danger"><%= msgs.get("invite.revoke") %></button>
                </form>
            </div>
        </div>
        <% } %>
    </div>
    <% } %>
</main>
<%@ include file="footer.jspf" %>
