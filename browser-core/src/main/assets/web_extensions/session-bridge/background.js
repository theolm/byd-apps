"use strict";

/**
 * Bridge between the Gecko profile and the Android app.
 *
 * The app asks for a signed-in state, exports the cookie jar to a bundle or
 * restores a bundle from the sibling app. Cookies never leave the device and
 * are only sent to the app that owns this profile.
 */

const AUTH_COOKIE_NAMES = ["SID", "__Secure-1PSID", "__Secure-3PSID"];

function isAuthCookie(cookie) {
  return (
    AUTH_COOKIE_NAMES.includes(cookie.name) &&
    /(^|\.)google\.com$/.test(cookie.domain)
  );
}

/**
 * Reads the whole jar. Firefox returns only non-partitioned cookies unless a
 * partitionKey is passed, and GeckoView enables partitioning, so both calls
 * are merged.
 */
async function readCookies() {
  const byKey = new Map();
  const add = (cookies) => {
    for (const cookie of cookies) {
      const key = [
        cookie.storeId,
        cookie.domain,
        cookie.path,
        cookie.name,
        cookie.firstPartyDomain || "",
        cookie.partitionKey ? JSON.stringify(cookie.partitionKey) : "",
      ].join("\u0000");
      byKey.set(key, cookie);
    }
  };

  add(await browser.cookies.getAll({}));
  try {
    add(await browser.cookies.getAll({ partitionKey: {} }));
  } catch (error) {
    // Older Gecko builds reject the empty partition key; the first call is
    // still useful, so this is not fatal.
  }
  return Array.from(byKey.values());
}

function serialize(cookie) {
  const result = {
    name: cookie.name,
    value: cookie.value,
    domain: cookie.domain,
    path: cookie.path,
    secure: !!cookie.secure,
    httpOnly: !!cookie.httpOnly,
  };
  if (cookie.storeId) result.storeId = cookie.storeId;
  if (cookie.sameSite) result.sameSite = cookie.sameSite;
  if (typeof cookie.expirationDate === "number") {
    result.expirationDate = cookie.expirationDate;
  }
  if (cookie.firstPartyDomain) {
    result.firstPartyDomain = cookie.firstPartyDomain;
  }
  if (cookie.partitionKey) result.partitionKey = cookie.partitionKey;
  return result;
}

function urlFor(cookie) {
  const host = cookie.domain.startsWith(".")
    ? cookie.domain.slice(1)
    : cookie.domain;
  const scheme = cookie.secure ? "https" : "http";
  return `${scheme}://${host}${cookie.path || "/"}`;
}

async function status() {
  const cookies = await readCookies();
  return { signedIn: cookies.some(isAuthCookie) };
}

async function exportSession() {
  const cookies = await readCookies();
  return {
    signedIn: cookies.some(isAuthCookie),
    cookies: cookies.map(serialize),
  };
}

async function importSession(payload) {
  const cookies = (payload && payload.cookies) || [];
  let applied = 0;
  const errors = [];

  for (const cookie of cookies) {
    const details = {
      url: urlFor(cookie),
      name: cookie.name,
      value: cookie.value,
      path: cookie.path || "/",
      secure: !!cookie.secure,
      httpOnly: !!cookie.httpOnly,
    };
    // Only a leading dot marks a domain cookie. Passing the bare host would
    // silently widen a host-only cookie to its subdomains.
    if (cookie.domain && cookie.domain.startsWith(".")) {
      details.domain = cookie.domain;
    }
    if (cookie.storeId) details.storeId = cookie.storeId;
    if (
      cookie.sameSite &&
      cookie.sameSite !== "unspecified"
    ) {
      details.sameSite = cookie.sameSite;
    }
    if (typeof cookie.expirationDate === "number") {
      details.expirationDate = cookie.expirationDate;
    }
    if (cookie.firstPartyDomain) {
      details.firstPartyDomain = cookie.firstPartyDomain;
    }
    if (cookie.partitionKey) details.partitionKey = cookie.partitionKey;

    try {
      await browser.cookies.set(details);
      applied++;
    } catch (error) {
      // SameSite=None is rejected unless the cookie is Secure; retry without
      // the attribute instead of dropping the login cookie.
      delete details.sameSite;
      try {
        await browser.cookies.set(details);
        applied++;
      } catch (retryError) {
        if (errors.length < 5) {
          errors.push({
            name: cookie.name,
            domain: cookie.domain,
            error: String((retryError && retryError.message) || retryError),
          });
        }
      }
    }
  }

  return {
    applied,
    failed: cookies.length - applied,
    errors,
  };
}

async function handle(message) {
  switch (message.type) {
    case "status":
      return status();
    case "export":
      return exportSession();
    case "import":
      return importSession(message.payload);
    default:
      throw new Error(`Unknown request type: ${message.type}`);
  }
}

const port = browser.runtime.connectNative("browser");

port.onMessage.addListener(async (message) => {
  const { id, type } = message || {};
  if (typeof id !== "number") return;
  try {
    const result = await handle(message);
    port.postMessage({ id, ok: true, result });
  } catch (error) {
    port.postMessage({
      id,
      ok: false,
      error: String((error && error.message) || error),
    });
  }
});
