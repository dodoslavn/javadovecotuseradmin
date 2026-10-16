<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="eu.fordo.mailadmin.service.Messages" %>
<%@ page import="java.util.LinkedHashMap, java.util.Map" %>
<% Messages msgs = Messages.of(request); %>
<%
    // Native display name per language code — same set/order as footer.jspf.
    Map<String, String> accountLanguageNames = new LinkedHashMap<>();
    accountLanguageNames.put("en", "English");
    accountLanguageNames.put("cs", "Čeština");
    accountLanguageNames.put("de", "Deutsch");
    accountLanguageNames.put("es", "Español");
    accountLanguageNames.put("hu", "Magyar");
    accountLanguageNames.put("it", "Italiano");
    accountLanguageNames.put("pl", "Polski");
    accountLanguageNames.put("sk", "Slovenčina");
    accountLanguageNames.put("uk", "Українська");
    String profileLanguage = (String) request.getAttribute("profileLanguage");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= msgs.get("account.heading") %> — <%= msgs.get("app.title") %></title>
    <link rel="icon" href="${pageContext.request.contextPath}/assets/logo.svg" type="image/svg+xml">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=<%= eu.fordo.mailadmin.service.AssetVersion.VALUE %>">
</head>
<body>
<%@ include file="nav.jspf" %>
<main>
    <h2><%= msgs.get("account.heading") %></h2>
    <p class="page-intro"><%= msgs.get("account.intro") %></p>
    <% if (request.getAttribute("errorKey") != null) { %><div class="flash flash-error"><%= msgs.get((String) request.getAttribute("errorKey")) %></div><% } %>
    <% if (request.getAttribute("success") != null) { %><div class="flash flash-success"><%= msgs.get("account.success") %></div><% } %>
    <% if (request.getAttribute("languageSuccess") != null) { %><div class="flash flash-success"><%= msgs.get("account.language.success") %></div><% } %>

    <div class="section">
        <p class="muted"><%= msgs.get("account.username") %>: <strong>${sessionScope.webAccountUsername}</strong></p>
        <form method="post" action="${pageContext.request.contextPath}/account" class="form-grid">
            <label class="field-row"><%= msgs.get("account.newPassword") %>
                <input type="password" name="newPassword" minlength="8" required></label>
            <button type="submit"><%= msgs.get("account.changePassword") %></button>
        </form>
    </div>

    <div class="section">
        <form method="post" action="${pageContext.request.contextPath}/account" class="form-grid">
            <input type="hidden" name="action" value="language">
            <label class="field-row"><%= msgs.get("account.language") %>
                <select name="language">
                    <option value=""<%= profileLanguage == null ? " selected" : "" %>><%= msgs.get("account.language.auto") %></option>
                    <%
                        for (Map.Entry<String, String> e : accountLanguageNames.entrySet()) {
                            String sel = e.getKey().equals(profileLanguage) ? " selected" : "";
                    %>
                        <option value="<%= e.getKey() %>"<%= sel %>><%= e.getValue() %></option>
                    <% } %>
                </select>
            </label>
            <button type="submit"><%= msgs.get("account.language.save") %></button>
        </form>
    </div>
</main>
<%@ include file="footer.jspf" %>
