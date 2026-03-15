import { useState, useEffect, useCallback } from "react";
import "./App.css";

// ── Config ─────────────────────────────────────────────────────────────────────
const API_BASE  = "http://localhost:8080/api/counselling";
const AUTH_BASE = "http://localhost:8080/api/college/auth";

// ── Auth Store ─────────────────────────────────────────────────────────────────
function useAuth() {
  const [token,   setToken]   = useState(() => localStorage.getItem("ec_token") || null);
  const [profile, setProfile] = useState(() => {
    try { return JSON.parse(localStorage.getItem("ec_profile") || "null"); } catch { return null; }
  });

  const saveSession = (t, p) => {
    localStorage.setItem("ec_token",   t);
    localStorage.setItem("ec_profile", JSON.stringify(p));
    setToken(t);
    setProfile(p);
  };

  const logout = () => {
    localStorage.removeItem("ec_token");
    localStorage.removeItem("ec_profile");
    setToken(null);
    setProfile(null);
  };

  const updateProfile = (p) => {
    localStorage.setItem("ec_profile", JSON.stringify(p));
    setProfile(p);
  };

  return { token, profile, saveSession, logout, updateProfile };
}

// ── API helpers ────────────────────────────────────────────────────────────────
async function apiPost(url, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res  = await fetch(url, { method: "POST", headers, body: JSON.stringify(body) });
  const data = await res.json().catch(() => ({ error: "Invalid response" }));
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

async function apiPut(url, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res  = await fetch(url, { method: "PUT", headers, body: JSON.stringify(body) });
  const data = await res.json().catch(() => ({ error: "Invalid response" }));
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

// ── useFetch hook ───────────────────────────────────────────────────────────────
// refreshKey: increment to force a new fetch — used by Refresh button
function useFetch(url, token) {
  const [data,       setData]       = useState(null);
  const [loading,    setLoading]    = useState(false);
  const [error,      setError]      = useState(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const refresh = useCallback(() => setRefreshKey(k => k + 1), []);

  useEffect(() => {
    if (!url) return;
    let cancelled = false;
    setLoading(true); setError(null); setData(null);
    (async () => {
      try {
        const headers = {};
        if (token) headers["Authorization"] = `Bearer ${token}`;
        // cache: "no-store" ensures browser never serves a cached response
        const res = await fetch(url, { headers, cache: "no-store" });
        if (!res.ok) {
          const e = await res.json().catch(() => ({ error: `HTTP ${res.status}` }));
          throw new Error(e.error || `HTTP ${res.status}`);
        }
        if (!cancelled) setData(await res.json());
      } catch (e) { if (!cancelled) setError(e.message); }
      finally     { if (!cancelled) setLoading(false); }
    })();
    return () => { cancelled = true; };
  // refreshKey in deps causes fresh fetch on every refresh() call
  }, [url, token, refreshKey]);

  return { data, loading, error, refresh };
}

// ── Refresh button ──────────────────────────────────────────────────────────────
function RefreshBtn({ onClick, loading }) {
  return (
    <button onClick={onClick} disabled={loading}
      style={{
        display:"inline-flex", alignItems:"center", gap:".3rem",
        padding:".28rem .7rem", fontSize:".76rem", fontWeight:600,
        background:"transparent", border:"1px solid var(--border)",
        borderRadius:"6px", cursor: loading ? "not-allowed" : "pointer",
        color:"var(--text-muted)", transition:"border-color .15s",
      }}>
      <span style={{ display:"inline-block",
        animation: loading ? "spin 1s linear infinite" : "none" }}>↻</span>
      {loading ? "Refreshing…" : "Refresh"}
    </button>
  );
}

// ── Shared UI ──────────────────────────────────────────────────────────────────
function Spinner() {
  return <div className="spinner-wrap"><div className="spinner" /></div>;
}

function Alert({ type, children }) {
  const icons = { error: "⚠️", success: "✅", info: "ℹ️", warning: "⚡" };
  return (
    <div className={`alert alert-${type}`}>
      <span>{icons[type]}</span>
      <span>{children}</span>
    </div>
  );
}

function Badge({ type, children }) {
  return <span className={`badge badge-${type || "muted"}`}>{children}</span>;
}

function TrendBadge({ value }) {
  if (!value) return <Badge type="muted">—</Badge>;
  const t = value.toUpperCase();
  return <Badge type={t === "RISING" ? "rising" : t === "FALLING" ? "falling" : "stable"}>{t}</Badge>;
}

function BarChart({ items, colorClass }) {
  const max = Math.max(...items.map(i => i.value), 1);
  return (
    <div className="bar-chart">
      {items.map(({ label, value }) => (
        <div className="bar-row" key={label}>
          <div className="bar-label">{label}</div>
          <div className="bar-track">
            <div className={`bar-fill ${colorClass || ""}`}
                 style={{ width: `${(value / max) * 100}%` }} />
          </div>
          <div className="bar-count">{value.toLocaleString()}</div>
        </div>
      ))}
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// LOGIN PAGE
// ══════════════════════════════════════════════════════════════════════════════
function LoginPage({ onLogin, onGoRegister }) {
  const [email,    setEmail]    = useState("");
  const [password, setPassword] = useState("");
  const [loading,  setLoading]  = useState(false);
  const [error,    setError]    = useState("");

  const submit = async () => {
    setError("");
    if (!email || !password) { setError("Email and password are required."); return; }
    setLoading(true);
    try {
      const data = await apiPost(`${AUTH_BASE}/login`, { email, password });
      onLogin(data.token, data.profile);
    } catch (e) { setError(e.message); }
    finally     { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-left">
        <div className="auth-brand">
          <div className="auth-brand-icon">🎓</div>
          <div className="auth-brand-name">E-<span>Counsellor</span></div>
        </div>
        <div className="auth-hero-title">
          Your College's<br />
          Admission Intelligence<br />
          Dashboard
        </div>
        <div className="auth-hero-sub">
          Maharashtra MHT-CET counselling data — who's interested in your college,
          which students to target, and where your cutoffs are headed.
        </div>
        <div className="auth-features">
          {[
            ["👥", "Interested Students",    "See who viewed & shortlisted your college from the app"],
            ["🎯", "Students to Target",     "Find eligible students who haven't discovered you yet"],
            ["📊", "Cutoff Target Ranges",   "Data-driven percentile ranges per branch & category"],
            ["📈", "Cutoff History + ML",    "Historical cutoffs with ML predictions and trend analysis"],
          ].map(([icon, label, desc]) => (
            <div className="auth-feature" key={label}>
              <div className="auth-feature-icon">{icon}</div>
              <div className="auth-feature-text">
                <div className="auth-feature-label">{label}</div>
                <div className="auth-feature-desc">{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="auth-right">
        <div className="auth-form-wrap">
          <div className="auth-form-title">Welcome Back</div>
          <div className="auth-form-sub">Login to your college counselling dashboard</div>

          {error && <Alert type="error">{error}</Alert>}

          <div className="form-group">
            <label className="form-label">College Email</label>
            <input className="form-input" type="email"
                   placeholder="principal@college.ac.in"
                   value={email} onChange={e => setEmail(e.target.value)}
                   onKeyDown={e => e.key === "Enter" && submit()} />
          </div>
          <div className="form-group">
            <label className="form-label">Password</label>
            <input className="form-input" type="password"
                   placeholder="Min 6 characters"
                   value={password} onChange={e => setPassword(e.target.value)}
                   onKeyDown={e => e.key === "Enter" && submit()} />
          </div>

          <button className="btn btn-primary" onClick={submit} disabled={loading}
                  style={{ marginTop: ".5rem" }}>
            {loading ? "Logging in…" : "Login to Dashboard →"}
          </button>

          <div className="divider">or</div>

          <div style={{ textAlign: "center", fontSize: ".875rem", color: "var(--text-muted)" }}>
            New college?{" "}
            <button className="link-btn" onClick={onGoRegister}>Register your college</button>
          </div>
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// REGISTER PAGE
// ══════════════════════════════════════════════════════════════════════════════
function RegisterPage({ onLogin, onGoLogin }) {
  const [f, setF] = useState({ collegeCode: "", email: "", password: "", confirm: "", name: "", phone: "" });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState("");
  const [done,    setDone]    = useState(false);
  const set = (k, v) => setF(p => ({ ...p, [k]: v }));

  const submit = async () => {
    setError("");
    if (!f.collegeCode)        { setError("DTE college code is required."); return; }
    if (!f.email)              { setError("Email is required."); return; }
    if (f.password.length < 6) { setError("Password must be at least 6 characters."); return; }
    if (f.password !== f.confirm) { setError("Passwords do not match."); return; }
    setLoading(true);
    try {
      const data = await apiPost(`${AUTH_BASE}/register`, {
        collegeCode: f.collegeCode.trim(),
        email: f.email.trim().toLowerCase(),
        password: f.password,
        contactPersonName: f.name.trim(),
        contactPhone: f.phone.trim(),
      });
      if (data.role === "PENDING") { setDone(true); }
      else { onLogin(data.token, data.profile); }
    } catch (e) { setError(e.message); }
    finally     { setLoading(false); }
  };

  return (
    <div className="auth-page">
      <div className="auth-left">
        <div className="auth-brand">
          <div className="auth-brand-icon">🎓</div>
          <div className="auth-brand-name">E-<span>Counsellor</span></div>
        </div>
        <div className="auth-hero-title">Register Your College</div>
        <div className="auth-hero-sub">
          Get access to your college's counselling intelligence dashboard.
          One account per DTE college code.
        </div>
        <div className="auth-features">
          {[
            ["🏫", "One account per DTE code",   "Linked directly to your DTE registration"],
            ["🔒", "Secure JWT authentication",  "Your data stays private — only you can see it"],
            ["✅", "Admin verification",         "Accounts activated within 24 hours"],
            ["📞", "Contact person details",     "HOD / Principal for admin verification"],
          ].map(([icon, label, desc]) => (
            <div className="auth-feature" key={label}>
              <div className="auth-feature-icon">{icon}</div>
              <div className="auth-feature-text">
                <div className="auth-feature-label">{label}</div>
                <div className="auth-feature-desc">{desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="auth-right">
        <div className="auth-form-wrap">
          <div className="auth-form-title">Create Account</div>
          <div className="auth-form-sub">Enter your DTE details to register</div>

          {error && <Alert type="error">{error}</Alert>}
          {done  && (
            <Alert type="success">
              Registration submitted! Pending admin approval.
              <div style={{ marginTop: ".5rem" }}>
                <button className="link-btn" onClick={onGoLogin}>← Back to Login</button>
              </div>
            </Alert>
          )}

          {!done && (
            <>
              <div className="form-group">
                <label className="form-label">DTE College Code</label>
                <input className="form-input mono" placeholder="e.g. 06155"
                       value={f.collegeCode} onChange={e => set("collegeCode", e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">Official Email</label>
                <input className="form-input" type="email" placeholder="principal@college.ac.in"
                       value={f.email} onChange={e => set("email", e.target.value)} />
              </div>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Password</label>
                  <input className="form-input" type="password" placeholder="Min 6 characters"
                         value={f.password} onChange={e => set("password", e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="form-label">Confirm</label>
                  <input className="form-input" type="password" placeholder="Repeat"
                         value={f.confirm} onChange={e => set("confirm", e.target.value)} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">
                  Contact Person
                  <span style={{ fontWeight: 400, textTransform: "none", fontSize: ".72rem", color: "var(--text-muted)", marginLeft: ".35rem" }}>
                    (HOD / Principal)
                  </span>
                </label>
                <input className="form-input" placeholder="Dr. Ramesh Patil"
                       value={f.name} onChange={e => set("name", e.target.value)} />
              </div>
              <div className="form-group">
                <label className="form-label">
                  Contact Phone
                  <span style={{ fontWeight: 400, textTransform: "none", fontSize: ".72rem", color: "var(--text-muted)", marginLeft: ".35rem" }}>
                    (optional)
                  </span>
                </label>
                <input className="form-input mono" type="tel" placeholder="10-digit number"
                       value={f.phone} onChange={e => set("phone", e.target.value)} />
              </div>
              <button className="btn btn-primary" onClick={submit} disabled={loading}
                      style={{ marginTop: ".375rem" }}>
                {loading ? "Registering…" : "Register College →"}
              </button>
              <div className="divider">or</div>
              <div style={{ textAlign: "center", fontSize: ".875rem", color: "var(--text-muted)" }}>
                Already registered?{" "}
                <button className="link-btn" onClick={onGoLogin}>Login here</button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// TOPBAR + SIDEBAR
// ══════════════════════════════════════════════════════════════════════════════
function Topbar({ profile, onNav, onLogout }) {
  return (
    <header className="topbar">
      <div className="topbar-logo">
        <div className="topbar-logo-icon">🎓</div>
        <div className="topbar-brand-name">E-<span>Counsellor</span></div>
      </div>
      <div className="topbar-sep" />
      <div className="topbar-college">
        <div className="topbar-college-name">{profile?.collegeName || "College Portal"}</div>
        <div className="topbar-college-code">{profile?.collegeCode}</div>
      </div>
      <div className="topbar-right">
        <span className="topbar-pill">{profile?.collegeCode}</span>
        <button className="topbar-avatar-btn" onClick={() => onNav("profile")}
                title={profile?.contactPersonName || "Profile"}>
          {(profile?.collegeName || profile?.collegeCode || "C")[0].toUpperCase()}
        </button>
        <button className="topbar-logout-btn" onClick={onLogout}>Logout</button>
      </div>
    </header>
  );
}

const NAV = [
  { id: "interested",     icon: "👥", label: "Interested Students" },
  { id: "target-pool",    icon: "🎯", label: "Students to Target" },
  { id: "target-ranges",  icon: "📊", label: "Cutoff Target Ranges" },
  { id: "cutoff-history", icon: "📈", label: "Cutoff History + ML" },
  { id: "profile",        icon: "🏫", label: "College Profile" },
];

function Sidebar({ active, onNav }) {
  return (
    <nav className="sidebar">
      <div className="sidebar-section">Dashboard</div>
      {NAV.map(n => (
        <div key={n.id}
             className={`nav-item ${active === n.id ? "active" : ""}`}
             onClick={() => onNav(n.id)}>
          <span className="nav-icon">{n.icon}</span>
          {n.label}
        </div>
      ))}
    </nav>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// PAGE: INTERESTED STUDENTS
// ══════════════════════════════════════════════════════════════════════════════
function InterestedPage({ code, token }) {
  const { data, loading, error, refresh } = useFetch(code ? `${API_BASE}/${code}/interested` : null, token);
  const [activeBranch, setActiveBranch] = useState("ALL");

  if (loading) return <Spinner />;
  if (error)   return <><Alert type="error">{error}</Alert><div style={{marginTop:"1rem"}}><RefreshBtn onClick={refresh} loading={loading} /></div></>;
  if (!data)   return null;

  const branches   = data.byBranch        || [];
  const bands      = data.percentileBands || [];
  const categories = data.byCategory      || [];
  const convRate   = data.totalViews > 0
    ? `${(data.totalShortlists / data.totalViews * 100).toFixed(1)}%` : "—";

  const filtered = activeBranch === "ALL"
    ? branches : branches.filter(b => b.courseCode === activeBranch);

  return (
    <>
      <div style={{display:"flex",justifyContent:"flex-end",marginBottom:".75rem"}}>
        <RefreshBtn onClick={refresh} loading={loading} />
      </div>
      <div className="kpi-grid">
        <div className="kpi">
          <div className="kpi-label">Total Views</div>
          <div className="kpi-value">{(data.totalViews || 0).toLocaleString()}</div>
          <div className="kpi-sub">College page views from student app</div>
        </div>
        <div className="kpi accent">
          <div className="kpi-label">Shortlisted</div>
          <div className="kpi-value">{(data.totalShortlists || 0).toLocaleString()}</div>
          <div className="kpi-sub">Students saved your college ⭐</div>
        </div>
        <div className="kpi success">
          <div className="kpi-label">Conversion</div>
          <div className="kpi-value">{convRate}</div>
          <div className="kpi-sub">Views that became shortlists</div>
        </div>
        <div className="kpi muted">
          <div className="kpi-label">Active Branches</div>
          <div className="kpi-value">{branches.length}</div>
          <div className="kpi-sub">Branches with student interest</div>
        </div>
      </div>

      <div className="two-col">
        <div className="card">
          <div className="card-header">
            <div className="card-title">Percentile Band Distribution</div>
            <Badge type="muted">from views</Badge>
          </div>
          <div className="card-body">
            {bands.length === 0
              ? <div className="empty-state"><div className="empty-state-icon">📭</div>No view data yet</div>
              : <BarChart items={bands.map(b => ({ label: b.band, value: b.count }))} />}
          </div>
        </div>
        <div className="card">
          <div className="card-header">
            <div className="card-title">Category Breakdown</div>
            <Badge type="muted">all views</Badge>
          </div>
          <div className="card-body">
            {categories.length === 0
              ? <div className="empty-state"><div className="empty-state-icon">📭</div>No data yet</div>
              : <BarChart items={categories.map(c => ({ label: c.category, value: c.count }))} colorClass="accent" />}
          </div>
        </div>
      </div>

      {branches.length > 0 && (
        <div className="card">
          <div className="card-header">
            <div className="card-title">Branch-wise Shortlists</div>
            <Badge type="primary">{branches.length} branches</Badge>
          </div>
          <div className="card-body">
            <div className="pill-tabs">
              <div className={`pill-tab ${activeBranch === "ALL" ? "active" : ""}`}
                   onClick={() => setActiveBranch("ALL")}>All</div>
              {branches.map(b => (
                <div key={b.courseCode}
                     className={`pill-tab ${activeBranch === b.courseCode ? "active" : ""}`}
                     onClick={() => setActiveBranch(b.courseCode)}>
                  {(b.courseName || b.courseCode)?.split(" ").slice(0, 2).join(" ")}
                </div>
              ))}
            </div>
            <div style={{ overflowX: "auto" }}>
              <table className="tbl">
                <thead>
                  <tr>
                    <th>Branch</th>
                    <th>Shortlists</th>
                    <th>Views</th>
                    <th>Conversion</th>
                    <th>Top Category</th>
                    <th>All Categories</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(b => (
                    <tr key={b.courseCode}>
                      <td>
                        <div className="branch-name">{b.courseName}</div>
                        <div className="branch-code">{b.courseCode}</div>
                      </td>
                      <td className="mono">{(b.shortlists || 0).toLocaleString()}</td>
                      <td className="mono">{(b.views || 0).toLocaleString()}</td>
                      <td>
                        <Badge type={b.conversionRate > 30 ? "success" : b.conversionRate > 15 ? "warning" : "muted"}>
                          {b.conversionRate}%
                        </Badge>
                      </td>
                      <td>
                        {b.byCategory?.[0]
                          ? <><span style={{ fontWeight: 700 }}>{b.byCategory[0].category}</span>
                              <span className="text-muted"> ({b.byCategory[0].count})</span></>
                          : "—"}
                      </td>
                      <td>
                        <div className="flex flex-wrap gap-1">
                          {(b.byCategory || []).map(c => (
                            <span key={c.category} className="cat-chip">{c.category}: {c.count}</span>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// PAGE: TARGET POOL
// ══════════════════════════════════════════════════════════════════════════════
const CAP_CODES = ["GOPENH","GOBCS","GOSC","GOST","GONT1S","GONT2S","GONT3S","LOPEN","EWS","TFWS"];

function TargetPoolPage({ code, token }) {
  const [form, setForm] = useState({ courseCode: "", cap: "GOPENH", round: 4 });
  const [url,  setUrl]  = useState(null);
  const { data, loading, error, refresh } = useFetch(url, token);
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const analyze = () => {
    if (!form.courseCode.trim()) return;
    setUrl(`${API_BASE}/${code}/target-pool?courseCode=${form.courseCode.trim()}&capCategoryCode=${form.cap}&round=${form.round}`);
  };

  return (
    <>
      <div className="card">
        <div className="card-header">
          <div className="card-title">🎯 Analyze Student Pool for a Branch</div>
        </div>
        <div className="card-body">
          <div className="filter-row">
            <div className="filter-group">
              <div className="filter-label">Course Code</div>
              <input className="form-input mono" style={{ width: 140 }} placeholder="e.g. 101"
                     value={form.courseCode} onChange={e => set("courseCode", e.target.value)}
                     onKeyDown={e => e.key === "Enter" && analyze()} />
            </div>
            <div className="filter-group">
              <div className="filter-label">CAP Category</div>
              <select className="form-select" style={{ width: 148 }}
                      value={form.cap} onChange={e => set("cap", e.target.value)}>
                {CAP_CODES.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div className="filter-group">
              <div className="filter-label">Round</div>
              <select className="form-select" style={{ width: 120 }}
                      value={form.round} onChange={e => set("round", Number(e.target.value))}>
                {[1,2,3,4].map(r => <option key={r} value={r}>Round {r}</option>)}
              </select>
            </div>
            <div className="filter-group">
              <div className="filter-label">&nbsp;</div>
              <button className="btn btn-accent" onClick={analyze} disabled={!form.courseCode.trim()}>
                Analyze Pool
              </button>
            </div>
            {data && (
              <div className="filter-group">
                <div className="filter-label">&nbsp;</div>
                <RefreshBtn onClick={refresh} loading={loading} />
              </div>
            )}
          </div>
        </div>
      </div>

      {loading && <Spinner />}
      {error   && <Alert type="error">{error}</Alert>}

      {data && (
        <>
          <div className="target-banner">
            <div>
              <div className="target-label">Recommended Percentile Range to Target</div>
              <div className="range-pill">{data.targetMin} – {data.targetMax}</div>
              <div className="target-note">{data.note}</div>
            </div>
            <div style={{ textAlign: "right" }}>
              <div className="target-label">{data.capCategoryCode}</div>
              <div className="mono" style={{ color: "#FFD54F", fontSize: "1rem", fontWeight: 700, marginTop: ".25rem" }}>
                Round {form.round}
              </div>
            </div>
          </div>

          <div className="pool-grid">
            <div className="pool-card">
              <div className="pool-num">{(data.estimatedEligibleInApp || 0).toLocaleString()}</div>
              <div className="pool-lbl">Eligible in App</div>
              <div className="pool-sub">Students using the app with matching percentile + category</div>
            </div>
            <div className="pool-card blue">
              <div className="pool-num blue">{(data.alreadyShortlistedUs || 0).toLocaleString()}</div>
              <div className="pool-lbl">Already Shortlisted You ⭐</div>
              <div className="pool-sub">Strong intent — already interested in your college</div>
            </div>
            <div className="pool-card orange">
              <div className="pool-num orange">{(data.notYetAware || 0).toLocaleString()}</div>
              <div className="pool-lbl">Not Yet Aware 🎯</div>
              <div className="pool-sub">Eligible students who haven't shortlisted you — outreach target</div>
            </div>
          </div>
        </>
      )}

      {!data && !loading && !error && (
        <div className="empty-state">
          <div className="empty-state-icon">🔍</div>
          Enter a course code above and click Analyze Pool
        </div>
      )}
    </>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// PAGE: TARGET RANGES
// ══════════════════════════════════════════════════════════════════════════════
function TargetRangesPage({ code, token }) {
  const [round,  setRound]  = useState(4);
  const [filter, setFilter] = useState("ALL");
  const { data, loading, error, refresh } = useFetch(
    code ? `${API_BASE}/${code}/target-ranges?round=${round}` : null, token
  );

  if (loading) return <Spinner />;
  if (error)   return <><Alert type="error">{error}</Alert><div style={{marginTop:"1rem"}}><RefreshBtn onClick={refresh} loading={loading} /></div></>;

  const branches = data?.branches || [];
  const cats     = ["ALL", ...new Set(branches.map(b => b.category).filter(Boolean))];
  const filtered = filter === "ALL" ? branches : branches.filter(b => b.category === filter);

  return (
    <>
      {data && (
        <div className="kpi-grid">
          <div className="kpi">
            <div className="kpi-label">College</div>
            <div className="kpi-value" style={{ fontSize: "1.1rem" }}>{data.collegeName}</div>
            <div className="kpi-sub">Round {data.round} analysis</div>
          </div>
          <div className="kpi accent">
            <div className="kpi-label">Branch-Category Combos</div>
            <div className="kpi-value">{branches.length}</div>
            <div className="kpi-sub">With historical cutoff data</div>
          </div>
          <div className="kpi success">
            <div className="kpi-label">Rising Branches</div>
            <div className="kpi-value">
              {branches.filter(b => (b.predictedCutoff || 0) > (b.lastRoundCutoff || 0) + .5).length}
            </div>
            <div className="kpi-sub">Trending upward cutoff</div>
          </div>
        </div>
      )}

      <div className="flex gap-3" style={{ alignItems: "flex-end", flexWrap: "wrap", marginBottom: "1.25rem" }}>
        <div className="filter-group">
          <div className="filter-label">Round</div>
          <select className="form-select" style={{ width: 130 }}
                  value={round} onChange={e => setRound(Number(e.target.value))}>
            {[1,2,3,4].map(r => <option key={r} value={r}>Round {r}</option>)}
          </select>
        </div>
        <div className="filter-group">
          <div className="filter-label">&nbsp;</div>
          <RefreshBtn onClick={refresh} loading={loading} />
        </div>
        <div className="pill-tabs" style={{ marginBottom: 0 }}>
          {cats.map(c => (
            <div key={c} className={`pill-tab ${filter === c ? "active" : ""}`}
                 onClick={() => setFilter(c)}>{c}</div>
          ))}
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="card-title">Branch-wise Target Ranges</div>
          {data && <Badge type="primary">Round {data.round}</Badge>}
        </div>
        <div style={{ overflowX: "auto" }}>
          <table className="tbl">
            <thead>
              <tr>
                <th>Branch</th>
                <th>Category</th>
                <th>Intake</th>
                <th>Last Cutoff</th>
                <th>Predicted</th>
                <th>Target Range</th>
                <th>Trend</th>
                <th>Already Interested</th>
              </tr>
            </thead>
            <tbody>
              {!data && <tr><td colSpan={8}><Spinner /></td></tr>}
              {data && filtered.length === 0 && (
                <tr><td colSpan={8}><div className="empty-state">No data for this filter</div></td></tr>
              )}
              {filtered.map((b, i) => {
                const trend = (b.predictedCutoff || 0) > (b.lastRoundCutoff || 0) + .5 ? "RISING"
                  : (b.predictedCutoff || 0) < (b.lastRoundCutoff || 0) - .5 ? "FALLING" : "STABLE";
                return (
                  <tr key={i}>
                    <td>
                      <div className="branch-name">{b.courseName}</div>
                      <div className="branch-code">{b.courseCode}</div>
                    </td>
                    <td>
                      <Badge type="primary">{b.category}</Badge>
                      <div className="text-muted mt-1" style={{ fontSize: ".7rem" }}>{b.gender}</div>
                    </td>
                    <td className="mono">{b.intake}</td>
                    <td className="mono text-muted">{b.lastRoundCutoff?.toFixed(2) || "—"}</td>
                    <td>
                      <div className="predicted-val">{b.predictedCutoff?.toFixed(2) || "—"}</div>
                      {b.predictionConfidence && (
                        <div className="mt-1">
                          <Badge type={b.predictionConfidence === "HIGH" ? "success" : b.predictionConfidence === "MEDIUM" ? "warning" : "muted"}>
                            {b.predictionConfidence}
                          </Badge>
                        </div>
                      )}
                    </td>
                    <td><span className="range-chip">{b.targetMin} – {b.targetMax}</span></td>
                    <td><TrendBadge value={trend} /></td>
                    <td className="mono">{b.alreadyShortlisted || 0}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// PAGE: CUTOFF HISTORY
// ══════════════════════════════════════════════════════════════════════════════
function CutoffHistoryPage({ code, token }) {
  const { data, loading, error, refresh } = useFetch(code ? `${API_BASE}/${code}/cutoff-history` : null, token);
  const [activeBranch, setActiveBranch] = useState(null);
  const [catFilter,    setCatFilter]    = useState("ALL");

  if (loading) return <Spinner />;
  if (error)   return <><Alert type="error">{error}</Alert><div style={{marginTop:"1rem"}}><RefreshBtn onClick={refresh} loading={loading} /></div></>;
  if (!data)   return null;

  const branches      = data.branches || [];
  const displayBranch = activeBranch  || branches[0];
  const allCats       = ["ALL", ...new Set((displayBranch?.byCategory || []).map(c => c.capCategoryCode))];
  const filteredCats  = catFilter === "ALL"
    ? (displayBranch?.byCategory || [])
    : (displayBranch?.byCategory || []).filter(c => c.capCategoryCode === catFilter);
  const allRounds     = displayBranch?.byCategory
    ? [...new Set(displayBranch.byCategory.flatMap(c => (c.roundHistory || []).map(r => r.round)))].sort()
    : [];

  return (
    <>
      <div style={{display:"flex",justifyContent:"flex-end",marginBottom:".75rem"}}>
        <RefreshBtn onClick={refresh} loading={loading} />
      </div>
      <div className="kpi-grid">
        <div className="kpi">
          <div className="kpi-label">College</div>
          <div className="kpi-value" style={{ fontSize: "1.1rem" }}>{data.collegeName}</div>
          <div className="kpi-sub">{branches.length} branches tracked</div>
        </div>
        <div className="kpi accent">
          <div className="kpi-label">Branches</div>
          <div className="kpi-value">{branches.length}</div>
          <div className="kpi-sub">With cutoff history</div>
        </div>
        <div className="kpi success">
          <div className="kpi-label">Total Seats</div>
          <div className="kpi-value">{branches.reduce((s, b) => s + (b.intake || 0), 0)}</div>
          <div className="kpi-sub">Across all branches</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="card-title">Select Branch</div>
          <Badge type="primary">{branches.length} branches</Badge>
        </div>
        <div className="card-body">
          <div className="pill-tabs">
            {branches.map(b => (
              <div key={b.courseCode}
                   className={`pill-tab ${(activeBranch?.courseCode || branches[0]?.courseCode) === b.courseCode ? "active" : ""}`}
                   onClick={() => { setActiveBranch(b); setCatFilter("ALL"); }}>
                {(b.courseName || b.courseCode)?.split(" ").slice(0, 2).join(" ")}
              </div>
            ))}
          </div>
        </div>
      </div>

      {displayBranch && (
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">{displayBranch.courseName}</div>
              <div className="text-muted" style={{ fontSize: ".78rem", marginTop: 2 }}>
                {displayBranch.intake} seats · Code: {displayBranch.courseCode}
              </div>
            </div>
          </div>
          <div className="card-body">
            <div className="pill-tabs">
              {allCats.map(c => (
                <div key={c} className={`pill-tab ${catFilter === c ? "active" : ""}`}
                     onClick={() => setCatFilter(c)}>{c}</div>
              ))}
            </div>
            <div style={{ overflowX: "auto" }}>
              <table className="tbl">
                <thead>
                  <tr>
                    <th>CAP Category</th>
                    <th>Gender</th>
                    {allRounds.map(r => <th key={r}>Round {r}</th>)}
                    <th>Predicted</th>
                    <th>Trend</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredCats.length === 0 && (
                    <tr><td colSpan={allRounds.length + 4}>
                      <div className="empty-state">No data for this category</div>
                    </td></tr>
                  )}
                  {filteredCats.map((cat, i) => {
                    const rm = {};
                    (cat.roundHistory || []).forEach(r => { rm[r.round] = r.cutoffPercentile; });
                    return (
                      <tr key={i}>
                        <td><Badge type="primary">{cat.capCategoryCode}</Badge></td>
                        <td className="text-muted" style={{ fontSize: ".82rem" }}>{cat.gender || "—"}</td>
                        {allRounds.map(r => (
                          <td key={r} className="mono">
                            {rm[r] != null ? <span className="fw-700">{rm[r].toFixed(2)}</span>
                                           : <span className="text-muted">—</span>}
                          </td>
                        ))}
                        <td>
                          {cat.predictedNextCutoff != null
                            ? <span className="predicted-val">{cat.predictedNextCutoff.toFixed(2)}</span>
                            : "—"}
                        </td>
                        <td><TrendBadge value={cat.trend} /></td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// PAGE: PROFILE
// ══════════════════════════════════════════════════════════════════════════════
function ProfilePage({ profile, token, onUpdate, onLogout }) {
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({
    contactPersonName: profile?.contactPersonName || "",
    contactPhone:      profile?.contactPhone      || "",
    currentPassword:   "",
    newPassword:       "",
  });
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState("");
  const [success, setSuccess] = useState("");
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const save = async () => {
    setError(""); setSuccess("");
    setLoading(true);
    try {
      const updated = await apiPut(`${AUTH_BASE}/me`, {
        contactPersonName: form.contactPersonName || undefined,
        contactPhone:      form.contactPhone      || undefined,
        currentPassword:   form.currentPassword   || undefined,
        newPassword:       form.newPassword       || undefined,
      }, token);
      onUpdate(updated);
      setSuccess("Profile updated successfully!");
      setEditing(false);
      setForm(f => ({ ...f, currentPassword: "", newPassword: "" }));
    } catch (e) { setError(e.message); }
    finally     { setLoading(false); }
  };

  const p = profile || {};

  return (
    <>
      <div className="page-header">
        <div className="page-title">College Profile</div>
        <div className="page-sub">Your college account and DTE registration details</div>
      </div>

      <div className="profile-hero">
        <div className="profile-avatar">
          {(p.collegeName || p.collegeCode || "C")[0].toUpperCase()}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div className="profile-college-name">{p.collegeName || p.collegeCode}</div>
          <div className="profile-meta">
            {p.district    && <div className="profile-meta-item">📍 {p.district}</div>}
            {p.university  && <div className="profile-meta-item">🏛 {p.university}</div>}
            {p.fundingType && <div className="profile-meta-item">💼 {p.fundingType}</div>}
          </div>
        </div>
        <div className="profile-actions">
          <button className="btn btn-danger btn-sm" onClick={onLogout}>🚪 Logout</button>
        </div>
      </div>

      {success && <Alert type="success">{success}</Alert>}
      {error   && <Alert type="error">{error}</Alert>}

      <div className="two-col">
        <div className="card">
          <div className="card-header">
            <div className="card-title">🏫 College Information</div>
            <span className="mono text-primary" style={{ fontSize: ".78rem", fontWeight: 700 }}>{p.collegeCode}</span>
          </div>
          <div className="card-body">
            <div className="info-grid">
              {[
                ["College Name",  p.collegeName    || "—"],
                ["College Code",  p.collegeCode    || "—"],
                ["University",    p.university     || "—"],
                ["Funding Type",  p.fundingType    || "—"],
                ["District",      p.district       || "—"],
                ["Region",        p.region         || "—"],
                ["Total Intake",  p.totalIntake ? `${p.totalIntake} seats` : "—"],
                ["Minority",      p.minorityStatus || "Non-Minority"],
              ].map(([label, value]) => (
                <div className="info-row" key={label}>
                  <div className="info-label">{label}</div>
                  <div className="info-value">{value}</div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="card-header">
            <div className="card-title">👤 Account Details</div>
            {!editing && (
              <button className="btn btn-outline btn-sm"
                      onClick={() => { setEditing(true); setSuccess(""); setError(""); }}>
                ✏ Edit
              </button>
            )}
          </div>
          <div className="card-body">
            {!editing ? (
              <div className="info-grid">
                {[
                  ["Login Email",    p.email              || "—"],
                  ["Contact Person", p.contactPersonName  || "—"],
                  ["Contact Phone",  p.contactPhone       || "—"],
                  ["Status",         p.approved ? "✅ Approved" : "⏳ Pending Approval"],
                ].map(([label, value]) => (
                  <div className="info-row" key={label}>
                    <div className="info-label">{label}</div>
                    <div className="info-value">{value}</div>
                  </div>
                ))}
              </div>
            ) : (
              <>
                <div className="form-group">
                  <label className="form-label">Contact Person Name</label>
                  <input className="form-input" value={form.contactPersonName}
                         onChange={e => set("contactPersonName", e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="form-label">Contact Phone</label>
                  <input className="form-input mono" type="tel" value={form.contactPhone}
                         onChange={e => set("contactPhone", e.target.value)} />
                </div>
                <div style={{ borderTop: "1px solid var(--border)", paddingTop: "1rem", marginTop: ".5rem" }}>
                  <div style={{ fontSize: ".7rem", fontWeight: 700, textTransform: "uppercase", letterSpacing: ".5px", color: "var(--text-muted)", marginBottom: ".75rem" }}>
                    Change Password (optional)
                  </div>
                  <div className="form-group">
                    <label className="form-label">Current Password</label>
                    <input className="form-input" type="password" value={form.currentPassword}
                           onChange={e => set("currentPassword", e.target.value)} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">New Password</label>
                    <input className="form-input" type="password" value={form.newPassword}
                           onChange={e => set("newPassword", e.target.value)} />
                  </div>
                </div>
                <div className="flex gap-2 mt-2">
                  <button className="btn btn-primary" style={{ flex: 1 }} onClick={save} disabled={loading}>
                    {loading ? "Saving…" : "Save Changes"}
                  </button>
                  <button className="btn btn-outline" onClick={() => { setEditing(false); setError(""); }}>
                    Cancel
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ROOT APP
// ══════════════════════════════════════════════════════════════════════════════
const PAGE_SUBTITLES = {
  "interested":     "See which students from the app viewed and shortlisted your college.",
  "target-pool":    "Find how many eligible students haven't discovered your college yet.",
  "target-ranges":  "Data-driven percentile ranges to target per branch and category.",
  "cutoff-history": "Historical cutoffs per branch and category, with ML trend predictions.",
};

export default function App() {
  const { token, profile, saveSession, logout, updateProfile } = useAuth();
  const [page,     setPage]     = useState("interested");
  const [authView, setAuthView] = useState("login");

  if (!token || !profile) {
    return authView === "register"
      ? <RegisterPage onLogin={saveSession} onGoLogin={() => setAuthView("login")} />
      : <LoginPage    onLogin={saveSession} onGoRegister={() => setAuthView("register")} />;
  }

  const currentNav = NAV.find(n => n.id === page);
  const code       = profile.collegeCode;

  return (
    <div className="app-shell">
      <Topbar profile={profile} onNav={setPage} onLogout={logout} />
      <div className="body-layout">
        <Sidebar active={page} onNav={setPage} />
        <main className="main">
          {page !== "profile" && (
            <div className="page-header">
              <div className="page-title">{currentNav?.label}</div>
              <div className="page-sub">{PAGE_SUBTITLES[page]}</div>
            </div>
          )}
          {page === "interested"     && <InterestedPage    code={code} token={token} />}
          {page === "target-pool"    && <TargetPoolPage    code={code} token={token} />}
          {page === "target-ranges"  && <TargetRangesPage  code={code} token={token} />}
          {page === "cutoff-history" && <CutoffHistoryPage code={code} token={token} />}
          {page === "profile"        && (
            <ProfilePage
              profile={profile}
              token={token}
              onUpdate={updateProfile}
              onLogout={logout}
            />
          )}
        </main>
      </div>
    </div>
  );
}