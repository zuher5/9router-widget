package com.ninerouter.monitor.ui.webview

/**
 * Mobile-friendly CSS & JS injection to adapt 9Router's Next.js web dashboard
 * for optimal touch use on Android devices.
 */
object MobileCss {
    // CSS to ensure touch targets >= 44px, prevent horizontal cutoffs, and disable double-tap zoom
    val MOBILE_CSS = """
        /* Force mobile viewport & disable double-tap zoom delay */
        html, body {
            touch-action: manipulation !important;
            -webkit-text-size-adjust: 100% !important;
            max-width: 100vw !important;
            overflow-x: hidden !important;
        }

        /* Minimum touch target size for buttons, links and interactive items */
        button, a, input, select, [role="button"] {
            min-height: 40px !important;
            min-width: 40px !important;
        }

        /* Prevent ugly tap highlight gray boxes */
        * {
            -webkit-tap-highlight-color: transparent !important;
        }

        /* Smooth scroll for mobile */
        * {
            -webkit-overflow-scrolling: touch !important;
        }

        /* Prevent fixed headers or toasts from overlapping notch / status bar */
        header {
            padding-top: max(8px, env(safe-area-inset-top)) !important;
        }
    """.trimIndent().replace("\n", " ").replace(Regex("\\s+"), " ")

    fun getInjectionScript(): String {
        return """
            (function() {
                var style = document.getElementById('ninerouter-mobile-override');
                if (!style) {
                    style = document.createElement('style');
                    style.id = 'ninerouter-mobile-override';
                    style.type = 'text/css';
                    style.innerHTML = '$MOBILE_CSS';
                    document.head.appendChild(style);
                }
                // Ensure viewport meta tag exists and forbids user-scaling for native app feel
                var meta = document.querySelector('meta[name="viewport"]');
                if (!meta) {
                    meta = document.createElement('meta');
                    meta.name = 'viewport';
                    document.head.appendChild(meta);
                }
                meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';
            })();
        """.trimIndent()
    }
}
