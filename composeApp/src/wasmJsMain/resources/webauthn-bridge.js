
function base64urlEncode(str) {
    return btoa(str).replace(/\+/g, '-').replace(/\//g, '/').replace(/=+$/, '');
}

function base64urlDecode(str) {
    str = str.replace(/-/g, '+').replace(/_/g, '/');
    while (str.length % 4) {
        str += '=';
    }
    return atob(str);
}

async function _startRegistration(options) {
    try {
        const credential = await navigator.credentials.create({ publicKey: options });
        return credential;
    } catch (error) {
        console.error("Error during registration:", error);
        throw error;
    }
}


async function _startAuthentication(options) {
    try {
        const assertion = await navigator.credentials.get({ publicKey: options });
        return assertion;
    } catch (error) {
        console.error("Error during authentication:", error);
        throw error;
    }
}

window.myWebAuthnBridge = {
    startRegistration: function(
        challenge,
        rpId,
        rpName,
        userId,
        userName,
        userDisplayName,
        timeout,
        attestationType,
        authenticatorAttachment,
        residentKey,
        requireResidentKey,
        userVerification
    ) {
        console.log("startRegistration called");
        const decodedChallenge = Uint8Array.from(base64urlDecode(challenge), c => c.charCodeAt(0));
        const decodedUserId = Uint8Array.from(base64urlDecode(userId), c => c.charCodeAt(0));

        const options = {
            challenge: decodedChallenge,
            rp: {
                id: rpId,
                name: rpName
            },
            user: {
                id: decodedUserId,
                name: userName,
                displayName: userDisplayName
            },
            pubKeyCredParams: [
                {
                    type: "public-key",
                    alg: -7
                },
                {
                    type: "public-key",
                    alg: -257
                }
            ],
            timeout,
            attestation: attestationType,
            authenticatorSelection: {
                authenticatorAttachment,
                residentKey,
                requireResidentKey,
                userVerification
            }
        };

        return _startRegistration(options);
    },

    startAuthentication: function(
        challenge,
        rpId,
        timeout,
        userVerification,
        allowCredentialIds
    ) {
        const decodedChallenge = Uint8Array.from(base64urlDecode(challenge), c => c.charCodeAt(0));
        const credentialIds = JSON.parse(allowCredentialIds).map(id => ({
            type: 'public-key',
            id: Uint8Array.from(base64urlDecode(id), c => c.charCodeAt(0))
        }));

        const options = {
            challenge: decodedChallenge,
            rpId,
            timeout,
            userVerification,
            allowCredentials: credentialIds
        };

        return _startAuthentication(options);
    },

    arrayBufferToBase64Url: function(buffer) {
        return btoa(String.fromCharCode.apply(null, new Uint8Array(buffer)))
            .replace(/\+/g, '-')
            .replace(/\//g, '_')
            .replace(/=+$/, '');
    },

    encodeBase64: function(bytes) {
            return btoa(String.fromCharCode(...bytes));
        },
        decodeBase64: function(encoded) {
            return Uint8Array.from(atob(encoded), c => c.charCodeAt(0));
        },

    isSecureContext: function() {
        console.log("Secure context:", window.isSecureContext);
        return window.isSecureContext;
    }
};