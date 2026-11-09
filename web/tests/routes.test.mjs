import test from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const app = readFileSync(new URL("../src/app.mjs",import.meta.url),"utf8");
const index = readFileSync(new URL("../index.html",import.meta.url),"utf8");
const styles = readFileSync(new URL("../styles.css",import.meta.url),"utf8");
const headers = readFileSync(new URL("../_headers",import.meta.url),"utf8");

test("each primary workspace has a routed screen",()=>{
 for(const route of ["overview","planning","vendors","finance","vendor","notifications","admin"]){
  assert.ok(app.includes('route==="'+route+'"'),"Missing route "+route);
 }
});
test("planner and booking tabs are represented",()=>{
 for(const tab of ["ceremonies","guests","tasks","budget","catering","enquiries","quotes","bookings","payments"]){
  assert.ok(app.includes('"'+tab+'"'),"Missing tab "+tab);
 }
});
test("source contains real endpoint integrations for each role",()=>{
 for(const path of ["/auth/login","/account/me","/weddings","/vendor-profiles/mine",
                   "/admin/vendors/pending","/admin/summary","/razorpay/orders",
                   "/notifications"]){
  assert.ok(app.includes(path),"Missing integration "+path);
 }
});
test("responsive and accessible outer shell exists",()=>{
 assert.match(index,/name="viewport"/);
 assert.match(index,/aria-live="polite"/);
 assert.match(styles,/@media\(max-width:780px\)/);
 assert.match(styles,/:focus-visible/);
});
test("static CSP forbids inline scripts and framing",()=>{
 assert.match(headers,/script-src 'self'/);
 assert.match(headers,/frame-ancestors 'none'/);
 assert.doesNotMatch(headers,/script-src [^\n]*'unsafe-inline'/);
});
test("mutations send location only when there is short-lived consent",()=>{
 assert.match(app,/method !== "GET" && store.geo/);
 assert.match(app,/Date\.now\(\) - store.geo.at < 300000/);
});
