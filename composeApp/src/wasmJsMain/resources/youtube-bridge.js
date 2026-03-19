/**
 * YouTube Player Bridge for Kotlin/Wasm (CMP 1.10.2+)
 *
 * ROOT CAUSE (discovered via live DOM inspection):
 * CMP 1.10.2 mounts the Compose/Skiko canvas inside document.body.shadowRoot.
 * The shadow root has NO <slot>, so any element appended to document.body
 * (light DOM) is never rendered — it gets a 0×0 bounding rect.
 *
 * FIX: append the iframe INTO document.body.shadowRoot so it lives in the
 * same shadow tree as the canvas. Later DOM order puts it above the canvas.
 */
window.wdwYoutubeBridge = {

    player: null,

    /** Creates or replaces the YouTube iframe at Compose window coordinates. */
    showPlayer: function(videoId, left, top, width, height) {
        this.hidePlayer();

        if (!videoId || videoId.trim() === '') {
            console.warn("YoutubeBridge: showPlayer called with empty videoId");
            return;
        }

        const iframe = document.createElement('iframe');
        iframe.id    = 'wdw-youtube-player';
        iframe.src   = 'https://www.youtube.com/embed/' + videoId +
                       '?autoplay=0&rel=0&modestbranding=1';
        iframe.setAttribute('frameborder',     '0');
        iframe.setAttribute('allowfullscreen', 'true');
        iframe.setAttribute('allow',
            'accelerometer; autoplay; clipboard-write; encrypted-media; ' +
            'gyroscope; picture-in-picture; web-share');
        iframe.style.border        = 'none';
        iframe.style.pointerEvents = 'auto';
        iframe.style.borderRadius  = '8px';
        iframe.style.zIndex        = '9999';

        // ── Determine where to insert the iframe ────────────────────────────
        //
        // CMP 1.10.2: canvas is in document.body.shadowRoot inside a <div>.
        // We try to insert into THAT div (position:absolute) first.
        // Fallback 1: shadowRoot root itself (position:fixed).
        // Fallback 2: document.body (pre-CMP-1.10.2 / no shadow root).

        const shadow     = document.body.shadowRoot;
        const canvasDiv  = shadow ? shadow.querySelector('div') : null;

        if (canvasDiv) {
            // Make it a positioned container if it isn't already
            var divPos = window.getComputedStyle(canvasDiv).position;
            if (divPos === 'static') { canvasDiv.style.position = 'relative'; }

            // Subtract the div's own viewport offset so our absolute coords land right
            var rect = canvasDiv.getBoundingClientRect();
            iframe.style.position = 'absolute';
            iframe.style.left     = (left - rect.left) + 'px';
            iframe.style.top      = (top  - rect.top)  + 'px';
            iframe.style.width    = width  + 'px';
            iframe.style.height   = height + 'px';
            canvasDiv.appendChild(iframe);

        } else if (shadow) {
            // Append directly into the shadow root with fixed positioning
            iframe.style.position = 'fixed';
            iframe.style.left     = left   + 'px';
            iframe.style.top      = top    + 'px';
            iframe.style.width    = width  + 'px';
            iframe.style.height   = height + 'px';
            shadow.appendChild(iframe);

        } else {
            // Old-style: no shadow root
            iframe.style.position = 'fixed';
            iframe.style.left     = left   + 'px';
            iframe.style.top      = top    + 'px';
            iframe.style.width    = width  + 'px';
            iframe.style.height   = height + 'px';
            document.body.appendChild(iframe);
        }

        this.player = iframe;
        console.log("YoutubeBridge: player shown for", videoId,
                    "at", left, top, width, height,
                    "(shadowRoot:", !!shadow, "canvasDiv:", !!canvasDiv, ")");
    },

    /** Removes the YouTube iframe. */
    hidePlayer: function() {
        if (this.player && this.player.parentNode) {
            this.player.parentNode.removeChild(this.player);
            this.player = null;
        }
        // Clean up orphans in shadowRoot or body
        ['shadowRoot', 'body'].forEach(function(key) {
            var root = key === 'shadowRoot' ? document.body.shadowRoot : document.body;
            if (!root) return;
            var orphan = root.querySelector('#wdw-youtube-player');
            if (orphan && orphan.parentNode) orphan.parentNode.removeChild(orphan);
        });
    },

    /** Repositions an already-visible player without recreating the iframe. */
    updatePosition: function(left, top, width, height) {
        if (!this.player) return;
        var parent = this.player.parentNode;
        if (this.player.style.position === 'absolute' && parent && parent.getBoundingClientRect) {
            var rect = parent.getBoundingClientRect();
            this.player.style.left = (left - rect.left) + 'px';
            this.player.style.top  = (top  - rect.top)  + 'px';
        } else {
            this.player.style.left = left + 'px';
            this.player.style.top  = top  + 'px';
        }
        this.player.style.width  = width  + 'px';
        this.player.style.height = height + 'px';
    }
};

console.log("YoutubeBridge: wdwYoutubeBridge defined on window");
