package eu.fordo.mailadmin.service;

/**
 * Derives the default mailbox folder (Dovecot userdb mail location) for an email address when an
 * admin doesn't type an explicit one. Layout is &lt;base&gt;/&lt;domain&gt;/&lt;localpart&gt;, with the base taken
 * from the mail.folderBase config key (default /var/vmail). An admin can always override the
 * result per-account; aliases that should share a mailbox just get pointed at the same folder.
 */
public final class MailFolder {

    private static final String DEFAULT_BASE = "/var/vmail";

    private MailFolder() {}

    /** The configured base directory, trailing slash trimmed. */
    public static String base() {
        String base = AppConfig.get("mail.folderBase", DEFAULT_BASE);
        if (base == null || base.isBlank()) {
            base = DEFAULT_BASE;
        }
        base = base.trim();
        while (base.length() > 1 && base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    /**
     * Default folder for {@code email}: base/domain/localpart. If the address has no '@' (shouldn't
     * happen for a real mailbox), falls back to base/&lt;whole-address&gt; rather than guessing.
     */
    public static String derive(String email) {
        String addr = email == null ? "" : email.trim();
        int at = addr.lastIndexOf('@');
        if (at <= 0 || at == addr.length() - 1) {
            return base() + "/" + addr;
        }
        String local = addr.substring(0, at);
        String domain = addr.substring(at + 1);
        return base() + "/" + domain + "/" + local;
    }
}
