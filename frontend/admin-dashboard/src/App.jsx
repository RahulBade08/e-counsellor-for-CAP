/**
 * E-Counsellor — Admin Dashboard
 * Single-file React (JSX) application
 *
 * Pages:
 *  1. Login
 *  2. Overview       – system-wide KPIs
 *  3. Database       – browse tables, raw counts
 *  4. Accounts       – approve/suspend college & student accounts
 *  5. Data Import    – scrape CET site → preview cleaned data → push to DB
 *  6. ML Model       – retrain, view metrics, status
 *  7. Colleges       – CRUD colleges
 *  8. System Logs    – recent activity feed
 *
 * All API calls target http://localhost:8080 (Spring Boot backend).
 * ML retrain targets http://localhost:8001 (Python ML service).
 */

import { useState, useEffect, useCallback, useRef } from "react";

// ─── CONFIG ──────────────────────────────────────────────────────────────────
const API   = "http://localhost:8080/api";
const ML    = "http://localhost:8001";

// ─── GLOBAL STYLES ───────────────────────────────────────────────────────────
const STYLE = `
  @import url('https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=Sora:wght@300;400;500;600;700&display=swap');

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  :root {
    --bg:        #0a0c10;
    --bg2:       #0f1218;
    --bg3:       #161b24;
    --border:    #1e2530;
    --border2:   #2a3345;
    --text:      #e8edf5;
    --text-muted:#6b7a93;
    --text-dim:  #3d4a5e;
    --accent:    #00e5ff;
    --accent2:   #7b61ff;
    --green:     #00c896;
    --amber:     #ffb547;
    --red:       #ff4d6d;
    --mono:      'Space Mono', monospace;
    --sans:      'Sora', sans-serif;
    --radius:    10px;
    --sidebar-w: 220px;
    --topbar-h:  56px;
  }

  html, body, #root { height: 100%; }
  body {
    background: var(--bg);
    color: var(--text);
    font-family: var(--sans);
    font-size: 14px;
    -webkit-font-smoothing: antialiased;
  }

  /* ── SCROLLBAR ── */
  ::-webkit-scrollbar { width: 5px; height: 5px; }
  ::-webkit-scrollbar-track { background: var(--bg2); }
  ::-webkit-scrollbar-thumb { background: var(--border2); border-radius: 9px; }

  /* ── SHELL ── */
  .shell { display: flex; height: 100vh; overflow: hidden; }

  /* ── TOPBAR ── */
  .topbar {
    position: fixed; top: 0; left: 0; right: 0;
    height: var(--topbar-h);
    background: var(--bg2);
    border-bottom: 1px solid var(--border);
    display: flex; align-items: center; gap: 1rem;
    padding: 0 1.25rem;
    z-index: 100;
  }
  .topbar-logo {
    display: flex; align-items: center; gap: .5rem;
    font-family: var(--mono); font-size: .95rem; font-weight: 700;
    color: var(--accent); letter-spacing: -.5px;
    text-decoration: none;
  }
  .topbar-logo .dot { color: var(--accent2); }
  .topbar-badge {
    font-size: .6rem; font-weight: 700; font-family: var(--mono);
    background: var(--red); color: #fff;
    padding: .15rem .4rem; border-radius: 4px;
    text-transform: uppercase; letter-spacing: .5px;
  }
  .topbar-right { margin-left: auto; display: flex; align-items: center; gap: .75rem; }
  .topbar-user {
    display: flex; align-items: center; gap: .5rem;
    font-size: .8rem; color: var(--text-muted);
  }
  .topbar-avatar {
    width: 30px; height: 30px; border-radius: 50%;
    background: var(--accent2);
    display: flex; align-items: center; justify-content: center;
    font-size: .7rem; font-weight: 700; color: #fff;
    font-family: var(--mono);
  }
  .btn-logout {
    background: none; border: 1px solid var(--border2);
    color: var(--text-muted); font-family: var(--sans); font-size: .75rem;
    padding: .3rem .7rem; border-radius: 6px; cursor: pointer;
    transition: border-color .15s, color .15s;
  }
  .btn-logout:hover { border-color: var(--red); color: var(--red); }

  /* ── SIDEBAR ── */
  .sidebar {
    width: var(--sidebar-w);
    background: var(--bg2);
    border-right: 1px solid var(--border);
    padding-top: calc(var(--topbar-h) + 1rem);
    display: flex; flex-direction: column;
    flex-shrink: 0;
    overflow-y: auto;
  }
  .sidebar-section {
    font-size: .6rem; font-weight: 700; font-family: var(--mono);
    text-transform: uppercase; letter-spacing: 1.5px;
    color: var(--text-dim); padding: .6rem 1.1rem .3rem;
  }
  .nav-item {
    display: flex; align-items: center; gap: .65rem;
    padding: .55rem 1.1rem; cursor: pointer;
    color: var(--text-muted); font-size: .82rem; font-weight: 500;
    border-left: 2px solid transparent;
    transition: color .15s, background .15s, border-color .15s;
    position: relative;
  }
  .nav-item:hover { color: var(--text); background: var(--bg3); }
  .nav-item.active { color: var(--accent); border-left-color: var(--accent); background: rgba(0,229,255,.06); }
  .nav-icon { font-size: 1rem; width: 18px; text-align: center; }
  .nav-badge {
    margin-left: auto; font-size: .6rem; font-family: var(--mono);
    background: var(--red); color: #fff;
    padding: .1rem .35rem; border-radius: 99px; font-weight: 700;
  }

  /* ── MAIN ── */
  .main {
    flex: 1; overflow-y: auto;
    padding: calc(var(--topbar-h) + 1.5rem) 1.75rem 2rem;
  }

  /* ── PAGE HEADER ── */
  .page-header { margin-bottom: 1.5rem; }
  .page-title { font-size: 1.35rem; font-weight: 700; color: var(--text); letter-spacing: -.3px; }
  .page-sub { font-size: .8rem; color: var(--text-muted); margin-top: .2rem; }

  /* ── KPI GRID ── */
  .kpi-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; margin-bottom: 1.5rem; }
  .kpi {
    background: var(--bg2); border: 1px solid var(--border);
    border-radius: var(--radius); padding: 1rem 1.1rem;
    position: relative; overflow: hidden;
  }
  .kpi::before {
    content: ''; position: absolute; top: 0; left: 0; right: 0;
    height: 2px; background: var(--border2);
  }
  .kpi.accent::before { background: var(--accent); }
  .kpi.green::before { background: var(--green); }
  .kpi.amber::before { background: var(--amber); }
  .kpi.red::before   { background: var(--red); }
  .kpi.purple::before{ background: var(--accent2); }
  .kpi-label { font-size: .68rem; font-weight: 600; text-transform: uppercase; letter-spacing: .8px; color: var(--text-muted); margin-bottom: .45rem; }
  .kpi-value { font-size: 1.65rem; font-weight: 700; font-family: var(--mono); color: var(--text); line-height: 1; }
  .kpi-sub   { font-size: .68rem; color: var(--text-muted); margin-top: .35rem; }
  .kpi.accent .kpi-value { color: var(--accent); }
  .kpi.green  .kpi-value { color: var(--green); }
  .kpi.amber  .kpi-value { color: var(--amber); }
  .kpi.red    .kpi-value { color: var(--red); }
  .kpi.purple .kpi-value { color: var(--accent2); }

  /* ── CARD ── */
  .card {
    background: var(--bg2); border: 1px solid var(--border);
    border-radius: var(--radius); margin-bottom: 1.25rem; overflow: hidden;
  }
  .card-header {
    display: flex; align-items: center; justify-content: space-between;
    padding: .85rem 1.1rem; border-bottom: 1px solid var(--border);
  }
  .card-title { font-size: .88rem; font-weight: 700; color: var(--text); }
  .card-sub   { font-size: .73rem; color: var(--text-muted); margin-top: .15rem; }
  .card-body  { padding: 1.1rem; }
  .card-actions { display: flex; gap: .5rem; align-items: center; }

  /* ── TABLE ── */
  .tbl { width: 100%; border-collapse: collapse; }
  .tbl th {
    text-align: left; padding: .6rem .8rem;
    font-size: .65rem; font-weight: 700; font-family: var(--mono);
    text-transform: uppercase; letter-spacing: .8px; color: var(--text-muted);
    background: var(--bg3); border-bottom: 1px solid var(--border);
    white-space: nowrap;
  }
  .tbl td {
    padding: .65rem .8rem; font-size: .8rem; color: var(--text);
    border-bottom: 1px solid var(--border); vertical-align: middle;
  }
  .tbl tr:last-child td { border-bottom: none; }
  .tbl tr:hover td { background: rgba(255,255,255,.02); }
  .tbl-wrap { overflow-x: auto; }
  .mono { font-family: var(--mono); font-size: .78rem; }

  /* ── BADGES ── */
  .badge {
    display: inline-flex; align-items: center;
    font-size: .62rem; font-weight: 700; font-family: var(--mono);
    text-transform: uppercase; letter-spacing: .5px;
    padding: .15rem .45rem; border-radius: 4px;
  }
  .badge-green  { background: rgba(0,200,150,.15); color: var(--green); }
  .badge-red    { background: rgba(255,77,109,.15); color: var(--red); }
  .badge-amber  { background: rgba(255,181,71,.15); color: var(--amber); }
  .badge-blue   { background: rgba(0,229,255,.12); color: var(--accent); }
  .badge-purple { background: rgba(123,97,255,.15); color: var(--accent2); }
  .badge-muted  { background: var(--bg3); color: var(--text-muted); }

  /* ── BUTTONS ── */
  .btn {
    display: inline-flex; align-items: center; gap: .4rem;
    font-family: var(--sans); font-size: .78rem; font-weight: 600;
    padding: .45rem .9rem; border-radius: 7px; cursor: pointer;
    transition: all .15s; border: 1px solid transparent; white-space: nowrap;
  }
  .btn:disabled { opacity: .45; cursor: not-allowed; }
  .btn-primary  { background: var(--accent); color: #000; border-color: var(--accent); }
  .btn-primary:hover:not(:disabled) { background: #00ccee; }
  .btn-danger   { background: var(--red); color: #fff; border-color: var(--red); }
  .btn-danger:hover:not(:disabled)  { background: #e04060; }
  .btn-success  { background: var(--green); color: #000; border-color: var(--green); }
  .btn-success:hover:not(:disabled) { background: #00b085; }
  .btn-amber    { background: var(--amber); color: #000; border-color: var(--amber); }
  .btn-amber:hover:not(:disabled)   { background: #efa030; }
  .btn-outline  {
    background: transparent; color: var(--text-muted);
    border-color: var(--border2);
  }
  .btn-outline:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); }
  .btn-ghost { background: transparent; border: none; color: var(--text-muted); padding: .3rem .5rem; }
  .btn-ghost:hover { color: var(--text); }
  .btn-sm { font-size: .72rem; padding: .3rem .65rem; }
  .btn-icon { padding: .4rem; border-radius: 6px; }

  /* ── FORMS ── */
  .form-group { margin-bottom: .9rem; }
  .form-label { display: block; font-size: .7rem; font-weight: 600; color: var(--text-muted); margin-bottom: .3rem; text-transform: uppercase; letter-spacing: .5px; }
  .form-input, .form-select, .form-textarea {
    width: 100%;
    background: var(--bg3); border: 1px solid var(--border);
    color: var(--text); font-family: var(--sans); font-size: .82rem;
    padding: .55rem .75rem; border-radius: 7px;
    transition: border-color .15s, box-shadow .15s; outline: none;
  }
  .form-input:focus, .form-select:focus, .form-textarea:focus {
    border-color: var(--accent); box-shadow: 0 0 0 2px rgba(0,229,255,.12);
  }
  .form-input::placeholder { color: var(--text-dim); }
  .form-textarea { resize: vertical; min-height: 80px; }
  .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }

  /* ── ALERT ── */
  .alert {
    display: flex; align-items: flex-start; gap: .6rem;
    padding: .7rem .9rem; border-radius: 8px;
    font-size: .8rem; margin-bottom: .9rem;
  }
  .alert-error   { background: rgba(255,77,109,.1); border: 1px solid rgba(255,77,109,.3); color: #ff8fa3; }
  .alert-success { background: rgba(0,200,150,.1); border: 1px solid rgba(0,200,150,.3); color: var(--green); }
  .alert-info    { background: rgba(0,229,255,.08); border: 1px solid rgba(0,229,255,.25); color: var(--accent); }
  .alert-warning { background: rgba(255,181,71,.1); border: 1px solid rgba(255,181,71,.3); color: var(--amber); }

  /* ── SPINNER ── */
  .spinner-wrap { display: flex; align-items: center; justify-content: center; padding: 3rem; }
  .spinner {
    width: 28px; height: 28px; border: 2px solid var(--border2);
    border-top-color: var(--accent); border-radius: 50%;
    animation: spin .7s linear infinite;
  }
  @keyframes spin { to { transform: rotate(360deg); } }

  /* ── PROGRESS BAR ── */
  .progress-bar {
    height: 6px; background: var(--bg3); border-radius: 99px; overflow: hidden;
    margin: .5rem 0;
  }
  .progress-fill {
    height: 100%; border-radius: 99px; background: var(--accent);
    transition: width .4s ease;
  }

  /* ── STEP INDICATOR ── */
  .steps { display: flex; gap: 0; margin-bottom: 1.5rem; }
  .step {
    flex: 1; display: flex; align-items: center; gap: .5rem;
    font-size: .72rem; font-weight: 600; color: var(--text-muted);
    padding: .6rem .9rem;
    background: var(--bg3); border: 1px solid var(--border);
    position: relative;
  }
  .step:not(:last-child)::after {
    content: ''; position: absolute; right: -1px; top: 50%;
    transform: translateY(-50%);
    border: 6px solid transparent;
    border-left-color: var(--border2);
    z-index: 1;
  }
  .step:first-child { border-radius: 8px 0 0 8px; }
  .step:last-child  { border-radius: 0 8px 8px 0; }
  .step.active { background: rgba(0,229,255,.08); border-color: var(--accent); color: var(--accent); }
  .step.done   { background: rgba(0,200,150,.08); border-color: var(--green); color: var(--green); }
  .step-num {
    width: 20px; height: 20px; border-radius: 50%;
    display: flex; align-items: center; justify-content: center;
    font-family: var(--mono); font-size: .65rem; font-weight: 700;
    background: var(--border2); color: var(--text-muted); flex-shrink: 0;
  }
  .step.active .step-num { background: var(--accent); color: #000; }
  .step.done   .step-num { background: var(--green); color: #000; }

  /* ── LOG ENTRY ── */
  .log-entry {
    display: flex; align-items: flex-start; gap: .75rem;
    padding: .6rem 0; border-bottom: 1px solid var(--border);
    font-size: .78rem;
  }
  .log-entry:last-child { border-bottom: none; }
  .log-time { font-family: var(--mono); font-size: .65rem; color: var(--text-dim); flex-shrink: 0; min-width: 80px; }
  .log-dot { width: 7px; height: 7px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; }
  .log-msg { color: var(--text-muted); flex: 1; line-height: 1.45; }
  .log-actor { color: var(--text); font-weight: 600; }

  /* ── DB TABLE VIEWER ── */
  .db-tabs { display: flex; gap: .3rem; margin-bottom: 1rem; flex-wrap: wrap; }
  .db-tab {
    font-family: var(--mono); font-size: .7rem; font-weight: 700;
    padding: .35rem .7rem; border-radius: 6px;
    background: var(--bg3); border: 1px solid var(--border);
    color: var(--text-muted); cursor: pointer; transition: all .15s;
  }
  .db-tab:hover { border-color: var(--accent); color: var(--accent); }
  .db-tab.active { background: rgba(0,229,255,.1); border-color: var(--accent); color: var(--accent); }

  /* ── AUTH PAGE ── */
  .auth-page {
    min-height: 100vh; display: flex; align-items: center; justify-content: center;
    background: var(--bg);
    background-image: radial-gradient(circle at 20% 50%, rgba(0,229,255,.05) 0%, transparent 50%),
                      radial-gradient(circle at 80% 20%, rgba(123,97,255,.05) 0%, transparent 50%);
  }
  .auth-card {
    width: 100%; max-width: 380px;
    background: var(--bg2); border: 1px solid var(--border);
    border-radius: 16px; padding: 2.25rem;
  }
  .auth-brand {
    display: flex; align-items: center; gap: .5rem;
    font-family: var(--mono); font-size: 1.1rem; font-weight: 700;
    color: var(--accent); margin-bottom: 1.75rem;
  }
  .auth-title { font-size: 1.4rem; font-weight: 700; margin-bottom: .3rem; letter-spacing: -.3px; }
  .auth-sub   { font-size: .8rem; color: var(--text-muted); margin-bottom: 1.5rem; }

  /* ── ML STATUS RING ── */
  .ml-status-ring {
    width: 120px; height: 120px; border-radius: 50%;
    border: 3px solid var(--border2); position: relative;
    display: flex; align-items: center; justify-content: center; flex-direction: column;
    margin: 0 auto 1rem;
  }
  .ml-status-ring.active { border-color: var(--green); box-shadow: 0 0 20px rgba(0,200,150,.2); }
  .ml-status-ring.training { border-color: var(--amber); box-shadow: 0 0 20px rgba(255,181,71,.2); animation: pulse 1.5s ease-in-out infinite; }
  @keyframes pulse { 0%,100%{opacity:1} 50%{opacity:.6} }
  .ml-ring-label { font-family: var(--mono); font-size: .65rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: .5px; }
  .ml-ring-value { font-size: 1rem; font-weight: 700; color: var(--text); }

  /* ── EMPTY STATE ── */
  .empty { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 3rem 1rem; color: var(--text-muted); gap: .5rem; }
  .empty-icon { font-size: 2.5rem; }
  .empty-msg  { font-size: .82rem; }

  /* ── TWO COL ── */
  .two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 1.25rem; }
  @media (max-width: 900px) { .two-col { grid-template-columns: 1fr; } }

  /* ── SEARCH ROW ── */
  .search-row { display: flex; gap: .6rem; margin-bottom: 1rem; align-items: center; flex-wrap: wrap; }

  /* ── DIFF TABLE (import preview) ── */
  .diff-new    { background: rgba(0,200,150,.08); }
  .diff-update { background: rgba(255,181,71,.06); }
  .diff-skip   { opacity: .5; }

  /* ── SCROLLABLE CODE BLOCK ── */
  .code-block {
    background: #060910; border: 1px solid var(--border);
    border-radius: 8px; padding: .9rem 1rem;
    font-family: var(--mono); font-size: .72rem; color: #7dd3fc;
    overflow-x: auto; white-space: pre; line-height: 1.6;
    max-height: 260px; overflow-y: auto;
  }

  /* ── ANIMATIONS ── */
  .fade-in { animation: fadeIn .25s ease; }
  @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: none; } }

  .divider { height: 1px; background: var(--border); margin: 1rem 0; }
  .flex { display: flex; }
  .gap-2 { gap: .5rem; }
  .gap-3 { gap: .75rem; }
  .ml-auto { margin-left: auto; }
  .mt-1 { margin-top: .25rem; }
  .text-muted { color: var(--text-muted); }
  .text-accent { color: var(--accent); }
  .text-green { color: var(--green); }
  .text-red { color: var(--red); }
  .text-amber { color: var(--amber); }
  .fw-700 { font-weight: 700; }
`;

// ─── INJECT CSS ───────────────────────────────────────────────────────────────
function useStyles() {
  useEffect(() => {
    const el = document.createElement("style");
    el.textContent = STYLE;
    document.head.appendChild(el);
    return () => document.head.removeChild(el);
  }, []);
}

// ─── AUTH STORE ───────────────────────────────────────────────────────────────
function useAuth() {
  const [token,   setToken]   = useState(() => sessionStorage.getItem("adm_token") || null);
  const [admin,   setAdmin]   = useState(() => {
    try { return JSON.parse(sessionStorage.getItem("adm_info") || "null"); } catch { return null; }
  });
  const login = (t, a) => { sessionStorage.setItem("adm_token", t); sessionStorage.setItem("adm_info", JSON.stringify(a)); setToken(t); setAdmin(a); };
  const logout = () => { sessionStorage.clear(); setToken(null); setAdmin(null); };
  return { token, admin, login, logout };
}

// ─── API HELPERS ──────────────────────────────────────────────────────────────
async function apiFetch(path, opts = {}, token = null) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res  = await fetch(path, { ...opts, headers: { ...headers, ...(opts.headers||{}) } });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error || data.message || `HTTP ${res.status}`);
  return data;
}
const apiGet  = (url, token) => apiFetch(url, {}, token);
const apiPost = (url, body, token) => apiFetch(url, { method:"POST", body: JSON.stringify(body) }, token);
const apiPut  = (url, body, token) => apiFetch(url, { method:"PUT",  body: JSON.stringify(body) }, token);
const apiDel  = (url, token)       => apiFetch(url, { method:"DELETE" }, token);

// ─── GENERIC HOOKS ────────────────────────────────────────────────────────────
function useFetch(url, token) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [rev, setRev] = useState(0);
  const refresh = useCallback(() => setRev(r => r+1), []);

  useEffect(() => {
    if (!url || !token) return;
    let cancelled = false;
    setLoading(true); setError(null);
    apiGet(url, token)
      .then(d => { if (!cancelled) setData(d); })
      .catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [url, token, rev]);
  return { data, loading, error, refresh };
}

// ─── SHARED UI ────────────────────────────────────────────────────────────────
const Spinner = () => <div className="spinner-wrap"><div className="spinner"/></div>;
const Alert   = ({ type, children }) => <div className={`alert alert-${type} fade-in`}><span>{children}</span></div>;
const Badge   = ({ type, children }) => <span className={`badge badge-${type||"muted"}`}>{children}</span>;

function ConfirmDialog({ msg, onConfirm, onCancel }) {
  return (
    <div style={{ position:"fixed", inset:0, background:"rgba(0,0,0,.6)", display:"flex", alignItems:"center", justifyContent:"center", zIndex:999 }}>
      <div style={{ background:"var(--bg2)", border:"1px solid var(--border)", borderRadius:12, padding:"1.75rem", maxWidth:360, width:"90%" }}>
        <div style={{ fontWeight:700, marginBottom:".75rem" }}>Confirm Action</div>
        <div style={{ color:"var(--text-muted)", fontSize:".82rem", marginBottom:"1.25rem" }}>{msg}</div>
        <div className="flex gap-2">
          <button className="btn btn-danger" onClick={onConfirm}>Confirm</button>
          <button className="btn btn-outline" onClick={onCancel}>Cancel</button>
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// LOGIN
// ══════════════════════════════════════════════════════════════════════════════
function LoginPage({ onLogin }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState("");

  const submit = async () => {
    setError("");
    if (!username || !password) { setError("Username and password are required."); return; }
    setLoading(true);
    try {
      const data = await apiPost(`${API}/admin/auth/login`, { username, password });
      onLogin(data.token, { username: data.username || username, email: data.email });
    } catch (e) { setError(e.message); }
    finally     { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-card fade-in">
        <div className="auth-brand">
          <span>⚡</span> E<span className="dot">·</span>Counsellor
          <span className="topbar-badge" style={{marginLeft:".5rem"}}>Admin</span>
        </div>
        <div className="auth-title">Admin Console</div>
        <div className="auth-sub">Restricted access — authorised personnel only</div>
        {error && <Alert type="error">{error}</Alert>}
        <div className="form-group">
          <label className="form-label">Username</label>
          <input className="form-input" placeholder="admin" value={username}
                 onChange={e => setUsername(e.target.value)}
                 onKeyDown={e => e.key==="Enter" && submit()} />
        </div>
        <div className="form-group">
          <label className="form-label">Password</label>
          <input className="form-input" type="password" placeholder="••••••••" value={password}
                 onChange={e => setPassword(e.target.value)}
                 onKeyDown={e => e.key==="Enter" && submit()} />
        </div>
        <button className="btn btn-primary" style={{width:"100%", justifyContent:"center"}} onClick={submit} disabled={loading}>
          {loading ? "Authenticating…" : "Login →"}
        </button>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// OVERVIEW PAGE
// ══════════════════════════════════════════════════════════════════════════════
function OverviewPage({ token }) {
  const { data: stats, loading, error, refresh } = useFetch(`${API}/admin/stats`, token);

  // Fallback mock while your backend stats endpoint is being built
  const s = stats || {};

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">System Overview</div>
        <div className="page-sub">Live snapshot of the entire E-Counsellor platform</div>
      </div>
      {loading && <Spinner/>}
      {error && <Alert type="warning">Stats endpoint not yet live — showing placeholder data. Build <code>GET /api/admin/stats</code> to populate this.</Alert>}
      <div className="kpi-grid">
        <div className="kpi accent">  <div className="kpi-label">Total Colleges</div>     <div className="kpi-value">{s.totalColleges       ?? "—"}</div> <div className="kpi-sub">In database</div> </div>
        <div className="kpi green">   <div className="kpi-label">Active Students</div>    <div className="kpi-value">{s.activeStudents      ?? "—"}</div> <div className="kpi-sub">Registered in app</div> </div>
        <div className="kpi amber">   <div className="kpi-label">Pending Approvals</div>  <div className="kpi-value">{s.pendingApprovals    ?? "—"}</div> <div className="kpi-sub">College accounts awaiting</div> </div>
        <div className="kpi purple">  <div className="kpi-label">Cutoff Records</div>     <div className="kpi-value">{s.cutoffRecords       ?? "—"}</div> <div className="kpi-sub">Across all years & rounds</div> </div>
        <div className="kpi">         <div className="kpi-label">Courses</div>            <div className="kpi-value">{s.totalCourses        ?? "—"}</div> <div className="kpi-sub">Active course offerings</div> </div>
        <div className="kpi red">     <div className="kpi-label">Suspended Accounts</div> <div className="kpi-value">{s.suspendedAccounts   ?? "—"}</div> <div className="kpi-sub">Blocked users/colleges</div> </div>
      </div>

      <div className="two-col">
        <div className="card">
          <div className="card-header"><div className="card-title">🔗 Backend Services</div></div>
          <div className="card-body">
            {[
              { name:"Spring Boot API",    url:`${API}/admin/test`,   port:"8080" },
              { name:"ML Python Service",  url:`${ML}/health`,        port:"8001" },
            ].map(s => <ServiceStatus key={s.name} {...s} />)}
          </div>
        </div>
        <div className="card">
          <div className="card-header"><div className="card-title">📋 Quick Actions</div></div>
          <div className="card-body" style={{display:"flex", flexDirection:"column", gap:".6rem"}}>
            {[
              ["Go to Accounts", "→ Approve pending college accounts"],
              ["Import CET Data", "→ Scrape & import fresh cutoff data"],
              ["Retrain ML Model", "→ Train model with latest data"],
            ].map(([label, desc]) => (
              <div key={label} style={{display:"flex", alignItems:"center", gap:".75rem", padding:".6rem .75rem", background:"var(--bg3)", borderRadius:8, border:"1px solid var(--border)"}}>
                <div style={{flex:1}}>
                  <div style={{fontWeight:600, fontSize:".8rem"}}>{label}</div>
                  <div style={{fontSize:".7rem", color:"var(--text-muted)"}}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Backend endpoint hint */}
      <div className="card">
        <div className="card-header"><div className="card-title">📌 Backend Endpoints Needed for This Page</div></div>
        <div className="code-block">{`// Add to AdminController.java
@GetMapping("/stats")
public Map<String,Object> getStats() {
    return Map.of(
        "totalColleges",    collegeRepo.count(),
        "activeStudents",   studentRepo.countByActiveTrue(),
        "pendingApprovals", collegeAccountRepo.countByApprovedFalseAndActiveTrue(),
        "cutoffRecords",    cutoffRepo.count(),
        "totalCourses",     courseRepo.count(),
        "suspendedAccounts",studentRepo.countByActiveFalse()
    );
}`}</div>
      </div>
    </div>
  );
}

function ServiceStatus({ name, url, port }) {
  const [status, setStatus] = useState("checking");
  useEffect(() => {
    fetch(url, { signal: AbortSignal.timeout(2500) })
      .then(() => setStatus("up"))
      .catch(() => setStatus("down"));
  }, [url]);
  return (
    <div style={{display:"flex", alignItems:"center", gap:".75rem", padding:".55rem 0", borderBottom:"1px solid var(--border)"}}>
      <div style={{width:8, height:8, borderRadius:"50%", background: status==="up" ? "var(--green)" : status==="down" ? "var(--red)" : "var(--amber)", flexShrink:0}}/>
      <div style={{flex:1}}>
        <div style={{fontWeight:600, fontSize:".8rem"}}>{name}</div>
        <div style={{fontSize:".68rem", color:"var(--text-muted)", fontFamily:"var(--mono)"}}>:{port}</div>
      </div>
      <Badge type={status==="up" ? "green" : status==="down" ? "red" : "amber"}>{status}</Badge>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// DATABASE BROWSER PAGE
// ══════════════════════════════════════════════════════════════════════════════
const DB_TABLES = [
  { key:"colleges",        label:"Colleges",         url:`${API}/admin/db/colleges`,        cols:["collegeId","collegeCode","collegeName","region","district","fundingType"] },
  { key:"college_accounts",label:"College Accounts", url:`${API}/admin/db/college-accounts`,cols:["id","collegeCode","email","contactPersonName","approved","active","createdAt"] },
  { key:"students",        label:"Students",         url:`${API}/admin/db/students`,        cols:["id","name","phone","category","cetPercentile","admissionType","active","createdAt"] },
  { key:"courses",         label:"Courses",          url:`${API}/admin/db/courses`,         cols:["courseId","courseCode","courseName","collegeId","intake","university"] },
  { key:"cutoffs",         label:"Cutoffs",          url:`${API}/admin/db/cutoffs`,         cols:["cutoffId","courseId","categoryId","capCategoryCode","gender","round","lastRank","cutoffPercentile"] },
  { key:"categories",      label:"Categories",       url:`${API}/admin/db/categories`,      cols:["categoryId","categoryName"] },
];

function DatabasePage({ token }) {
  const [active, setActive] = useState(DB_TABLES[0]);
  const [search, setSearch] = useState("");
  const { data, loading, error, refresh } = useFetch(active.url, token);

  const rows = (data || []).filter(r =>
    !search || Object.values(r).some(v => String(v).toLowerCase().includes(search.toLowerCase()))
  );

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">Database Browser</div>
        <div className="page-sub">Read-only view of all tables. Use APIs or SQL client for writes.</div>
      </div>
      <div className="db-tabs">
        {DB_TABLES.map(t => (
          <div key={t.key} className={`db-tab${active.key===t.key?" active":""}`}
               onClick={() => { setActive(t); setSearch(""); }}>
            {t.label}
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <div>
            <div className="card-title">{active.label}</div>
            <div className="card-sub">{error ? "⚠ Could not load — ensure backend endpoint exists" : `${rows.length} records`}</div>
          </div>
          <div className="card-actions">
            <input className="form-input" style={{width:200}} placeholder="Search…"
                   value={search} onChange={e => setSearch(e.target.value)} />
            <button className="btn btn-outline btn-sm" onClick={refresh}>↻ Refresh</button>
          </div>
        </div>

        {loading && <Spinner/>}
        {error && (
          <div className="card-body">
            <Alert type="warning">
              Endpoint <code>{active.url}</code> not yet implemented. See hint below.
            </Alert>
            <div className="code-block">{`// AdminDbController.java — add GET endpoints for each table
@GetMapping("/db/colleges")
public List<College> allColleges() { return collegeRepo.findAll(); }

@GetMapping("/db/college-accounts")
public List<CollegeAccount> allCollegeAccounts() { return collegeAccountRepo.findAll(); }

@GetMapping("/db/students")
public List<Student> allStudents() { return studentRepo.findAll(); }

// ... etc for courses, cutoffs, categories`}</div>
          </div>
        )}
        {!loading && !error && (
          <div className="tbl-wrap">
            <table className="tbl">
              <thead>
                <tr>{active.cols.map(c => <th key={c}>{c}</th>)}</tr>
              </thead>
              <tbody>
                {rows.length === 0 && (
                  <tr><td colSpan={active.cols.length}><div className="empty"><div className="empty-icon">📭</div><div className="empty-msg">No records</div></div></td></tr>
                )}
                {rows.map((r, i) => (
                  <tr key={i}>
                    {active.cols.map(c => (
                      <td key={c}>
                        {c === "approved" || c === "active"
                          ? <Badge type={r[c] ? "green" : "red"}>{r[c] ? "Yes" : "No"}</Badge>
                          : <span className="mono">{String(r[c] ?? "—")}</span>}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ACCOUNTS MANAGEMENT PAGE
// ══════════════════════════════════════════════════════════════════════════════
function AccountsPage({ token }) {
  const [tab, setTab] = useState("colleges");
  const [confirm, setConfirm] = useState(null);
  const [busy, setBusy] = useState({});
  const [msg, setMsg] = useState(null);

  const colUrl = `${API}/admin/accounts/colleges`;
  const stuUrl = `${API}/admin/accounts/students`;
  const { data: colleges, loading: cLoad, error: cErr, refresh: cRef } = useFetch(colUrl, token);
  const { data: students, loading: sLoad, error: sErr, refresh: sRef } = useFetch(stuUrl, token);

  const action = async (id, endpoint, label) => {
    setBusy(b => ({ ...b, [id]: true }));
    setMsg(null);
    try {
      await apiPut(`${API}/admin/accounts/${endpoint}/${id}`, {}, token);
      setMsg({ type:"success", text: `${label} successful` });
      cRef(); sRef();
    } catch(e) { setMsg({ type:"error", text: e.message }); }
    finally { setBusy(b => ({ ...b, [id]: false })); }
  };

  const endpointHint = `// AdminAccountController.java
@PutMapping("/accounts/approve-college/{id}")
public ResponseEntity<?> approveCollege(@PathVariable Long id) {
    CollegeAccount acc = collegeAccountRepo.findById(id).orElseThrow();
    acc.setApproved(true);
    collegeAccountRepo.save(acc);
    return ResponseEntity.ok(Map.of("message","approved"));
}

@PutMapping("/accounts/suspend-college/{id}")
public ResponseEntity<?> suspendCollege(@PathVariable Long id) {
    CollegeAccount acc = collegeAccountRepo.findById(id).orElseThrow();
    acc.setActive(false);
    collegeAccountRepo.save(acc);
    return ResponseEntity.ok(Map.of("message","suspended"));
}

@PutMapping("/accounts/suspend-student/{id}")
public ResponseEntity<?> suspendStudent(@PathVariable Long id) {
    Student s = studentRepo.findById(id).orElseThrow();
    s.setActive(false);
    studentRepo.save(s);
    return ResponseEntity.ok(Map.of("message","suspended"));
}

@PutMapping("/accounts/activate-student/{id}")
public ResponseEntity<?> activateStudent(@PathVariable Long id) {
    Student s = studentRepo.findById(id).orElseThrow();
    s.setActive(true);
    studentRepo.save(s);
    return ResponseEntity.ok(Map.of("message","activated"));
}

// GET all college accounts with pending/active filter
@GetMapping("/accounts/colleges")
public List<CollegeAccount> collegeAccounts() { return collegeAccountRepo.findAll(); }

@GetMapping("/accounts/students")
public List<Student> studentAccounts() { return studentRepo.findAll(); }`;

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">Account Management</div>
        <div className="page-sub">Approve, suspend, and manage college and student accounts</div>
      </div>

      {msg && <Alert type={msg.type}>{msg.text}</Alert>}

      <div className="flex gap-2" style={{marginBottom:"1rem"}}>
        {["colleges","students"].map(t => (
          <button key={t} className={`btn ${tab===t?"btn-primary":"btn-outline"}`} onClick={() => setTab(t)}>
            {t === "colleges" ? "🏫 College Accounts" : "🎓 Student Accounts"}
          </button>
        ))}
      </div>

      {/* COLLEGE ACCOUNTS */}
      {tab === "colleges" && (
        <div className="card">
          <div className="card-header">
            <div className="card-title">College Accounts</div>
            <button className="btn btn-outline btn-sm" onClick={cRef}>↻ Refresh</button>
          </div>
          {cLoad && <Spinner/>}
          {cErr && <div className="card-body"><Alert type="warning">Endpoint not found. Add to backend ↓</Alert><div className="code-block">{endpointHint}</div></div>}
          {!cLoad && !cErr && (
            <div className="tbl-wrap">
              <table className="tbl">
                <thead><tr><th>College Code</th><th>Email</th><th>Contact</th><th>Registered</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>
                  {(!colleges || colleges.length === 0) && (
                    <tr><td colSpan={6}><div className="empty"><div className="empty-icon">📭</div><div className="empty-msg">No accounts</div></div></td></tr>
                  )}
                  {(colleges||[]).map(c => (
                    <tr key={c.id} className={!c.approved ? "diff-update" : ""}>
                      <td><span className="mono text-accent">{c.collegeCode}</span></td>
                      <td className="mono" style={{fontSize:".74rem"}}>{c.email}</td>
                      <td>{c.contactPersonName || "—"}<br/><span style={{fontSize:".68rem",color:"var(--text-muted)"}}>{c.contactPhone}</span></td>
                      <td className="mono" style={{fontSize:".7rem"}}>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : "—"}</td>
                      <td>
                        <Badge type={!c.active ? "red" : !c.approved ? "amber" : "green"}>
                          {!c.active ? "Suspended" : !c.approved ? "Pending" : "Active"}
                        </Badge>
                      </td>
                      <td>
                        <div className="flex gap-2">
                          {!c.approved && c.active && (
                            <button className="btn btn-success btn-sm" disabled={busy[c.id]}
                                    onClick={() => action(c.id, "approve-college", "Approval")}>
                              ✓ Approve
                            </button>
                          )}
                          {c.active && (
                            <button className="btn btn-danger btn-sm" disabled={busy[c.id]}
                                    onClick={() => setConfirm({ msg:`Suspend college account ${c.collegeCode}?`, cb:() => action(c.id,"suspend-college","Suspension") })}>
                              ✕ Suspend
                            </button>
                          )}
                          {!c.active && (
                            <button className="btn btn-amber btn-sm" disabled={busy[c.id]}
                                    onClick={() => action(c.id, "activate-college", "Reactivation")}>
                              ↩ Reactivate
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* STUDENT ACCOUNTS */}
      {tab === "students" && (
        <div className="card">
          <div className="card-header">
            <div className="card-title">Student Accounts</div>
            <button className="btn btn-outline btn-sm" onClick={sRef}>↻ Refresh</button>
          </div>
          {sLoad && <Spinner/>}
          {sErr && <div className="card-body"><Alert type="warning">Endpoint not found. Add to backend.</Alert></div>}
          {!sLoad && !sErr && (
            <div className="tbl-wrap">
              <table className="tbl">
                <thead><tr><th>Name</th><th>Phone</th><th>Category</th><th>Percentile</th><th>Registered</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>
                  {(!students || students.length === 0) && (
                    <tr><td colSpan={7}><div className="empty"><div className="empty-icon">📭</div><div className="empty-msg">No students</div></div></td></tr>
                  )}
                  {(students||[]).map(s => (
                    <tr key={s.id}>
                      <td style={{fontWeight:600}}>{s.name}</td>
                      <td className="mono">{s.phone}</td>
                      <td><Badge type="blue">{s.category || "—"}</Badge></td>
                      <td className="mono text-accent">{s.cetPercentile?.toFixed(2) ?? "—"}</td>
                      <td className="mono" style={{fontSize:".7rem"}}>{s.createdAt ? new Date(s.createdAt).toLocaleDateString() : "—"}</td>
                      <td><Badge type={s.active ? "green" : "red"}>{s.active ? "Active" : "Suspended"}</Badge></td>
                      <td>
                        {s.active
                          ? <button className="btn btn-danger btn-sm" disabled={busy[s.id]}
                                    onClick={() => setConfirm({ msg:`Suspend student ${s.name}?`, cb:() => action(s.id,"suspend-student","Suspension") })}>✕ Suspend</button>
                          : <button className="btn btn-success btn-sm" disabled={busy[s.id]}
                                    onClick={() => action(s.id,"activate-student","Activation")}>↩ Activate</button>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {confirm && <ConfirmDialog msg={confirm.msg} onConfirm={() => { confirm.cb(); setConfirm(null); }} onCancel={() => setConfirm(null)} />}
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// DATA IMPORT PAGE (CET Scrape + Clean + Push)
// ══════════════════════════════════════════════════════════════════════════════
const IMPORT_STEPS = ["Configure", "Scrape / Upload", "Preview & Clean", "Push to DB"];

function DataImportPage({ token }) {
  const [step,    setStep]    = useState(0);
  const [config,  setConfig]  = useState({ year:"2024", rounds:"1,2,3,4", mode:"upload" });
  const [rawData, setRawData] = useState(null);
  const [cleaned, setCleaned] = useState(null);
  const [loading, setLoading] = useState(false);
  const [log,     setLog]     = useState([]);
  const [pushResult, setPushResult] = useState(null);
  const [error,   setError]   = useState("");
  const fileRef = useRef();

  const addLog = (msg, type="info") => setLog(l => [...l, { msg, type, ts: new Date().toLocaleTimeString() }]);

  // ── Step 0: Config ──────────────────────────────────────────────────────────
  const StepConfig = () => (
    <div>
      <div className="form-group">
        <label className="form-label">Year</label>
        <input className="form-input" style={{maxWidth:160}} placeholder="e.g. 2024"
               value={config.year} onChange={e => setConfig(c => ({...c, year: e.target.value}))} />
      </div>
      <div className="form-group">
        <label className="form-label">Rounds to Import (comma-separated)</label>
        <input className="form-input" style={{maxWidth:220}} placeholder="1,2,3,4"
               value={config.rounds} onChange={e => setConfig(c => ({...c, rounds: e.target.value}))} />
      </div>
      <div className="form-group">
        <label className="form-label">Import Mode</label>
        <div className="flex gap-2">
          {[["upload","📁 Upload CSV/Excel"],["scrape","🤖 Selenium Scrape (backend)"]].map(([v,l]) => (
            <button key={v} className={`btn ${config.mode===v?"btn-primary":"btn-outline"}`}
                    onClick={() => setConfig(c => ({...c, mode:v}))}>{l}</button>
          ))}
        </div>
      </div>
      {config.mode === "scrape" && (
        <Alert type="info">
          The backend will use Selenium to navigate the MHT-CET CAP website, extract cutoff tables, and return raw JSON.
          Make sure Selenium &amp; ChromeDriver are installed on the server.{" "}
          <strong>Backend endpoint required:</strong> <code>POST /api/admin/import/scrape</code>
        </Alert>
      )}
      {config.mode === "upload" && (
        <Alert type="info">
          Upload a CSV or Excel file downloaded from the MHT-CET CAP portal. Columns expected:{" "}
          <code>college_code, college_name, course_code, course_name, cap_category, gender, round, last_rank, cutoff_percentile</code>
        </Alert>
      )}
      <button className="btn btn-primary" onClick={() => setStep(1)}>
        Next: {config.mode === "upload" ? "Upload File" : "Start Scrape"} →
      </button>
    </div>
  );

  // ── Step 1: Scrape / Upload ─────────────────────────────────────────────────
  const StepScrape = () => {
    const handleFile = async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      setLoading(true); setError(""); setLog([]);
      addLog(`Reading file: ${file.name}`);
      try {
        const text = await file.text();
        const lines = text.trim().split("\n");
        const headers = lines[0].split(",").map(h => h.trim().replace(/"/g,""));
        const rows = lines.slice(1).map(line => {
          const vals = line.split(",").map(v => v.trim().replace(/"/g,""));
          return Object.fromEntries(headers.map((h,i) => [h, vals[i]]));
        });
        addLog(`Parsed ${rows.length} rows, ${headers.length} columns`, "success");
        setRawData({ rows, headers, source:"upload", year: config.year });
        setStep(2);
      } catch(e) { setError("Failed to parse file: " + e.message); addLog("Parse error: " + e.message, "error"); }
      finally { setLoading(false); }
    };

    const handleScrape = async () => {
      setLoading(true); setError(""); setLog([]);
      addLog("Sending scrape request to backend…");
      try {
        const data = await apiPost(`${API}/admin/import/scrape`, { year: config.year, rounds: config.rounds }, token);
        addLog(`Scraped ${data.rows?.length} rows successfully`, "success");
        setRawData(data);
        setStep(2);
      } catch(e) {
        setError(e.message);
        addLog("Scrape failed: " + e.message, "error");
      }
      finally { setLoading(false); }
    };

    return (
      <div>
        {config.mode === "upload" ? (
          <>
            <div
              style={{ border:"2px dashed var(--border2)", borderRadius:12, padding:"2.5rem", textAlign:"center", cursor:"pointer", transition:"border-color .2s" }}
              onClick={() => fileRef.current?.click()}
              onDragOver={e => e.preventDefault()}
              onDrop={e => { e.preventDefault(); const f = e.dataTransfer.files[0]; if(f) { const dt = new DataTransfer(); dt.items.add(f); fileRef.current.files = dt.files; handleFile({target:{files:[f]}}); } }}>
              <div style={{fontSize:"2.5rem", marginBottom:".75rem"}}>📁</div>
              <div style={{fontWeight:700, marginBottom:".3rem"}}>Drop CSV / Excel file here</div>
              <div style={{fontSize:".78rem", color:"var(--text-muted)"}}>or click to browse</div>
              <input ref={fileRef} type="file" accept=".csv,.xlsx,.xls" style={{display:"none"}} onChange={handleFile} />
            </div>
            <div className="card" style={{marginTop:"1rem"}}>
              <div className="card-header"><div className="card-title">📌 Selenium Scraper (Backend)</div></div>
              <div className="code-block">{`# scraper/cet_scraper.py
from selenium import webdriver
from selenium.webdriver.common.by import By
import pandas as pd, time, json

def scrape_cap_cutoffs(year, rounds):
    driver = webdriver.Chrome()
    results = []
    for round_no in rounds:
        url = f"https://cetcell.mahacet.org/cap-cutoff/?year={year}&round={round_no}"
        driver.get(url)
        time.sleep(3)  # wait for JS render
        rows = driver.find_elements(By.CSS_SELECTOR, "table.cutoff-table tbody tr")
        for row in rows:
            cols = row.find_elements(By.TAG_NAME, "td")
            if len(cols) >= 8:
                results.append({
                    "college_code": cols[0].text,
                    "college_name": cols[1].text,
                    "course_code":  cols[2].text,
                    "course_name":  cols[3].text,
                    "cap_category": cols[4].text,
                    "gender":       cols[5].text,
                    "round":        round_no,
                    "last_rank":    cols[6].text,
                    "cutoff_percentile": cols[7].text
                })
    driver.quit()
    return results

# Spring Boot calls this via ProcessBuilder or REST
# Expose as: POST /api/admin/import/scrape`}</div>
            </div>
          </>
        ) : (
          <div>
            <Alert type="info">
              This will trigger a Selenium scrape on the server for year <strong>{config.year}</strong>, rounds <strong>{config.rounds}</strong>.
              Ensure ChromeDriver is installed on the backend server.
            </Alert>
            <button className="btn btn-primary" onClick={handleScrape} disabled={loading}>
              {loading ? "Scraping…" : "🤖 Start Selenium Scrape"}
            </button>
          </div>
        )}
        {loading && <Spinner/>}
        {error && <Alert type="error">{error}</Alert>}
        {log.length > 0 && (
          <div style={{marginTop:"1rem"}}>
            {log.map((l,i) => (
              <div key={i} className="log-entry">
                <div className="log-time">{l.ts}</div>
                <div className="log-dot" style={{background: l.type==="error" ? "var(--red)" : l.type==="success" ? "var(--green)" : "var(--accent)"}}/>
                <div className="log-msg">{l.msg}</div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  };

  // ── Step 2: Preview & Clean ─────────────────────────────────────────────────
  const StepPreview = () => {
    const [cleanedRows, setCleanedRows] = useState(null);
    const [cleaning, setCleaning] = useState(false);

    const clean = async () => {
      setCleaning(true); setLog([]);
      addLog("Starting data cleaning & normalisation…");
      await new Promise(r => setTimeout(r, 600));
      const rows = rawData.rows || [];
      const issues = [];
      const out = rows.map((r, idx) => {
        const cleaned = { ...r };
        let status = "new";

        // 1. Normalise college_code to 5-digit zero-padded
        if (cleaned.college_code) {
          const padded = String(cleaned.college_code).trim().padStart(5, "0");
          if (padded !== cleaned.college_code) { issues.push(`Row ${idx+1}: Padded college_code ${cleaned.college_code} → ${padded}`); }
          cleaned.college_code = padded;
        }

        // 2. Trim all string fields
        Object.keys(cleaned).forEach(k => { if (typeof cleaned[k] === "string") cleaned[k] = cleaned[k].trim(); });

        // 3. Normalise gender
        const g = (cleaned.gender || "").toUpperCase();
        cleaned.gender = g.includes("F") ? "Female" : g.includes("M") ? "Male" : "All";

        // 4. Parse numerics
        cleaned.last_rank = parseInt(cleaned.last_rank) || null;
        cleaned.cutoff_percentile = parseFloat(cleaned.cutoff_percentile) || null;
        cleaned.round = parseInt(cleaned.round) || null;

        // 5. Normalise cap_category code
        if (cleaned.cap_category) cleaned.cap_category = cleaned.cap_category.toUpperCase().replace(/\s+/g,"");

        // 6. Flag obvious bad rows
        if (!cleaned.cutoff_percentile || cleaned.cutoff_percentile > 100) status = "skip";

        return { ...cleaned, _status: status };
      });

      addLog(`Cleaned ${out.length} rows — ${out.filter(r=>r._status==="skip").length} flagged for skip`, "success");
      addLog(`${issues.length} normalisation fixes applied`);
      setCleanedRows(out);
      setCleaned(out);
      setCleaning(false);
    };

    return (
      <div>
        {!cleanedRows && (
          <>
            <Alert type="info">Raw data loaded: <strong>{rawData?.rows?.length}</strong> rows from <strong>{rawData?.source}</strong>. Click Clean to normalise.</Alert>
            <div style={{marginBottom:"1rem"}}>
              <div className="card-title" style={{marginBottom:".5rem"}}>Cleaning Rules Applied:</div>
              {[
                "Zero-pad college_code to 5 digits",
                "Trim all string whitespace",
                "Normalise gender → Male / Female / All",
                "Parse last_rank and cutoff_percentile to numbers",
                "Uppercase & strip spaces from CAP category codes",
                "Flag rows with missing/invalid cutoff_percentile as SKIP",
              ].map(r => <div key={r} style={{fontSize:".78rem", color:"var(--text-muted)", padding:".2rem 0"}}>✓ {r}</div>)}
            </div>
            <button className="btn btn-primary" onClick={clean} disabled={cleaning}>
              {cleaning ? "Cleaning…" : "🧹 Clean & Normalise Data"}
            </button>
          </>
        )}

        {cleaning && <Spinner/>}

        {log.length > 0 && (
          <div style={{marginBottom:"1rem"}}>
            {log.map((l,i) => (
              <div key={i} className="log-entry">
                <div className="log-time">{l.ts}</div>
                <div className="log-dot" style={{background: l.type==="error" ? "var(--red)" : l.type==="success" ? "var(--green)" : "var(--accent)"}}/>
                <div className="log-msg">{l.msg}</div>
              </div>
            ))}
          </div>
        )}

        {cleanedRows && (
          <>
            <div className="flex gap-2" style={{marginBottom:"1rem"}}>
              <Badge type="green">{cleanedRows.filter(r=>r._status==="new").length} New</Badge>
              <Badge type="amber">{cleanedRows.filter(r=>r._status==="update").length} Update</Badge>
              <Badge type="red">{cleanedRows.filter(r=>r._status==="skip").length} Skip</Badge>
            </div>
            <div className="tbl-wrap">
              <table className="tbl">
                <thead><tr><th>#</th><th>College Code</th><th>Course</th><th>CAP Cat</th><th>Gender</th><th>Round</th><th>Cutoff %ile</th><th>Status</th></tr></thead>
                <tbody>
                  {cleanedRows.slice(0,50).map((r,i) => (
                    <tr key={i} className={r._status==="skip" ? "diff-skip" : r._status==="new" ? "diff-new" : "diff-update"}>
                      <td className="mono">{i+1}</td>
                      <td className="mono text-accent">{r.college_code}</td>
                      <td style={{maxWidth:180, overflow:"hidden", textOverflow:"ellipsis", whiteSpace:"nowrap"}}>{r.course_name || r.course_code}</td>
                      <td><Badge type="blue">{r.cap_category}</Badge></td>
                      <td>{r.gender}</td>
                      <td className="mono">{r.round}</td>
                      <td className="mono text-green">{r.cutoff_percentile}</td>
                      <td><Badge type={r._status==="skip" ? "red" : r._status==="new" ? "green" : "amber"}>{r._status}</Badge></td>
                    </tr>
                  ))}
                  {cleanedRows.length > 50 && (
                    <tr><td colSpan={8} style={{textAlign:"center", color:"var(--text-muted)", fontFamily:"var(--mono)", fontSize:".72rem"}}>… {cleanedRows.length - 50} more rows</td></tr>
                  )}
                </tbody>
              </table>
            </div>
            <div style={{marginTop:"1rem"}}>
              <button className="btn btn-primary" onClick={() => setStep(3)}>Push to Database →</button>
            </div>
          </>
        )}
      </div>
    );
  };

  // ── Step 3: Push to DB ──────────────────────────────────────────────────────
  const StepPush = () => {
    const [pushing, setPushing] = useState(false);
    const [progress, setProgress] = useState(0);

    const push = async () => {
      setPushing(true); setError(""); setLog([]); setProgress(0);
      const validRows = (cleaned||[]).filter(r => r._status !== "skip");
      addLog(`Pushing ${validRows.length} valid rows to backend…`);
      try {
        // Batch into chunks of 100
        const BATCH = 100;
        let pushed = 0;
        for (let i = 0; i < validRows.length; i += BATCH) {
          const batch = validRows.slice(i, i + BATCH);
          await apiPost(`${API}/admin/import/push`, { year: config.year, rows: batch }, token);
          pushed += batch.length;
          setProgress(Math.round((pushed / validRows.length) * 100));
          addLog(`Pushed ${pushed}/${validRows.length} rows`);
        }
        addLog("All data pushed successfully!", "success");
        setPushResult({ success: true, count: validRows.length });
      } catch(e) {
        setError(e.message);
        addLog("Push failed: " + e.message, "error");
      }
      finally { setPushing(false); }
    };

    return (
      <div>
        {!pushResult && (
          <>
            <Alert type="warning">
              You are about to write <strong>{(cleaned||[]).filter(r=>r._status!=="skip").length} cutoff records</strong> to the database for year <strong>{config.year}</strong>.
              This action is reversible only via SQL.
            </Alert>

            <div className="card" style={{marginBottom:"1rem"}}>
              <div className="card-header"><div className="card-title">📌 Backend Endpoint Required</div></div>
              <div className="code-block">{`// AdminImportController.java
@PostMapping("/import/push")
@Transactional
public ResponseEntity<?> pushCutoffs(@RequestBody ImportPayload payload) {
    int saved = 0;
    for (CutoffRow row : payload.getRows()) {
        College college = collegeRepo.findByCollegeCode(row.getCollegeCode())
            .orElseGet(() -> {
                College c = new College();
                c.setCollegeCode(row.getCollegeCode());
                c.setCollegeName(row.getCollegeName());
                return collegeRepo.save(c);
            });
        Course course = courseRepo.findByCollegeAndCourseCode(college, row.getCourseCode())
            .orElseGet(() -> {
                Course c = new Course();
                c.setCollege(college); c.setCourseCode(row.getCourseCode());
                c.setCourseName(row.getCourseName());
                return courseRepo.save(c);
            });
        Category cat = categoryRepo.findByName(row.getCapCategory())
            .orElseGet(() -> { Category c = new Category(); c.setCategoryName(row.getCapCategory()); return categoryRepo.save(c); });
        Cutoff cutoff = new Cutoff();
        cutoff.setCourse(course); cutoff.setCategory(cat);
        cutoff.setCapCategoryCode(row.getCapCategory());
        cutoff.setGender(row.getGender()); cutoff.setRound(row.getRound());
        cutoff.setLastRank(row.getLastRank()); cutoff.setCutoffPercentile(row.getCutoffPercentile());
        cutoffRepo.save(cutoff);
        saved++;
    }
    return ResponseEntity.ok(Map.of("saved", saved));
}`}</div>
            </div>

            <button className="btn btn-primary" onClick={push} disabled={pushing}>
              {pushing ? "Pushing…" : "⬆ Push to Database"}
            </button>

            {pushing && (
              <div style={{marginTop:"1rem"}}>
                <div className="progress-bar"><div className="progress-fill" style={{width:`${progress}%`}}/></div>
                <div className="mono" style={{fontSize:".72rem", color:"var(--text-muted)"}}>{progress}% complete</div>
              </div>
            )}
          </>
        )}

        {pushResult?.success && (
          <Alert type="success">✅ Successfully pushed {pushResult.count} records to the database!</Alert>
        )}

        {error && <Alert type="error">{error}</Alert>}

        {log.map((l,i) => (
          <div key={i} className="log-entry">
            <div className="log-time">{l.ts}</div>
            <div className="log-dot" style={{background: l.type==="error" ? "var(--red)" : l.type==="success" ? "var(--green)" : "var(--accent)"}}/>
            <div className="log-msg">{l.msg}</div>
          </div>
        ))}

        {pushResult?.success && (
          <button className="btn btn-outline" style={{marginTop:"1rem"}} onClick={() => { setStep(0); setRawData(null); setCleaned(null); setLog([]); setPushResult(null); }}>
            ↩ Start New Import
          </button>
        )}
      </div>
    );
  };

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">CET Data Import</div>
        <div className="page-sub">Scrape, clean, and push MHT-CET cutoff history into the database</div>
      </div>

      <div className="steps">
        {IMPORT_STEPS.map((s, i) => (
          <div key={s} className={`step ${i < step ? "done" : i === step ? "active" : ""}`}>
            <div className="step-num">{i < step ? "✓" : i+1}</div>
            {s}
          </div>
        ))}
      </div>

      <div className="card">
        <div className="card-body">
          {step === 0 && <StepConfig/>}
          {step === 1 && <StepScrape/>}
          {step === 2 && rawData && <StepPreview/>}
          {step === 3 && cleaned && <StepPush/>}
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ML MODEL PAGE
// ══════════════════════════════════════════════════════════════════════════════
function MLPage({ token }) {
  const [status, setStatus] = useState(null);
  const [training, setTraining] = useState(false);
  const [log, setLog] = useState([]);
  const [error, setError] = useState("");
  const [progress, setProgress] = useState(0);
  const { data: metrics } = useFetch(`${ML}/metrics`, token);

  useEffect(() => {
    fetch(`${ML}/health`).then(r => r.json()).then(d => setStatus(d)).catch(() => setStatus(null));
  }, []);

  const retrain = async () => {
    setTraining(true); setLog([]); setError(""); setProgress(0);
    const addLog = (msg) => setLog(l => [...l, { msg, ts: new Date().toLocaleTimeString() }]);
    addLog("Triggering ML model retrain…");
    try {
      // Simulate progress ticks while waiting
      const tick = setInterval(() => setProgress(p => Math.min(p + 5, 90)), 800);
      const data = await apiPost(`${ML}/retrain`, { source: "db" });
      clearInterval(tick);
      setProgress(100);
      addLog(`Retrain complete! Accuracy: ${data.accuracy ?? "N/A"}, R²: ${data.r2 ?? "N/A"}`);
    } catch(e) {
      setError(e.message);
      addLog("Retrain failed: " + e.message);
    }
    finally { setTraining(false); }
  };

  const isUp = !!status;

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">ML Model Management</div>
        <div className="page-sub">Retrain the admission probability prediction model with latest cutoff data</div>
      </div>

      <div className="two-col">
        {/* Status card */}
        <div className="card">
          <div className="card-header"><div className="card-title">Model Status</div></div>
          <div className="card-body" style={{textAlign:"center"}}>
            <div className={`ml-status-ring ${training ? "training" : isUp ? "active" : ""}`}>
              <div className="ml-ring-value">{training ? "⏳" : isUp ? "✓" : "✕"}</div>
              <div className="ml-ring-label">{training ? "Training" : isUp ? "Active" : "Offline"}</div>
            </div>
            <Badge type={training ? "amber" : isUp ? "green" : "red"} style={{margin:"0 auto"}}>
              {training ? "Retraining" : isUp ? "ML Service Online" : "ML Service Offline"}
            </Badge>
            <div style={{marginTop:"1rem", fontSize:".75rem", color:"var(--text-muted)"}}>
              Endpoint: <code style={{fontFamily:"var(--mono)"}}>{ML}</code>
            </div>
          </div>
        </div>

        {/* Metrics card */}
        <div className="card">
          <div className="card-header"><div className="card-title">Model Metrics</div></div>
          <div className="card-body">
            {!metrics ? (
              <Alert type="warning">Metrics not available — build <code>GET {ML}/metrics</code></Alert>
            ) : (
              <div>
                {[["Accuracy", metrics.accuracy], ["R² Score", metrics.r2], ["Training Samples", metrics.trainingSamples], ["Last Trained", metrics.lastTrained]].map(([l,v]) => (
                  <div key={l} style={{display:"flex", justifyContent:"space-between", padding:".5rem 0", borderBottom:"1px solid var(--border)"}}>
                    <div style={{color:"var(--text-muted)", fontSize:".78rem"}}>{l}</div>
                    <div className="mono">{v ?? "—"}</div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Retrain card */}
      <div className="card">
        <div className="card-header"><div className="card-title">🔁 Retrain Model</div></div>
        <div className="card-body">
          <Alert type="info">
            Retraining pulls fresh cutoff data from the database and retrains the Random Forest / Logistic Regression model.
            Takes ~30–120 seconds depending on data size.
          </Alert>
          <button className="btn btn-primary" onClick={retrain} disabled={training || !isUp}>
            {training ? "Training…" : "⚡ Start Retrain"}
          </button>
          {!isUp && <span style={{marginLeft:".75rem", fontSize:".78rem", color:"var(--text-muted)"}}>ML service is offline — start it first.</span>}

          {training && (
            <div style={{marginTop:"1rem"}}>
              <div className="progress-bar"><div className="progress-fill" style={{width:`${progress}%`, background:"var(--amber)"}}/></div>
              <div className="mono" style={{fontSize:".72rem", color:"var(--amber)"}}>{progress}%</div>
            </div>
          )}

          {error && <Alert type="error">{error}</Alert>}
          {log.map((l,i) => (
            <div key={i} className="log-entry">
              <div className="log-time">{l.ts}</div>
              <div className="log-dot" style={{background:"var(--accent)"}}/>
              <div className="log-msg">{l.msg}</div>
            </div>
          ))}

          <div className="divider"/>
          <div className="code-block">{`# ml_service.py — add retrain endpoint
from flask import Flask, request, jsonify
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
import psycopg2, pickle, numpy as np

@app.route('/retrain', methods=['POST'])
def retrain():
    conn = psycopg2.connect("dbname=ecounsellor_db user=postgres password=admin123")
    df = pd.read_sql("SELECT cutoff_percentile, last_rank FROM cutoffs WHERE cutoff_percentile IS NOT NULL", conn)
    # Feature: difference between student percentile and cutoff
    # Label: 1 if student rank <= last_rank (would get admission)
    # ... training logic ...
    model.fit(X_train, y_train)
    pickle.dump(model, open('model.pkl', 'wb'))
    return jsonify({"accuracy": float(acc), "r2": float(r2), "trainingSamples": len(df)})

@app.route('/metrics')
def metrics():
    meta = json.load(open('model_meta.json'))
    return jsonify(meta)`}</div>
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// COLLEGES CRUD PAGE
// ══════════════════════════════════════════════════════════════════════════════
function CollegesPage({ token }) {
  const { data: colleges, loading, error, refresh } = useFetch(`${API}/admin/college/all`, token);
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState(null); // null | "new" | college object
  const [form, setForm] = useState({});
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState(null);
  const [confirm, setConfirm] = useState(null);

  const filtered = (colleges||[]).filter(c =>
    !search || [c.collegeName,c.collegeCode,c.district,c.region].some(v => String(v||"").toLowerCase().includes(search.toLowerCase()))
  );

  const openNew = () => { setForm({ collegeCode:"", collegeName:"", region:"", district:"", fundingType:"", courseUniversity:"", totalIntake:"", address:"" }); setEditing("new"); };
  const openEdit = (c) => { setForm({...c}); setEditing(c); };

  const save = async () => {
    setSaving(true); setMsg(null);
    try {
      if (editing === "new") {
        await apiPost(`${API}/admin/college`, form, token);
        setMsg({ type:"success", text:"College created." });
      } else {
        await apiPut(`${API}/admin/college/${editing.collegeId}`, form, token);
        setMsg({ type:"success", text:"College updated." });
      }
      setEditing(null); refresh();
    } catch(e) { setMsg({ type:"error", text: e.message }); }
    finally { setSaving(false); }
  };

  const del = async (id) => {
    try {
      await apiDel(`${API}/admin/college/${id}`, token);
      setMsg({ type:"success", text:"College deleted." }); refresh();
    } catch(e) { setMsg({ type:"error", text: e.message }); }
  };

  const FIELDS = [
    ["collegeCode","College Code"],["collegeName","College Name"],["region","Region"],
    ["district","District"],["fundingType","Funding Type"],["courseUniversity","University"],
    ["totalIntake","Total Intake"],["address","Address"],
  ];

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">Colleges</div>
        <div className="page-sub">Create, edit, and delete colleges in the system</div>
      </div>
      {msg && <Alert type={msg.type}>{msg.text}</Alert>}
      <div className="search-row">
        <input className="form-input" style={{maxWidth:300}} placeholder="Search by name, code, district…"
               value={search} onChange={e => setSearch(e.target.value)} />
        <button className="btn btn-primary" onClick={openNew}>+ Add College</button>
        <button className="btn btn-outline btn-sm ml-auto" onClick={refresh}>↻ Refresh</button>
      </div>

      {loading && <Spinner/>}
      {error && <Alert type="error">{error}</Alert>}

      {!loading && !error && (
        <div className="card">
          <div className="card-header">
            <div className="card-title">All Colleges</div>
            <Badge type="blue">{filtered.length} colleges</Badge>
          </div>
          <div className="tbl-wrap">
            <table className="tbl">
              <thead><tr><th>Code</th><th>Name</th><th>Region</th><th>District</th><th>Funding</th><th>Intake</th><th>Actions</th></tr></thead>
              <tbody>
                {filtered.length === 0 && (
                  <tr><td colSpan={7}><div className="empty"><div className="empty-icon">🏫</div><div className="empty-msg">No colleges</div></div></td></tr>
                )}
                {filtered.map(c => (
                  <tr key={c.collegeId}>
                    <td className="mono text-accent">{c.collegeCode}</td>
                    <td style={{fontWeight:600, maxWidth:220}}>{c.collegeName}</td>
                    <td>{c.region||"—"}</td>
                    <td>{c.district||"—"}</td>
                    <td><Badge type="muted">{c.fundingType||"—"}</Badge></td>
                    <td className="mono">{c.totalIntake||"—"}</td>
                    <td>
                      <div className="flex gap-2">
                        <button className="btn btn-outline btn-sm" onClick={() => openEdit(c)}>✏ Edit</button>
                        <button className="btn btn-danger btn-sm" onClick={() => setConfirm({ msg:`Delete ${c.collegeName}?`, cb:() => del(c.collegeId) })}>✕</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Edit / New Modal */}
      {editing && (
        <div style={{position:"fixed",inset:0,background:"rgba(0,0,0,.65)",display:"flex",alignItems:"center",justifyContent:"center",zIndex:999,padding:"1rem"}}>
          <div style={{background:"var(--bg2)",border:"1px solid var(--border)",borderRadius:14,padding:"1.75rem",width:"100%",maxWidth:520,maxHeight:"90vh",overflowY:"auto"}}>
            <div style={{fontWeight:700,fontSize:"1rem",marginBottom:"1.25rem"}}>{editing==="new" ? "Add College" : "Edit College"}</div>
            {msg && <Alert type={msg.type}>{msg.text}</Alert>}
            <div className="form-row">
              {FIELDS.map(([k,l]) => (
                <div className="form-group" key={k}>
                  <label className="form-label">{l}</label>
                  <input className="form-input" value={form[k]||""} onChange={e => setForm(f => ({...f,[k]:e.target.value}))} />
                </div>
              ))}
            </div>
            <div className="flex gap-2 mt-1">
              <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? "Saving…" : "Save"}</button>
              <button className="btn btn-outline" onClick={() => { setEditing(null); setMsg(null); }}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      {confirm && <ConfirmDialog msg={confirm.msg} onConfirm={() => { confirm.cb(); setConfirm(null); }} onCancel={() => setConfirm(null)} />}
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// SYSTEM LOGS PAGE
// ══════════════════════════════════════════════════════════════════════════════
const MOCK_LOGS = [
  { ts:"10:42:05", actor:"admin", msg:"Approved college account: 06155 (VJTI Mumbai)", type:"green" },
  { ts:"10:38:22", actor:"system", msg:"ML model retrained. Accuracy: 0.87, R²: 0.91", type:"accent" },
  { ts:"10:15:00", actor:"admin", msg:"Imported 1,842 cutoff records for year 2024", type:"green" },
  { ts:"09:55:10", actor:"system", msg:"Student account suspended: +91-9876543210", type:"red" },
  { ts:"09:40:00", actor:"admin", msg:"College created: 06299 (New Engineering College)", type:"accent" },
  { ts:"09:10:33", actor:"system", msg:"College self-registration: collegeXYZ@example.com (pending)", type:"amber" },
  { ts:"08:50:00", actor:"admin", msg:"Admin login from 192.168.1.10", type:"muted" },
];

function LogsPage({ token }) {
  const { data: logs, loading, error, refresh } = useFetch(`${API}/admin/logs`, token);
  const display = logs || MOCK_LOGS;

  return (
    <div className="fade-in">
      <div className="page-header">
        <div className="page-title">System Logs</div>
        <div className="page-sub">Admin activity, account changes, imports, and ML events</div>
      </div>
      {!logs && <Alert type="info">Showing mock logs — build <code>GET /api/admin/logs</code> to populate with real data.</Alert>}
      <div className="card">
        <div className="card-header">
          <div className="card-title">Recent Activity</div>
          <button className="btn btn-outline btn-sm" onClick={refresh}>↻ Refresh</button>
        </div>
        <div className="card-body">
          {loading && <Spinner/>}
          {error && <Alert type="error">{error}</Alert>}
          {display.map((l, i) => (
            <div key={i} className="log-entry">
              <div className="log-time">{l.ts || l.timestamp}</div>
              <div className="log-dot" style={{background: l.type==="green" ? "var(--green)" : l.type==="red" ? "var(--red)" : l.type==="amber" ? "var(--amber)" : l.type==="accent" ? "var(--accent)" : "var(--border2)"}}/>
              <div className="log-msg">
                <span className="log-actor">[{l.actor || "system"}]</span>{" "}{l.msg || l.message}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-header"><div className="card-title">📌 Backend Logging Setup</div></div>
        <div className="code-block">{`// AdminLog.java — new entity
@Entity @Table(name="admin_logs")
public class AdminLog {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column private String actor;       // "admin" | "system"
    @Column private String message;
    @Column private String type;        // "info" | "success" | "error" | "warning"
    @Column private LocalDateTime timestamp;
    @PrePersist void prePersist() { if(timestamp==null) timestamp=LocalDateTime.now(); }
}

// AdminLogService.java
@Service public class AdminLogService {
    private final AdminLogRepository repo;
    public void log(String actor, String msg, String type) {
        AdminLog l = new AdminLog();
        l.setActor(actor); l.setMessage(msg); l.setType(type);
        repo.save(l);
    }
}

// Use it in AdminAccountController:
// logService.log("admin", "Approved college " + collegeCode, "success");

// AdminController.java
@GetMapping("/logs")
public List<AdminLog> getLogs() { return logRepo.findTop100ByOrderByTimestampDesc(); }`}</div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// NAV CONFIG
// ══════════════════════════════════════════════════════════════════════════════
const NAV = [
  { id:"overview",  icon:"📊", label:"Overview",        section:"Dashboard" },
  { id:"accounts",  icon:"👥", label:"Accounts",        section:"Management", badge:"pending" },
  { id:"colleges",  icon:"🏫", label:"Colleges",        section:"Management" },
  { id:"db",        icon:"🗄",  label:"Database",        section:"Management" },
  { id:"import",    icon:"⬆",  label:"Data Import",     section:"Data" },
  { id:"ml",        icon:"🤖", label:"ML Model",        section:"Data" },
  { id:"logs",      icon:"📋", label:"System Logs",     section:"System" },
];

// ══════════════════════════════════════════════════════════════════════════════
// ROOT APP
// ══════════════════════════════════════════════════════════════════════════════
export default function App() {
  useStyles();
  const { token, admin, login, logout } = useAuth();
  const [page, setPage] = useState("overview");

  if (!token) return <LoginPage onLogin={login} />;

  const sections = [...new Set(NAV.map(n => n.section))];

  return (
    <div className="shell">
      {/* TOPBAR */}
      <header className="topbar">
        <div className="topbar-logo">
          ⚡ E<span className="dot">·</span>Counsellor
          <span className="topbar-badge">Admin</span>
        </div>
        <div className="topbar-right">
          <div className="topbar-user">
            <div className="topbar-avatar">{(admin?.username || "A")[0].toUpperCase()}</div>
            <span>{admin?.username || "Admin"}</span>
          </div>
          <button className="btn-logout" onClick={logout}>Logout</button>
        </div>
      </header>

      {/* SIDEBAR */}
      <nav className="sidebar">
        {sections.map(sec => (
          <div key={sec}>
            <div className="sidebar-section">{sec}</div>
            {NAV.filter(n => n.section === sec).map(n => (
              <div key={n.id} className={`nav-item${page===n.id?" active":""}`} onClick={() => setPage(n.id)}>
                <span className="nav-icon">{n.icon}</span>
                {n.label}
              </div>
            ))}
          </div>
        ))}
      </nav>

      {/* MAIN */}
      <main className="main">
        {page === "overview"  && <OverviewPage  token={token} />}
        {page === "accounts"  && <AccountsPage  token={token} />}
        {page === "colleges"  && <CollegesPage  token={token} />}
        {page === "db"        && <DatabasePage  token={token} />}
        {page === "import"    && <DataImportPage token={token} />}
        {page === "ml"        && <MLPage        token={token} />}
        {page === "logs"      && <LogsPage      token={token} />}
      </main>
    </div>
  );
}
