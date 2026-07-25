package com.opencookie.app.data.wallet

/**
 * MWA [ConnectionIdentity] metadata shown by the wallet during authorize/sign.
 *
 * [ICON_RELATIVE] must stay relative — the SDK rejects absolute icon URIs.
 * Wallets resolve it as `$IDENTITY_URI/$ICON_RELATIVE` (e.g. icon.png on the site).
 *
 * Bump [ICON_VERSION] when the website icon changes so persisted auth tokens are dropped
 * and the next session runs a full authorize (wallet refetches icon metadata).
 */
object DAppIdentity {
    const val IDENTITY_URI = "https://open-cookie.pages.dev"
    const val IDENTITY_NAME = "Open Cookie"
    const val ICON_RELATIVE = "icon.png?v=2"
    const val ICON_VERSION = "2"
}
