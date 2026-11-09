import test from "node:test";
import assert from "node:assert/strict";
import { escapeHTML,rupeesToPaise,money,niceDate,statusStyle,parseApiError } from "../src/core.mjs";

test("HTML escaping prevents user-provided scripts",()=>{
 assert.equal(escapeHTML('<img src=x onerror="evil()">&'),"&lt;img src=x onerror=&quot;evil()&quot;&gt;&amp;");
});
test("currency converts rupees to paise without floating-point drift",()=>{
 assert.equal(rupeesToPaise("130.05"),13005);
 assert.equal(rupeesToPaise("0.01"),1);
 assert.equal(rupeesToPaise("1.5"),150);
 assert.throws(()=>rupeesToPaise("1.234"));
 assert.throws(()=>rupeesToPaise("-4"));
 assert.throws(()=>rupeesToPaise("1e4"));
});
test("money and dates display friendly INR and local-neutral date",()=>{
 assert.match(money(12500),/125/);
 assert.equal(niceDate(null),"Not scheduled");
 assert.match(niceDate("2026-11-10"),/Nov/);
});
test("status badges distinguish verified and pending",()=>{
 assert.equal(statusStyle("verified"),"ok");
 assert.equal(statusStyle("PENDING"),"pending");
 assert.equal(statusStyle("FAILED"),"danger");
});
test("error detail keeps trace reference but suppresses server internals",()=>{
 const result=parseApiError(500,{detail:"java.sql.SQLException: secret",requestId:"r-1"});
 assert.equal(result.status,500);
 assert.match(result.message,/Reference: r-1/);
 assert.doesNotMatch(result.message,/SQLException|secret/);
});
