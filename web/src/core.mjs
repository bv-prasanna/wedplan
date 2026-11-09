export function escapeHTML(value = "") {
  return String(value ?? "").replace(/[&<>"']/g, char => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
  })[char]);
}
export function rupeesToPaise(raw) {
  const value = String(raw ?? "").trim();
  if (!/^(?:0|[1-9]\d{0,9})(?:\.\d{1,2})?$/.test(value)) {
    throw new Error("Enter a valid amount in rupees, with up to two decimal places.");
  }
  const [whole, fraction = ""] = value.split(".");
  const result = Number(whole) * 100 + Number(fraction.padEnd(2, "0"));
  if (!Number.isSafeInteger(result)) throw new Error("Amount is too large.");
  return result;
}
export function money(paise) {
  const amount = Number(paise ?? 0);
  if (!Number.isFinite(amount)) return "—";
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(amount / 100);
}
export function niceDate(date) {
  if (!date) return "Not scheduled";
  const parts = String(date).split("-");
  if (parts.length !== 3) return String(date);
  const parsed = new Date(Date.UTC(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2])));
  return Number.isNaN(parsed.getTime()) ? String(date) :
    new Intl.DateTimeFormat("en-IN", { dateStyle: "medium", timeZone: "UTC" }).format(parsed);
}
export function statusStyle(status) {
  const value = String(status ?? "").toUpperCase();
  if (["VERIFIED","APPROVED","ACCEPTED","CONFIRMED","PROCESSED","COMPLETED","SENT"].includes(value)) return "ok";
  if (["REJECTED","FAILED","DECLINED","CANCELLED"].includes(value)) return "danger";
  return "pending";
}
export function errorText(status, detail) {
  if (status === 401) return "Your session has expired, or your credentials were incorrect.";
  if (status === 403) return "You do not have permission to perform that action.";
  if (status === 404) return "The requested item could not be found.";
  if (status === 409) return detail || "This request conflicts with a recent change. Refresh and retry.";
  if (status === 422 || status === 400) return detail || "Please check the information entered.";
  if (status === 429) return "Too many attempts. Please try again later.";
  if (status >= 500) return "The service could not complete the request. Please try again later.";
  return detail || "Something went wrong.";
}
export function parseApiError(status, body, requestId) {
  const reference = body?.requestId || requestId || "";
  const message = errorText(status, typeof body?.detail === "string" ? body.detail : "");
  const exception = new Error(message + (reference ? " Reference: " + reference : ""));
  exception.status = status;
  exception.requestId = reference;
  return exception;
}
