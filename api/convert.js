import crypto from "node:crypto";

const API_URL = "https://eco.taobao.com/router/rest";

function signTop(params, secret) {
  const keys = Object.keys(params).sort();
  const base = keys.map((k) => k + params[k]).join("");
  return crypto.createHmac("md5", secret).update(base, "utf8").digest("hex").toUpperCase();
}

function json(res, status, body) {
  res.status(status).setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type");
  return res.end(JSON.stringify(body));
}

export default async function handler(req, res) {
  if (req.method === "OPTIONS") return json(res, 200, { ok: true });
  if (req.method !== "POST") return json(res, 405, { error: "Only POST is supported." });

  const appKey = process.env.ALIEXPRESS_APP_KEY;
  const appSecret = process.env.ALIEXPRESS_APP_SECRET;
  const trackingId = process.env.ALIEXPRESS_TRACKING_ID;

  if (!appKey || !appSecret || !trackingId) {
    return json(res, 500, { error: "השרת עדיין לא הוגדר עם פרטי AliExpress." });
  }

  const sourceUrl = typeof req.body?.url === "string" ? req.body.url.trim() : "";
  if (!/^https?:\/\/(?:[^/]+\.)?aliexpress\.com(?:\/|$)/i.test(sourceUrl)) {
    return json(res, 400, { error: "כתובת AliExpress אינה תקינה." });
  }

  const now = new Date();
  const timestamp = now.toISOString().slice(0, 19).replace("T", " ");

  const params = {
    app_key: appKey,
    format: "json",
    method: "aliexpress.affiliate.link.generate",
    partner_id: "AliLink",
    sign_method: "hmac",
    timestamp,
    v: "2.0",
    promotion_link_type: "0",
    source_values: sourceUrl,
    tracking_id: trackingId
  };

  params.sign = signTop(params, appSecret);

  const body = new URLSearchParams(params);
  const apiResponse = await fetch(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
    body
  });

  const data = await apiResponse.json();
  const result = data?.aliexpress_affiliate_link_generate_response?.resp_result;
  const affiliateUrl = result?.result?.promotion_links?.promotion_link?.[0]?.promotion_link;

  if (affiliateUrl) {
    return json(res, 200, { affiliateUrl });
  }

  const message = result?.resp_msg || data?.error_response?.sub_msg || data?.error_response?.msg || "AliExpress לא החזירה קישור.";
  return json(res, 502, { error: message });
}
