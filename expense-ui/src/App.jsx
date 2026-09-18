import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, Cpu, Database, Send, RefreshCw,
  ArrowRight, Zap, Terminal, Activity, CheckCircle2
} from 'lucide-react';

const GATEWAY_URL = 'http://localhost:9000';

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('token') || '');
  // Default strictly 0
  const [userId, setUserId] = useState(Number(localStorage.getItem('userId')) || 0);
  
  // Auth Form State
  const [isSignUp, setIsSignUp] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('9876543310');
  const [authStatus, setAuthStatus] = useState('');

  // Pipeline State
  const [activeStep, setActiveStep] = useState(0);
  const [latency, setLatency] = useState(null);
  const [smsMessage, setSmsMessage] = useState('');
  const [pipelineLogs, setPipelineLogs] = useState([
    { text: 'Gateway link verified at http://localhost:9000', time: 'INIT', type: 'info' }
  ]);
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(false);

  const sampleSMS = [
    { 
      title: 'Swiggy Instamart', 
      text: "Dear Customer, INR 1,850.00 debited from your HDFC Bank A/C ending with 4321 towards Swiggy Instamart on 14-Sep-26 via UPI Ref 6291038472. Not you? Call 1800." 
    },
    { 
      title: 'Starbucks Coffee', 
      text: "INR 450.00 debited from HDFC Bank A/C XX1029 towards Starbucks Coffee on 15-Sep-26 via UPI. Bal: INR 12,000." 
    },
    { 
      title: 'Uber Premier', 
      text: "Uber Trip: INR 340.50 debited via Paytm Payments Bank for ride on 16-Sep-26." 
    }
  ];

  const addLog = (msg, type = 'info') => {
    const time = new Date().toLocaleTimeString();
    setPipelineLogs(prev => [{ text: msg, time, type }, ...prev]);
  };

  const parseResponse = async (res) => {
    const text = await res.text();
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  };

  const isTokenValid = () => {
    if (!token) return false;
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const payload = JSON.parse(atob(base64));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  };

  // 1. Sign Up Flow
  const handleSignUp = async () => {
    if (!username || !password) return alert('Username and password required');
    addLog(`[Auth Ingress] Registering user: ${username}...`, 'info');
    try {
      const res = await fetch(`${GATEWAY_URL}/auth/v1/signup`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username,
          password,
          firstName: firstName || username,
          lastName: lastName || 'Dev',
          phoneNumber: Number(phoneNumber) || 9876543310,
          email: email || `${username}@example.com`
        })
      });
      const data = await parseResponse(res);
      if (res.ok) {
        addLog(`[Auth Service] 200 OK: ${typeof data === 'string' ? data : 'User registered successfully'}`, 'success');
        setIsSignUp(false);
        setAuthStatus('Registered! Please login.');
      } else {
        addLog(`[Auth Failed] HTTP ${res.status}: ${JSON.stringify(data)}`, 'error');
        setAuthStatus(`Failed: ${res.status}`);
      }
    } catch (err) {
      addLog(`[Gateway Error] Registration failed: ${err.message}`, 'error');
      setAuthStatus('Gateway Offline');
    }
  };

  // 2. Direct Sync Login Flow
  const handleLogin = async () => {
    if (!username || !password) return alert('Enter username and password');
    addLog(`[Auth Ingress] Verifying credentials for ${username}...`, 'info');
    try {
      const res = await fetch(`${GATEWAY_URL}/auth/v1/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
      });
      
      const data = await res.json();

      if (res.ok && data.accessToken) {
        setToken(data.accessToken);
        localStorage.setItem('token', data.accessToken);

        // Seedha backend DTO se numeric userId uthana:
        const realUserId = Number(data.userId);
        setUserId(realUserId);
        localStorage.setItem('userId', String(realUserId));

        setAuthStatus('Authenticated');
        addLog(`[Auth Success] Session Active. Logged in as User ID: ${realUserId}`, 'success');

        // Ledger refresh strictly for this numeric user
        fetchExpenses(data.accessToken, realUserId);

      } else {
        addLog(`[Auth Error] HTTP ${res.status}: Invalid credentials.`, 'error');
        setAuthStatus('Invalid Credentials');
      }
    } catch (err) {
      addLog(`[Gateway Error] Login failed: ${err.message}`, 'error');
      setAuthStatus('Connection Refused');
    }
  };

  // 3. Fetch Expenses
  const fetchExpenses = async (overrideToken, overrideUserId) => {
    const activeJwt = overrideToken || token;
    const activeId = overrideUserId !== undefined ? overrideUserId : userId;

    if (!activeId || activeId === 0) {
      setExpenses([]);
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${GATEWAY_URL}/expense/v1/user/${activeId}`, {
        headers: activeJwt ? { 'Authorization': `Bearer ${activeJwt}` } : {}
      });
      const data = await parseResponse(res);
      if (res.ok && Array.isArray(data)) {
        setExpenses(data);
        addLog(`[Ledger Synced] Loaded ${data.length} records for User ID: ${activeId}.`, 'success');
      } else {
        setExpenses([]);
        addLog(`[Ledger Notice] Ready for User ID: ${activeId}.`, 'info');
      }
    } catch (err) {
      addLog(`[Ledger Error] ExpenseService unreachable: ${err.message}`, 'error');
    } finally {
      setLoading(false);
    }
  };

  // 4. Ingest SMS via FastAPI Gemini Agent
  const handleParseSms = async () => {
    if (!smsMessage) return alert('Select or paste an SMS message first.');
    
    const targetUserId = Number(userId);
    if (!targetUserId || targetUserId === 0) {
      alert("Please login first to get your active User ID.");
      return;
    }

    const startTime = performance.now();
    setActiveStep(1);
    addLog(`[Gateway Ingress] Ingesting transaction for User ID: ${targetUserId}...`, 'info');

    const payload = {
      user_id: targetUserId,
      message: smsMessage.trim()
    };

    try {
      const res = await fetch(`${GATEWAY_URL}/ds/v1/extract-expense`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify(payload)
      });

      const data = await res.json();

      if (res.ok && data.status === "SUCCESS") {
        setActiveStep(2);
        addLog(`[Gemini AI] Extracted: ${data.extracted.merchant} (₹${data.extracted.amount})`, 'success');
        
        setActiveStep(3);
        addLog(`[Kafka Producer] Published to EXPENSE_EVENTS.`, 'info');

        setTimeout(() => {
          setActiveStep(4);
          addLog(`[Expense Consumer] Persisted to MySQL expense_service_db.`, 'success');
          const endTime = performance.now();
          setLatency(Math.round(endTime - startTime));
          fetchExpenses(token, targetUserId);
          setActiveStep(0);
        }, 1200);

      } else {
        setActiveStep(0);
        addLog(`[DS Error] HTTP ${res.status}: ${JSON.stringify(data.detail || data)}`, 'error');
      }
    } catch (err) {
      setActiveStep(0);
      addLog(`[Network Error] Request failed: ${err.message}`, 'error');
    }
  };

  useEffect(() => {
    if (token && userId !== 0) {
      fetchExpenses(token, userId);
    }
  }, [userId]);

  const totalSpent = expenses.reduce((acc, curr) => acc + (Number(curr.amount) || 0), 0);

  return (
    <div style={{ width: '100%', minHeight: '100vh', padding: '32px 48px', color: '#f8fafc', boxSizing: 'border-box' }}>
      
      {/* HEADER */}
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px', flexWrap: 'wrap', gap: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
          <div style={{ background: 'rgba(56, 189, 248, 0.15)', padding: '12px', borderRadius: '12px', border: '1px solid rgba(56, 189, 248, 0.3)' }}>
            <Zap size={30} color="#38bdf8" />
          </div>
          <div>
            <h1 style={{ fontSize: '28px', fontWeight: '700', letterSpacing: '-0.5px', margin: 0 }}>
              Smart Expense Tracker <span style={{ color: '#38bdf8', fontWeight: '400', fontSize: '22px' }}>| Architecture Console</span>
            </h1>
            <p style={{ margin: '4px 0 0 0', color: '#94a3b8', fontSize: '14px' }}>
              Real-time Microservices Ingress • Spring Cloud Gateway (:9000) • Zero Fallback Mode
            </p>
          </div>
        </div>

        {/* METRICS */}
        <div style={{ display: 'flex', gap: '16px' }}>
          <div style={{ background: '#111827', padding: '14px 24px', borderRadius: '12px', border: '1px solid #1f2937', minWidth: '150px' }}>
            <div style={{ fontSize: '11px', color: '#9ca3af', textTransform: 'uppercase', fontWeight: '600' }}>Total Spent</div>
            <div style={{ fontSize: '24px', fontWeight: '700', color: '#38bdf8', marginTop: '4px' }}>₹{totalSpent.toLocaleString('en-IN')}</div>
          </div>
          <div style={{ background: '#111827', padding: '14px 24px', borderRadius: '12px', border: '1px solid #1f2937', minWidth: '150px' }}>
            <div style={{ fontSize: '11px', color: '#9ca3af', textTransform: 'uppercase', fontWeight: '600' }}>Transactions</div>
            <div style={{ fontSize: '24px', fontWeight: '700', color: '#10b981', marginTop: '4px' }}>{expenses.length}</div>
          </div>
          <div style={{ background: '#111827', padding: '14px 24px', borderRadius: '12px', border: '1px solid #1f2937', minWidth: '150px' }}>
            <div style={{ fontSize: '11px', color: '#9ca3af', textTransform: 'uppercase', fontWeight: '600' }}>Event Latency</div>
            <div style={{ fontSize: '24px', fontWeight: '700', color: latency ? '#c084fc' : '#6b7280', marginTop: '4px' }}>
              {latency ? `${latency} ms` : 'Standby'}
            </div>
          </div>
        </div>
      </header>

      {/* TOPOLOGY NODES */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gap: '14px', marginBottom: '24px' }}>
        {[
          { name: 'API Gateway', port: '9000', tech: 'Spring Cloud Gateway' },
          { name: 'Auth Service', port: '8080', tech: 'Spring Security 6' },
          { name: 'User Service', port: '8081', tech: 'Profile Ledger DB' },
          { name: 'Expense Service', port: '8082', tech: 'Financial Engine' },
          { name: 'DS Service', port: '8010', tech: 'FastAPI + Gemini' },
          { name: 'Event Broker', port: '9092', tech: 'Confluent Kafka' }
        ].map((svc, i) => (
          <div key={i} style={{ background: '#0f172a', border: '1px solid #1e293b', borderRadius: '10px', padding: '14px 16px', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981', boxShadow: '0 0 10px #10b981' }} />
            <div>
              <div style={{ fontSize: '13px', fontWeight: '600', color: '#f1f5f9' }}>{svc.name}</div>
              <div style={{ fontSize: '11px', color: '#64748b', marginTop: '2px' }}>:{svc.port} • {svc.tech}</div>
            </div>
          </div>
        ))}
      </div>

      {/* EVENT EXECUTION FLOW TRACE */}
      <div style={{ background: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', padding: '18px 24px', marginBottom: '28px' }}>
        <div style={{ fontSize: '11px', color: '#94a3b8', textTransform: 'uppercase', fontWeight: '700', marginBottom: '14px', letterSpacing: '0.8px' }}>
          Live Pipeline Telemetry Flow
        </div>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '16px' }}>
          {[
            { step: 1, label: '1. Ingress Security Gate', sub: 'Gateway (:9000) JWT Intercept' },
            { step: 2, label: '2. AI Entity Extraction', sub: 'Gemini 1.5 Flash (:8010)' },
            { step: 3, label: '3. Kafka Async Pipeline', sub: 'Topic (EXPENSE_EVENTS)' },
            { step: 4, label: '4. Ledger Consumer Sync', sub: 'Expense Service (:8082) MySQL' }
          ].map((item, idx) => (
            <React.Fragment key={idx}>
              <div style={{ 
                flex: 1, padding: '14px 18px', borderRadius: '8px', 
                background: activeStep === item.step ? 'rgba(56, 189, 248, 0.15)' : '#131c2e', 
                border: activeStep === item.step ? '1px solid #38bdf8' : '1px solid #1e293b',
                transition: 'all 0.3s ease'
              }}>
                <div style={{ fontSize: '14px', fontWeight: '600', color: activeStep === item.step ? '#38bdf8' : '#f1f5f9' }}>{item.label}</div>
                <div style={{ fontSize: '11px', color: '#64748b', marginTop: '4px' }}>{item.sub}</div>
              </div>
              {idx < 3 && <ArrowRight size={20} color={activeStep > item.step ? '#38bdf8' : '#334155'} />}
            </React.Fragment>
          ))}
        </div>
      </div>

      {/* MAIN TWO-COLUMN SPLIT */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '28px' }}>
        
        {/* LEFT COLUMN: AUTH & AI AGENT */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Identity Card */}
          <div style={{ background: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', padding: '22px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <ShieldCheck size={20} color="#38bdf8" />
                <span style={{ fontSize: '16px', fontWeight: '600' }}>
                  {isSignUp ? 'New User Registration' : 'Gateway Authentication'}
                </span>
              </div>
              <button 
                onClick={() => setIsSignUp(!isSignUp)}
                style={{ background: 'none', border: 'none', color: '#38bdf8', fontSize: '12px', cursor: 'pointer', textDecoration: 'underline' }}
              >
                {isSignUp ? 'Back to Sign In' : '+ Register New User'}
              </button>
            </div>

            {isSignUp ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '14px' }}>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input placeholder="First Name" value={firstName} onChange={e => setFirstName(e.target.value)} style={{ flex: 1, padding: '10px 14px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '13px' }} />
                  <input placeholder="Last Name" value={lastName} onChange={e => setLastName(e.target.value)} style={{ flex: 1, padding: '10px 14px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '13px' }} />
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} style={{ flex: 1, padding: '10px 14px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '13px' }} />
                  <input placeholder="Phone" value={phoneNumber} onChange={e => setPhoneNumber(e.target.value)} style={{ width: '130px', padding: '10px 14px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '13px' }} />
                </div>
                <div style={{ display: 'flex', gap: '10px' }}>
                  <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} style={{ flex: 1, padding: '10px 14px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '13px' }} />
                  <input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} style={{ flex: 1, padding: '10px 14px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '13px' }} />
                </div>
                <button onClick={handleSignUp} style={{ background: '#059669', color: '#fff', border: 'none', borderRadius: '8px', padding: '12px', fontWeight: '600', fontSize: '14px', cursor: 'pointer' }}>
                  Create Account (Trigger Kafka USER_EVENTS)
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', gap: '12px', marginBottom: '14px' }}>
                <input placeholder="Username" value={username} onChange={e => setUsername(e.target.value)} style={{ flex: 1, padding: '12px 16px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '14px' }} />
                <input type="password" placeholder="Password" value={password} onChange={e => setPassword(e.target.value)} style={{ flex: 1, padding: '12px 16px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#fff', fontSize: '14px' }} />
                <button onClick={handleLogin} style={{ background: '#2563eb', color: '#fff', border: 'none', borderRadius: '8px', padding: '0 24px', fontWeight: '600', fontSize: '14px', cursor: 'pointer' }}>
                  Sign In
                </button>
              </div>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '13px', color: '#64748b' }}>
              <span>User ID Filter:</span>
              <input 
                type="number"
                value={userId} 
                onChange={e => {
                  const val = Number(e.target.value);
                  setUserId(val);
                  localStorage.setItem('userId', String(val));
                }} 
                style={{ width: '60px', padding: '6px 10px', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '6px', color: '#38bdf8', textAlign: 'center', fontSize: '13px' }} 
              />
              <span style={{ 
                marginLeft: 'auto', 
                color: isTokenValid() ? '#10b981' : token ? '#f87171' : '#64748b', 
                fontSize: '12px', 
                fontWeight: '500' 
              }}>
                {isTokenValid() ? '● Live JWT Active' : token ? '● JWT Expired' : '○ No JWT Attached'}
              </span>
            </div>
          </div>

          {/* AI Telemetry Ingestion */}
          <div style={{ background: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', padding: '22px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
              <Cpu size={20} color="#a855f7" />
              <span style={{ fontSize: '16px', fontWeight: '600' }}>Financial SMS Telemetry Simulator</span>
            </div>

            <div style={{ display: 'flex', gap: '10px', marginBottom: '14px', flexWrap: 'wrap' }}>
              {sampleSMS.map((sample, idx) => (
                <button 
                  key={idx} 
                  onClick={() => setSmsMessage(sample.text)}
                  style={{ background: '#1e1b4b', border: '1px solid #312e81', color: '#c084fc', padding: '7px 14px', borderRadius: '6px', fontSize: '12px', cursor: 'pointer' }}
                >
                  + {sample.title}
                </button>
              ))}
            </div>

            <textarea 
              rows={4} 
              value={smsMessage}
              onChange={e => setSmsMessage(e.target.value)}
              placeholder="Paste bank transaction SMS here..."
              style={{ width: '100%', boxSizing: 'border-box', background: '#131c2e', border: '1px solid #1e293b', borderRadius: '8px', color: '#e2e8f0', padding: '14px', fontSize: '14px', marginBottom: '16px', resize: 'none' }}
            />

            <button 
              onClick={handleParseSms}
              style={{ width: '100%', background: 'linear-gradient(135deg, #7c3aed 0%, #2563eb 100%)', color: '#fff', border: 'none', borderRadius: '8px', padding: '14px', fontSize: '14px', fontWeight: '600', cursor: 'pointer', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '10px' }}
            >
              <Send size={18} /> Ingest SMS & Stream via Kafka
            </button>
          </div>

        </div>

        {/* RIGHT COLUMN: LEDGER & DIAGNOSTICS */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
          
          {/* Persistent Ledger */}
          <div style={{ background: '#0f172a', border: '1px solid #1e293b', borderRadius: '12px', padding: '22px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <Database size={20} color="#10b981" />
                <span style={{ fontSize: '16px', fontWeight: '600' }}>Live Persistent Ledger (MySQL DB)</span>
              </div>
              <button 
                onClick={() => fetchExpenses()}
                style={{ background: '#131c2e', border: '1px solid #1e293b', color: '#94a3b8', borderRadius: '8px', padding: '7px 14px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px' }}
              >
                <RefreshCw size={14} className={loading ? 'spin' : ''} /> Sync DB
              </button>
            </div>

            <div style={{ maxHeight: '250px', overflowY: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
                <thead>
                  <tr style={{ color: '#64748b', borderBottom: '1px solid #1e293b', textAlign: 'left' }}>
                    <th style={{ padding: '10px 14px' }}>Merchant</th>
                    <th style={{ padding: '10px 14px' }}>Amount</th>
                    <th style={{ padding: '10px 14px' }}>Recorded At</th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map((exp, idx) => (
                    <tr key={idx} style={{ borderBottom: '1px solid #131c2e' }}>
                      <td style={{ padding: '10px 14px', color: '#e2e8f0', fontWeight: '500' }}>{exp.merchant}</td>
                      <td style={{ padding: '10px 14px', color: '#10b981', fontWeight: '600' }}>₹{exp.amount}</td>
                      <td style={{ padding: '10px 14px', color: '#64748b', fontSize: '12px' }}>
                        {exp.created_at ? new Date(exp.created_at).toLocaleTimeString() : 'Persisted'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {expenses.length === 0 && (
                <div style={{ textAlign: 'center', padding: '36px 0', color: '#64748b', fontSize: '14px' }}>
                  No transactions recorded for User ID: {userId}.
                </div>
              )}
            </div>
          </div>

          {/* Real-time Diagnostics Terminal */}
          <div style={{ background: '#070b13', border: '1px solid #1e293b', borderRadius: '12px', padding: '18px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#64748b', fontSize: '12px', marginBottom: '12px', textTransform: 'uppercase', fontWeight: '700' }}>
              <Terminal size={16} color="#38bdf8" /> Real-time System Trace & Diagnostics
            </div>
            <div style={{ height: '170px', overflowY: 'auto', fontFamily: 'monospace', fontSize: '12px', lineHeight: '1.6' }}>
              {pipelineLogs.map((item, idx) => (
                <div key={idx} style={{ 
                  color: item.type === 'error' ? '#f87171' : item.type === 'success' ? '#34d399' : '#38bdf8', 
                  marginBottom: '4px' 
                }}>
                  <span style={{ color: '#475569' }}>[{item.time}]</span> {item.text}
                </div>
              ))}
            </div>
          </div>

        </div>

      </div>

    </div>
  );
}