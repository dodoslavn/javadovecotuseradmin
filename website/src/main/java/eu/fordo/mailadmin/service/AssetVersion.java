package eu.fordo.mailadmin.service;

/**
 * Cache-busting query param for static assets (css/logo). Captured once when this class
 * first loads — which happens on every redeploy, since that gets a fresh classloader — so
 * every deploy automatically invalidates browsers' cached copies without needing to bump
 * a version number by hand.
 */
public final class AssetVersion {

    public static final String VALUE = Long.toString(System.currentTimeMillis());

    private AssetVersion() {}
}
