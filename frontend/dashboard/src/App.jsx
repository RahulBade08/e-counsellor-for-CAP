import { useState, useEffect, useCallback } from "react";

// ── Config ────────────────────────────────────────────────────────────────────
// Change this to your backend URL
const API_BASE = "http://localhost:8080/api/counselling";

// ── Color palette: deep navy + saffron — professional, Maharashtra-rooted ────
const STYLES = `
  @import url('https://fonts.googleapis.com/css2?family=DM+Serif+Display:ital@0;1&family=DM+Sans:wght@300;400;500;600&family=DM+Mono&display=swap');

  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

  :root {
    --navy:     #0B1D3A;
    --navy-mid: #142852;
    --navy-lt:  #1E3A6A;
    --saffron:  #E8860A;
    --saffron-lt: #F5A334;
    --gold:     #FFCA5A;
    --cream:    #FAF7F2;
    --white:    #FFFFFF;
    --gray-100: #F3F4F6;
    --gray-200: #E5E7EB;
    --gray-400: #9CA3AF;
    --gray-600: #4B5563;
    --gray-800: #1F2937;
    --green:    #059669;
    --red:      #DC2626;
    --blue:     #2563EB;
    --rising:   #059669;
    --falling:  #DC2626;
    --stable:   #D97706;
    --safe:     #059669;
    --moderate: #D97706;
    --risky:    #DC2626;
  }

  body { font-family: 'DM Sans', sans-serif; background: var(--cream); color: var(--navy); }

  /* Layout */
  .app { min-height: 100vh; display: flex; flex-direction: column; }
  .header {
    background: var(--navy); color: white; padding: 0 2rem;
    display: flex; align-items: center; justify-content: space-between;
    height: 60px; position: sticky; top: 0; z-index: 100;
    box-shadow: 0 2px 20px rgba(0,0,0,0.3);
  }
  .header-logo { font-family: 'DM Serif Display', serif; font-size: 1.4rem; color: var(--gold); letter-spacing: -0.5px; }
  .header-sub  { font-size: 0.75rem; color: var(--gray-400); margin-top: 1px; }
  .header-code { font-family: 'DM Mono'; font-size: 0.8rem; background: var(--navy-lt); padding: 4px 10px; border-radius: 6px; color: var(--saffron-lt); }

  .main-layout { display: flex; flex: 1; }
  .sidebar {
    width: 220px; background: var(--navy-mid); flex-shrink: 0;
    padding: 1.5rem 0; display: flex; flex-direction: column; gap: 0.25rem;
    position: sticky; top: 60px; height: calc(100vh - 60px); overflow-y: auto;
  }
  .sidebar-section { padding: 0.5rem 1.25rem; font-size: 0.65rem; font-weight: 600; text-transform: uppercase; letter-spacing: 1.5px; color: var(--gray-400); margin-top: 0.75rem; }
  .nav-item {
    display: flex; align-items: center; gap: 0.75rem;
    padding: 0.65rem 1.25rem; cursor: pointer; color: var(--gray-400);
    font-size: 0.875rem; font-weight: 500; transition: all 0.15s;
    border-left: 3px solid transparent;
  }
  .nav-item:hover { background: rgba(255,255,255,0.05); color: white; }
  .nav-item.active { background: rgba(232,134,10,0.12); color: var(--saffron-lt); border-left-color: var(--saffron); }
  .nav-icon { font-size: 1rem; width: 1.2rem; text-align: center; }

  .content { flex: 1; padding: 2rem; max-width: 1200px; overflow-x: hidden; }

  /* College picker */
  .college-bar {
    background: white; border-radius: 12px; padding: 1rem 1.5rem;
    display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem;
    box-shadow: 0 1px 8px rgba(0,0,0,0.06); flex-wrap: wrap;
  }
  .college-bar label { font-size: 0.8rem; font-weight: 600; color: var(--gray-600); white-space: nowrap; }
  .college-bar input {
    flex: 1; min-width: 200px; padding: 0.5rem 0.875rem; border: 1.5px solid var(--gray-200);
    border-radius: 8px; font-family: 'DM Mono'; font-size: 0.85rem; color: var(--navy);
    outline: none; transition: border 0.15s;
  }
  .college-bar input:focus { border-color: var(--saffron); }
  .college-bar select {
    padding: 0.5rem 0.875rem; border: 1.5px solid var(--gray-200);
    border-radius: 8px; font-family: 'DM Sans'; font-size: 0.85rem; color: var(--navy);
    outline: none; background: white; cursor: pointer;
  }
  .btn-load {
    background: var(--saffron); color: white; border: none; padding: 0.55rem 1.25rem;
    border-radius: 8px; font-weight: 600; font-size: 0.875rem; cursor: pointer;
    transition: background 0.15s; white-space: nowrap;
  }
  .btn-load:hover { background: var(--saffron-lt); }
  .btn-load:disabled { background: var(--gray-400); cursor: default; }

  /* Page title */
  .page-title { font-family: 'DM Serif Display', serif; font-size: 1.75rem; color: var(--navy); margin-bottom: 0.35rem; }
  .page-subtitle { font-size: 0.875rem; color: var(--gray-600); margin-bottom: 1.75rem; }

  /* KPI cards */
  .kpi-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; margin-bottom: 2rem; }
  .kpi-card {
    background: white; border-radius: 12px; padding: 1.25rem 1.5rem;
    box-shadow: 0 1px 8px rgba(0,0,0,0.06); border-left: 4px solid var(--saffron);
  }
  .kpi-card.blue  { border-left-color: var(--blue); }
  .kpi-card.green { border-left-color: var(--green); }
  .kpi-card.gold  { border-left-color: var(--gold); }
  .kpi-label { font-size: 0.75rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.8px; color: var(--gray-400); margin-bottom: 0.4rem; }
  .kpi-value { font-family: 'DM Serif Display', serif; font-size: 2rem; color: var(--navy); line-height: 1; }
  .kpi-sub   { font-size: 0.75rem; color: var(--gray-400); margin-top: 0.3rem; }

  /* Cards */
  .card { background: white; border-radius: 12px; box-shadow: 0 1px 8px rgba(0,0,0,0.06); margin-bottom: 1.5rem; overflow: hidden; }
  .card-header { padding: 1.1rem 1.5rem; border-bottom: 1px solid var(--gray-100); display: flex; align-items: center; justify-content: space-between; }
  .card-title { font-weight: 600; font-size: 0.95rem; color: var(--navy); }
  .card-body  { padding: 1.5rem; }

  /* Section divider */
  .section-label { font-size: 0.7rem; font-weight: 700; text-transform: uppercase; letter-spacing: 1.5px; color: var(--saffron); margin-bottom: 0.75rem; margin-top: 1.5rem; }

  /* Tables */
  .tbl { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
  .tbl th { padding: 0.6rem 1rem; background: var(--navy); color: white; text-align: left; font-size: 0.75rem; font-weight: 600; letter-spacing: 0.5px; }
  .tbl th:first-child { border-radius: 8px 0 0 0; }
  .tbl th:last-child  { border-radius: 0 8px 0 0; }
  .tbl td { padding: 0.7rem 1rem; border-bottom: 1px solid var(--gray-100); color: var(--gray-800); vertical-align: middle; }
  .tbl tr:last-child td { border-bottom: none; }
  .tbl tr:hover td { background: #FAFBFF; }
  .tbl td.mono { font-family: 'DM Mono'; font-size: 0.8rem; }

  /* Badges */
  .badge { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 99px; font-size: 0.72rem; font-weight: 600; }
  .badge-safe     { background: #D1FAE5; color: #065F46; }
  .badge-moderate { background: #FEF3C7; color: #92400E; }
  .badge-risky    { background: #FEE2E2; color: #991B1B; }
  .badge-rising   { background: #D1FAE5; color: #065F46; }
  .badge-falling  { background: #FEE2E2; color: #991B1B; }
  .badge-stable   { background: #FEF3C7; color: #92400E; }
  .badge-blue     { background: #DBEAFE; color: #1E40AF; }
  .badge-gray     { background: var(--gray-100); color: var(--gray-600); }

  /* Bar chart */
  .bar-chart { display: flex; flex-direction: column; gap: 0.6rem; }
  .bar-row { display: flex; align-items: center; gap: 0.75rem; }
  .bar-label { width: 70px; font-size: 0.78rem; color: var(--gray-600); text-align: right; flex-shrink: 0; }
  .bar-track { flex: 1; background: var(--gray-100); border-radius: 99px; height: 10px; overflow: hidden; }
  .bar-fill  { height: 100%; border-radius: 99px; background: var(--saffron); transition: width 0.6s cubic-bezier(.4,0,.2,1); }
  .bar-fill.blue  { background: var(--blue); }
  .bar-fill.green { background: var(--green); }
  .bar-count { width: 40px; font-size: 0.78rem; font-family: 'DM Mono'; color: var(--navy); font-weight: 600; }

  /* Target range banner */
  .target-banner {
    background: linear-gradient(135deg, var(--navy) 0%, var(--navy-lt) 100%);
    border-radius: 12px; padding: 1.25rem 1.5rem; color: white; margin-bottom: 1rem;
    display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap;
  }
  .target-range-pill {
    background: var(--saffron); color: white; font-family: 'DM Mono';
    font-size: 1.1rem; font-weight: 700; padding: 0.4rem 1rem; border-radius: 8px;
  }
  .target-rationale { font-size: 0.8rem; color: rgba(255,255,255,0.75); margin-top: 0.4rem; line-height: 1.5; }

  /* Cutoff history cell */
  .cutoff-cell { display: flex; flex-direction: column; gap: 2px; }
  .cutoff-val  { font-family: 'DM Mono'; font-size: 0.85rem; font-weight: 600; color: var(--navy); }
  .cutoff-rnd  { font-size: 0.68rem; color: var(--gray-400); }

  /* Empty/loading */
  .empty { padding: 3rem; text-align: center; color: var(--gray-400); font-size: 0.9rem; }
  .loading { padding: 2rem; text-align: center; }
  .spinner {
    width: 32px; height: 32px; border: 3px solid var(--gray-200);
    border-top-color: var(--saffron); border-radius: 50%;
    animation: spin 0.7s linear infinite; display: inline-block;
  }
  @keyframes spin { to { transform: rotate(360deg); } }
  .error-msg { background: #FEE2E2; color: #991B1B; border-radius: 8px; padding: 0.75rem 1rem; font-size: 0.85rem; margin-bottom: 1rem; }

  /* Pill filter tabs */
  .pill-tabs { display: flex; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 1rem; }
  .pill-tab  { padding: 0.35rem 0.875rem; border-radius: 99px; font-size: 0.8rem; font-weight: 500; cursor: pointer; border: 1.5px solid var(--gray-200); background: white; color: var(--gray-600); transition: all 0.15s; }
  .pill-tab.active { border-color: var(--saffron); background: var(--saffron); color: white; }

  /* Pool card */
  .pool-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1rem; }
  @media (max-width: 700px) { .pool-grid { grid-template-columns: 1fr; } }
  .pool-card { background: white; border-radius: 12px; padding: 1.25rem; box-shadow: 0 1px 6px rgba(0,0,0,0.07); text-align: center; }
  .pool-num  { font-family: 'DM Serif Display', serif; font-size: 2.5rem; color: var(--navy); }
  .pool-lbl  { font-size: 0.75rem; color: var(--gray-500); font-weight: 500; margin-top: 0.25rem; }
  .pool-sub  { font-size: 0.7rem; color: var(--gray-400); margin-top: 0.5rem; }
`;

// ── Data fetching hook ────────────────────────────────────────────────────────
function useFetch(url) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetch_ = useCallback(async () => {
    if (!url) return;
    setLoading(true); setError(null); setData(null);
    try {
      const res = await fetch(url);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setData(await res.json());
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, [url]);

  useEffect(() => { fetch_(); }, [fetch_]);
  return { data, loading, error, refetch: fetch_ };
}

// ── Sub-components ────────────────────────────────────────────────────────────

function Spinner() { return <div className="loading"><div className="spinner" /></div>; }

function BarChart({ items, maxVal, colorClass }) {
  return (
    <div className="bar-chart">
      {items.map(({ label, value }) => (
        <div className="bar-row" key={label}>
          <div className="bar-label">{label}</div>
          <div className="bar-track">
            <div className="bar-fill" style={{ width: `${maxVal ? (value / maxVal) * 100 : 0}%` }} />
          </div>
          <div className="bar-count">{value.toLocaleString()}</div>
        </div>
      ))}
    </div>
  );
}

function Badge({ text, type }) {
  const cls = { SAFE:"badge-safe", MODERATE:"badge-moderate", RISKY:"badge-risky",
                RISING:"badge-rising", FALLING:"badge-falling", STABLE:"badge-stable" }[text] || "badge-gray";
  return <span className={`badge ${cls}`}>{text}</span>;
}

// ═════════════════════════════════════════════════════════════════════════════
// PAGE 1: INTERESTED STUDENTS
// ═════════════════════════════════════════════════════════════════════════════
function InterestedPage({ collegeCode }) {
  const { data, loading, error } = useFetch(
    collegeCode ? `${API_BASE}/${collegeCode}/interested` : null
  );
  const [activeBranch, setActiveBranch] = useState("ALL");

  if (!collegeCode) return <div className="empty">Enter a college code above and click Load.</div>;
  if (loading) return <Spinner />;
  if (error)   return <div className="error-msg">⚠ {error} — Is the backend running?</div>;
  if (!data)   return null;

  const branches = data.byBranch || [];
  const maxShortlist = Math.max(...branches.map(b => b.shortlists), 1);
  const maxViews     = Math.max(...branches.map(b => b.views), 1);
  const bands        = data.percentileBands || [];
  const maxBand      = Math.max(...bands.map(b => b.count), 1);

  const filteredBranch = activeBranch === "ALL"
    ? null
    : branches.find(b => b.courseCode === activeBranch);

  return (
    <>
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">Total Views</div>
          <div className="kpi-value">{(data.totalViews || 0).toLocaleString()}</div>
          <div className="kpi-sub">College page views</div>
        </div>
        <div className="kpi-card blue">
          <div className="kpi-label">Shortlisted</div>
          <div className="kpi-value">{(data.totalShortlists || 0).toLocaleString()}</div>
          <div className="kpi-sub">Students saved your college</div>
        </div>
        <div className="kpi-card green">
          <div className="kpi-label">Conversion</div>
          <div className="kpi-value">
            {data.totalViews > 0
              ? `${Math.round(data.totalShortlists / data.totalViews * 1000) / 10}%`
              : "—"}
          </div>
          <div className="kpi-sub">Views → shortlists</div>
        </div>
        <div className="kpi-card gold">
          <div className="kpi-label">Active Branches</div>
          <div className="kpi-value">{branches.length}</div>
          <div className="kpi-sub">Branches with interest</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <div className="card-title">Shortlists by Branch</div>
          <span className="badge badge-blue">{branches.length} branches</span>
        </div>
        <div className="card-body">
          {branches.length === 0
            ? <div className="empty" style={{padding:"1rem"}}>No shortlist data yet. Share the app with students!</div>
            : <BarChart items={branches.map(b => ({ label: b.courseName?.split(" ")[0] || b.courseCode, value: b.shortlists }))} maxVal={maxShortlist} />
          }
        </div>
      </div>

      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1.5rem" }}>
        <div className="card">
          <div className="card-header"><div className="card-title">Percentile Band Distribution</div></div>
          <div className="card-body">
            {bands.length === 0
              ? <div className="empty" style={{padding:"1rem"}}>No data yet</div>
              : <BarChart
                  items={bands.map(b => ({ label: b.band, value: b.count }))}
                  maxVal={maxBand} colorClass="blue"
                />
            }
          </div>
        </div>
        <div className="card">
          <div className="card-header"><div className="card-title">Category Breakdown</div></div>
          <div className="card-body">
            {(data.byCategory || []).length === 0
              ? <div className="empty" style={{padding:"1rem"}}>No data yet</div>
              : <BarChart
                  items={(data.byCategory || []).map(c => ({ label: c.category, value: c.count }))}
                  maxVal={Math.max(...(data.byCategory || []).map(c => c.count), 1)}
                  colorClass="green"
                />
            }
          </div>
        </div>
      </div>

      {branches.length > 0 && (
        <div className="card">
          <div className="card-header">
            <div className="card-title">Branch Detail — Category Breakdown</div>
          </div>
          <div className="card-body">
            <div className="pill-tabs">
              <div className={`pill-tab ${activeBranch === "ALL" ? "active" : ""}`}
                   onClick={() => setActiveBranch("ALL")}>All Branches</div>
              {branches.map(b => (
                <div key={b.courseCode}
                     className={`pill-tab ${activeBranch === b.courseCode ? "active" : ""}`}
                     onClick={() => setActiveBranch(b.courseCode)}>
                  {b.courseName?.split(" ")[0] || b.courseCode}
                </div>
              ))}
            </div>
            <table className="tbl">
              <thead><tr>
                <th>Branch</th><th>Shortlists</th><th>Views</th><th>Conversion</th><th>Top Category</th>
              </tr></thead>
              <tbody>
                {(activeBranch === "ALL" ? branches : branches.filter(b => b.courseCode === activeBranch))
                  .map(b => (
                    <tr key={b.courseCode}>
                      <td><div style={{fontWeight:600,color:"var(--navy)"}}>{b.courseName}</div>
                          <div style={{fontSize:"0.72rem",color:"var(--gray-400)",fontFamily:"'DM Mono'"}}>{b.courseCode}</div></td>
                      <td className="mono">{b.shortlists.toLocaleString()}</td>
                      <td className="mono">{b.views.toLocaleString()}</td>
                      <td><span className={`badge ${b.conversionRate > 30 ? "badge-safe" : b.conversionRate > 15 ? "badge-moderate" : "badge-gray"}`}>{b.conversionRate}%</span></td>
                      <td>{b.byCategory?.[0]
                        ? <><span style={{fontWeight:600}}>{b.byCategory[0].category}</span>
                            <span style={{color:"var(--gray-400)",marginLeft:"0.5rem",fontSize:"0.78rem"}}>({b.byCategory[0].count})</span></>
                        : "—"}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}

// ═════════════════════════════════════════════════════════════════════════════
// PAGE 2: TARGET POOL
// ═════════════════════════════════════════════════════════════════════════════
function TargetPoolPage({ collegeCode }) {
  const [courseCode, setCourseCode] = useState("");
  const [capCode, setCapCode]       = useState("GOPENH");
  const [round, setRound]           = useState(4);
  const [fetchUrl, setFetchUrl]     = useState(null);
  const { data, loading, error }    = useFetch(fetchUrl);

  if (!collegeCode) return <div className="empty">Enter a college code above and click Load.</div>;

  const capCodes = ["GOPENH","GOBCS","GOSC","GOST","GONT1S","GONT2S","GONT3S","LOPEN","EWS","TFWS"];

  return (
    <>
      <div className="card">
        <div className="card-header"><div className="card-title">Check Student Pool for a Branch</div></div>
        <div className="card-body">
          <div style={{ display:"flex", gap:"1rem", flexWrap:"wrap", alignItems:"flex-end" }}>
            <div>
              <div className="section-label">Course Code</div>
              <input style={{ padding:"0.5rem 0.875rem", border:"1.5px solid var(--gray-200)", borderRadius:"8px", fontFamily:"'DM Mono'", fontSize:"0.85rem", width:"140px", outline:"none" }}
                     placeholder="e.g. 101"
                     value={courseCode} onChange={e => setCourseCode(e.target.value)} />
            </div>
            <div>
              <div className="section-label">Category Code</div>
              <select style={{ padding:"0.5rem 0.875rem", border:"1.5px solid var(--gray-200)", borderRadius:"8px", fontFamily:"'DM Sans'", fontSize:"0.85rem", background:"white", outline:"none" }}
                      value={capCode} onChange={e => setCapCode(e.target.value)}>
                {capCodes.map(c => <option key={c} value={c}>{c}</option>)}
              </select>
            </div>
            <div>
              <div className="section-label">Round</div>
              <select style={{ padding:"0.5rem 0.875rem", border:"1.5px solid var(--gray-200)", borderRadius:"8px", fontFamily:"'DM Sans'", fontSize:"0.85rem", background:"white", outline:"none" }}
                      value={round} onChange={e => setRound(Number(e.target.value))}>
                {[1,2,3,4].map(r => <option key={r} value={r}>Round {r}</option>)}
              </select>
            </div>
            <button className="btn-load" style={{ marginBottom:"0" }}
                    onClick={() => setFetchUrl(`${API_BASE}/${collegeCode}/target-pool?courseCode=${courseCode}&capCategoryCode=${capCode}&round=${round}`)}>
              Analyze Pool
            </button>
          </div>
        </div>
      </div>

      {loading && <Spinner />}
      {error && <div className="error-msg">⚠ {error}</div>}
      {data && (
        <>
          <div className="target-banner">
            <div>
              <div style={{ fontSize:"0.7rem", color:"rgba(255,255,255,0.6)", textTransform:"uppercase", letterSpacing:"1px", marginBottom:"0.4rem" }}>Recommended Target Percentile Range</div>
              <div className="target-range-pill">{data.targetMin} – {data.targetMax}</div>
              <div className="target-rationale">{data.note}</div>
            </div>
            <div style={{ textAlign:"right" }}>
              <div style={{ fontSize:"0.7rem", color:"rgba(255,255,255,0.6)", marginBottom:"0.25rem" }}>{data.capCategoryCode}</div>
              <div style={{ fontFamily:"'DM Mono'", fontSize:"1.1rem", color:"var(--gold)" }}>Round {round}</div>
            </div>
          </div>
          <div className="pool-grid">
            <div className="pool-card">
              <div className="pool-num">{(data.estimatedEligibleInApp || 0).toLocaleString()}</div>
              <div className="pool-lbl">Eligible Students in App</div>
              <div className="pool-sub">Viewed any college in this percentile+category range</div>
            </div>
            <div className="pool-card" style={{ border:"2px solid var(--blue)" }}>
              <div className="pool-num" style={{ color:"var(--blue)" }}>{(data.alreadyShortlistedUs || 0).toLocaleString()}</div>
              <div className="pool-lbl">Already Shortlisted You</div>
              <div className="pool-sub">Strong intent — already showing interest</div>
            </div>
            <div className="pool-card" style={{ border:"2px solid var(--saffron)" }}>
              <div className="pool-num" style={{ color:"var(--saffron)" }}>{(data.notYetAware || 0).toLocaleString()}</div>
              <div className="pool-lbl">Not Yet Aware of You</div>
              <div className="pool-sub">Eligible but haven't shortlisted — your outreach target</div>
            </div>
          </div>
        </>
      )}
    </>
  );
}

// ═════════════════════════════════════════════════════════════════════════════
// PAGE 3: TARGET RANGES
// ═════════════════════════════════════════════════════════════════════════════
function TargetRangesPage({ collegeCode, round }) {
  const { data, loading, error } = useFetch(
    collegeCode ? `${API_BASE}/${collegeCode}/target-ranges?round=${round}` : null
  );
  const [filter, setFilter] = useState("ALL");

  if (!collegeCode) return <div className="empty">Enter a college code above and click Load.</div>;
  if (loading) return <Spinner />;
  if (error)   return <div className="error-msg">⚠ {error}</div>;
  if (!data)   return null;

  const branches = data.branches || [];
  const categories = ["ALL", ...new Set(branches.map(b => b.category).filter(Boolean))];
  const filtered = filter === "ALL" ? branches : branches.filter(b => b.category === filter);

  return (
    <>
      <div className="kpi-grid">
        <div className="kpi-card">
          <div className="kpi-label">College</div>
          <div className="kpi-value" style={{ fontSize:"1.2rem", lineHeight:"1.3" }}>{data.collegeName}</div>
          <div className="kpi-sub">Round {data.round} data</div>
        </div>
        <div className="kpi-card blue">
          <div className="kpi-label">Branch-Category combos</div>
          <div className="kpi-value">{branches.length}</div>
          <div className="kpi-sub">With historical cutoff data</div>
        </div>
        <div className="kpi-card green">
          <div className="kpi-label">Rising Branches</div>
          <div className="kpi-value">{branches.filter(b => b.predictionConfidence === "RISING" || (b.predictedCutoff > b.lastRoundCutoff + 0.5)).length}</div>
          <div className="kpi-sub">Cutoff trending up</div>
        </div>
      </div>

      <div className="pill-tabs">
        {categories.map(c => (
          <div key={c} className={`pill-tab ${filter === c ? "active" : ""}`} onClick={() => setFilter(c)}>{c}</div>
        ))}
      </div>

      <div className="card">
        <div className="card-header">
          <div className="card-title">Branch-wise Target Ranges</div>
          <span className="badge badge-blue">Round {data.round}</span>
        </div>
        <div className="card-body" style={{ padding: 0 }}>
          <table className="tbl">
            <thead><tr>
              <th>Branch</th><th>Category</th><th>Intake</th>
              <th>Last Cutoff</th><th>Predicted</th>
              <th>Target Range</th><th>Already Interested</th><th>Rationale</th>
            </tr></thead>
            <tbody>
              {filtered.length === 0
                ? <tr><td colSpan={8} style={{ textAlign:"center", padding:"2rem", color:"var(--gray-400)" }}>No data for this filter</td></tr>
                : filtered.map((b, i) => (
                  <tr key={i}>
                    <td>
                      <div style={{ fontWeight:600, color:"var(--navy)", fontSize:"0.82rem" }}>{b.courseName}</div>
                      <div style={{ fontFamily:"'DM Mono'", fontSize:"0.7rem", color:"var(--gray-400)" }}>{b.courseCode}</div>
                    </td>
                    <td><span className="badge badge-blue">{b.category}</span>
                        <div style={{ fontSize:"0.68rem", color:"var(--gray-400)", marginTop:"2px" }}>{b.gender}</div></td>
                    <td className="mono">{b.intake}</td>
                    <td className="mono" style={{ color:"var(--gray-600)" }}>{b.lastRoundCutoff?.toFixed(1)}</td>
                    <td>
                      <div className="mono" style={{ fontWeight:700 }}>{b.predictedCutoff?.toFixed(1)}</div>
                      <div style={{ marginTop:"2px" }}><Badge text={b.predictedCutoff > (b.lastRoundCutoff + 0.5) ? "RISING" : b.predictedCutoff < (b.lastRoundCutoff - 0.5) ? "FALLING" : "STABLE"} /></div>
                    </td>
                    <td>
                      <div style={{ background:"var(--navy)", color:"var(--gold)", fontFamily:"'DM Mono'", fontSize:"0.8rem", fontWeight:700, padding:"3px 8px", borderRadius:"6px", display:"inline-block" }}>
                        {b.targetMin} – {b.targetMax}
                      </div>
                    </td>
                    <td className="mono">{b.alreadyShortlisted || 0}</td>
                    <td style={{ fontSize:"0.72rem", color:"var(--gray-600)", maxWidth:"200px" }}>{b.rationale}</td>
                  </tr>
                ))
              }
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

// ═════════════════════════════════════════════════════════════════════════════
// PAGE 4: CUTOFF HISTORY
// ═════════════════════════════════════════════════════════════════════════════
function CutoffHistoryPage({ collegeCode }) {
  const { data, loading, error } = useFetch(
    collegeCode ? `${API_BASE}/${collegeCode}/cutoff-history` : null
  );
  const [activeBranch, setActiveBranch] = useState(null);
  const [catFilter, setCatFilter] = useState("ALL");

  if (!collegeCode) return <div className="empty">Enter a college code above and click Load.</div>;
  if (loading) return <Spinner />;
  if (error)   return <div className="error-msg">⚠ {error}</div>;
  if (!data)   return null;

  const branches = data.branches || [];
  const displayBranch = activeBranch || branches[0];
  const allCats = ["ALL", ...new Set((displayBranch?.byCategory || []).map(c => c.capCategoryCode))];
  const filteredCats = catFilter === "ALL"
    ? (displayBranch?.byCategory || [])
    : (displayBranch?.byCategory || []).filter(c => c.capCategoryCode === catFilter);

  // Get all rounds in the data for table header
  const allRounds = displayBranch?.byCategory
    ? [...new Set(displayBranch.byCategory.flatMap(c => (c.roundHistory || []).map(r => r.round)))].sort()
    : [];

  return (
    <>
      <div className="card">
        <div className="card-header">
          <div className="card-title">{data.collegeName}</div>
          <span className="badge badge-blue">{branches.length} branches</span>
        </div>
        <div className="card-body">
          <div className="section-label">Select Branch</div>
          <div className="pill-tabs">
            {branches.map(b => (
              <div key={b.courseCode}
                   className={`pill-tab ${(activeBranch?.courseCode || branches[0]?.courseCode) === b.courseCode ? "active" : ""}`}
                   onClick={() => { setActiveBranch(b); setCatFilter("ALL"); }}>
                {b.courseName?.split(" ").slice(0, 2).join(" ") || b.courseCode}
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
              <div style={{ fontSize:"0.78rem", color:"var(--gray-400)", marginTop:"2px" }}>
                Intake: {displayBranch.intake} seats · Code: {displayBranch.courseCode}
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

            <table className="tbl">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Gender</th>
                  {allRounds.map(r => <th key={r}>Round {r} Cutoff</th>)}
                  <th>Predicted Next</th>
                  <th>Trend</th>
                </tr>
              </thead>
              <tbody>
                {filteredCats.length === 0
                  ? <tr><td colSpan={allRounds.length + 4} style={{ textAlign:"center", padding:"2rem", color:"var(--gray-400)" }}>No data</td></tr>
                  : filteredCats.map((cat, i) => {
                    const roundMap = {};
                    (cat.roundHistory || []).forEach(r => { roundMap[r.round] = r.cutoffPercentile; });
                    return (
                      <tr key={i}>
                        <td>
                          <span className="badge badge-blue">{cat.capCategoryCode}</span>
                        </td>
                        <td style={{ fontSize:"0.8rem", color:"var(--gray-600)" }}>{cat.gender || "—"}</td>
                        {allRounds.map(r => (
                          <td key={r} className="mono">
                            {roundMap[r] != null ? roundMap[r].toFixed(1) : <span style={{color:"var(--gray-300)"}}>—</span>}
                          </td>
                        ))}
                        <td>
                          {cat.predictedNextCutoff != null
                            ? <span style={{ fontFamily:"'DM Mono'", fontWeight:700, color:"var(--saffron)" }}>{cat.predictedNextCutoff.toFixed(1)}</span>
                            : "—"}
                        </td>
                        <td>{cat.trend ? <Badge text={cat.trend} /> : "—"}</td>
                      </tr>
                    );
                  })
                }
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  );
}

// ═════════════════════════════════════════════════════════════════════════════
// MAIN APP
// ═════════════════════════════════════════════════════════════════════════════
export default function App() {
  const [page, setPage]           = useState("interested");
  const [inputCode, setInputCode] = useState("");
  const [collegeCode, setCollegeCode] = useState("");
  const [round, setRound]         = useState(4);

  const nav = [
    { id: "interested",    label: "Interested Students",  icon: "👥" },
    { id: "target-pool",   label: "Students to Target",   icon: "🎯" },
    { id: "target-ranges", label: "Cutoff Target Ranges", icon: "📊" },
    { id: "cutoff-history",label: "Cutoff History & ML",  icon: "📈" },
  ];

  const currentNav = nav.find(n => n.id === page);

  return (
    <>
      <style>{STYLES}</style>
      <div className="app">
        <header className="header">
          <div>
            <div className="header-logo">E-Counsellor</div>
            <div className="header-sub">College Counselling Dashboard</div>
          </div>
          {collegeCode && <div className="header-code">{collegeCode}</div>}
        </header>

        <div className="main-layout">
          <nav className="sidebar">
            <div className="sidebar-section">Counselling</div>
            {nav.map(n => (
              <div key={n.id}
                   className={`nav-item ${page === n.id ? "active" : ""}`}
                   onClick={() => setPage(n.id)}>
                <span className="nav-icon">{n.icon}</span>
                {n.label}
              </div>
            ))}
          </nav>

          <main className="content">
            {/* College picker bar */}
            <div className="college-bar">
              <label>College Code</label>
              <input
                placeholder="Enter DTE college code (e.g. 06155)"
                value={inputCode}
                onChange={e => setInputCode(e.target.value)}
                onKeyDown={e => e.key === "Enter" && setCollegeCode(inputCode.trim())}
              />
              {(page === "target-ranges") && (
                <>
                  <label>Round</label>
                  <select value={round} onChange={e => setRound(Number(e.target.value))}>
                    {[1,2,3,4].map(r => <option key={r} value={r}>Round {r}</option>)}
                  </select>
                </>
              )}
              <button className="btn-load" onClick={() => setCollegeCode(inputCode.trim())} disabled={!inputCode.trim()}>
                Load
              </button>
            </div>

            {/* Page heading */}
            <div className="page-title">{currentNav?.label}</div>
            <div className="page-subtitle">
              {{
                "interested":     "See which students viewed and shortlisted your college and branches.",
                "target-pool":    "Find how many eligible students haven't discovered you yet.",
                "target-ranges":  "See what percentile range to target for outreach, per branch.",
                "cutoff-history": "Historical cutoffs per branch and category, with ML prediction.",
              }[page]}
            </div>

            {page === "interested"     && <InterestedPage     collegeCode={collegeCode} />}
            {page === "target-pool"    && <TargetPoolPage     collegeCode={collegeCode} />}
            {page === "target-ranges"  && <TargetRangesPage   collegeCode={collegeCode} round={round} />}
            {page === "cutoff-history" && <CutoffHistoryPage  collegeCode={collegeCode} />}
          </main>
        </div>
      </div>
    </>
  );
}
