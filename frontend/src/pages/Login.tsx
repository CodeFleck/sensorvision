import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Activity, ArrowRight, Zap, Shield, BarChart3 } from 'lucide-react';

export const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const { login } = useAuth();
  const navigate = useNavigate();

  const backendUrl = window.location.origin;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login({ username, password });
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    window.location.href = `${backendUrl}/oauth2/authorization/google`;
  };

  return (
    <div className="min-h-screen relative overflow-hidden bg-black">
      {/* Premium OLED Background with Mesh Gradient */}
      <div className="absolute inset-0">
        {/* Primary mesh gradient - subtle teal */}
        <div
          className="absolute inset-0"
          style={{
            background: `
              radial-gradient(ellipse 80% 50% at 50% -20%, rgba(20, 184, 166, 0.15), transparent),
              radial-gradient(ellipse 60% 50% at 80% 50%, rgba(59, 130, 246, 0.08), transparent),
              radial-gradient(ellipse 50% 40% at 20% 80%, rgba(139, 92, 246, 0.06), transparent)
            `
          }}
        />

        {/* Animated floating orb - top right */}
        <div
          className="absolute -top-32 -right-32 w-[500px] h-[500px] rounded-full opacity-30 animate-pulse-slow"
          style={{
            background: 'radial-gradient(circle, rgba(20, 184, 166, 0.4) 0%, transparent 70%)',
            filter: 'blur(60px)',
          }}
        />

        {/* Animated floating orb - bottom left */}
        <div
          className="absolute -bottom-32 -left-32 w-[400px] h-[400px] rounded-full opacity-20 animate-pulse-slow"
          style={{
            background: 'radial-gradient(circle, rgba(59, 130, 246, 0.5) 0%, transparent 70%)',
            filter: 'blur(80px)',
            animationDelay: '2s',
          }}
        />

        {/* Subtle noise texture overlay */}
        <div
          className="absolute inset-0 opacity-[0.015]"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noise)'/%3E%3C/svg%3E")`,
          }}
        />

        {/* Grid pattern - very subtle */}
        <div
          className="absolute inset-0 opacity-[0.02]"
          style={{
            backgroundImage: `
              linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
              linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px)
            `,
            backgroundSize: '80px 80px'
          }}
        />
      </div>

      <div className="relative min-h-screen flex">
        {/* Left side - Branding (Premium typography & layout) */}
        <div className="hidden lg:flex lg:w-1/2 xl:w-3/5 flex-col justify-between p-12 xl:p-20">
          {/* Logo with glow effect */}
          <div className="flex items-center gap-3 group cursor-pointer">
            <div className="relative">
              <div className="absolute inset-0 bg-teal-500/40 blur-2xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
              <div className="relative p-2 rounded-xl bg-gradient-to-br from-teal-500/20 to-teal-600/10 border border-teal-500/20">
                <Activity className="h-7 w-7 text-teal-400" />
              </div>
            </div>
            <span className="text-xl font-semibold text-white/90 tracking-tight">SensorVision</span>
          </div>

          {/* Hero content with premium typography */}
          <div className="max-w-xl">
            <h1 className="text-[clamp(2.5rem,5vw,4.5rem)] font-bold text-white leading-[1.05] tracking-[-0.03em] mb-8">
              Industrial IoT
              <br />
              <span className="bg-gradient-to-r from-teal-400 via-cyan-400 to-teal-400 bg-clip-text text-transparent">
                Intelligence
              </span>
            </h1>
            <p className="text-lg text-white/50 leading-relaxed mb-12 max-w-md font-light">
              Real-time monitoring, predictive analytics, and intelligent alerting
              for your entire device fleet.
            </p>

            {/* Feature pills with premium styling */}
            <div className="flex flex-wrap gap-3">
              {[
                { icon: Zap, label: 'Real-time Streaming', color: 'amber' },
                { icon: Shield, label: 'Enterprise Security', color: 'emerald' },
                { icon: BarChart3, label: 'Advanced Analytics', color: 'blue' },
              ].map(({ icon: Icon, label, color }) => (
                <div
                  key={label}
                  className="group flex items-center gap-2.5 px-4 py-2.5 rounded-full bg-white/[0.03] border border-white/[0.06] hover:bg-white/[0.05] hover:border-white/[0.1] transition-all duration-300 cursor-default"
                >
                  <Icon className={`h-4 w-4 ${
                    color === 'amber' ? 'text-amber-400' :
                    color === 'emerald' ? 'text-emerald-400' :
                    'text-blue-400'
                  }`} />
                  <span className="text-sm text-white/60 group-hover:text-white/80 transition-colors">{label}</span>
                </div>
              ))}
            </div>
          </div>

          {/* Footer stats with tabular numbers */}
          <div className="flex gap-16">
            {[
              { value: '99.9%', label: 'Uptime SLA' },
              { value: '<100ms', label: 'Latency' },
              { value: '1M+', label: 'Data points/day' },
            ].map(({ value, label }) => (
              <div key={label}>
                <div className="text-3xl font-bold text-white tracking-tight" style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {value}
                </div>
                <div className="text-sm text-white/30 font-medium mt-1">{label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Right side - Login form (Premium glass card) */}
        <div className="w-full lg:w-1/2 xl:w-2/5 flex items-center justify-center p-6 sm:p-12">
          <div className="w-full max-w-md">
            {/* Mobile logo */}
            <div className="lg:hidden flex items-center justify-center gap-3 mb-12">
              <div className="p-2 rounded-xl bg-gradient-to-br from-teal-500/20 to-teal-600/10 border border-teal-500/20">
                <Activity className="h-6 w-6 text-teal-400" />
              </div>
              <span className="text-lg font-semibold text-white/90">SensorVision</span>
            </div>

            {/* Premium glass card */}
            <div className="relative">
              {/* Subtle glow behind card */}
              <div className="absolute -inset-px rounded-2xl bg-gradient-to-b from-white/[0.08] to-transparent opacity-0 group-hover:opacity-100 transition-opacity" />

              <div
                className="relative rounded-2xl p-8 sm:p-10"
                style={{
                  background: 'rgba(255, 255, 255, 0.02)',
                  backdropFilter: 'blur(40px)',
                  WebkitBackdropFilter: 'blur(40px)',
                  border: '1px solid rgba(255, 255, 255, 0.06)',
                  boxShadow: `
                    0 0 0 1px rgba(255, 255, 255, 0.03) inset,
                    0 20px 50px -12px rgba(0, 0, 0, 0.5)
                  `
                }}
              >
                <div className="mb-8">
                  <h2 className="text-2xl font-bold text-white tracking-tight mb-2">Welcome back</h2>
                  <p className="text-white/70 font-light">
                    Sign in to access your dashboard
                  </p>
                </div>

                {error && (
                  <div
                    role="alert"
                    aria-live="assertive"
                    className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20"
                  >
                    <p className="text-sm text-red-400">{error}</p>
                  </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-5">
                  {/* Username field */}
                  <div className="relative">
                    <label
                      htmlFor="username"
                      className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                        focusedField === 'username' || username
                          ? '-top-2.5 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Username
                    </label>
                    <input
                      id="username"
                      type="text"
                      autoComplete="username"
                      required
                      aria-label="Username"
                      aria-required="true"
                      value={username}
                      onChange={(e) => setUsername(e.target.value)}
                      onFocus={() => setFocusedField('username')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full px-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                  </div>

                  {/* Password field */}
                  <div className="relative">
                    <label
                      htmlFor="password"
                      className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                        focusedField === 'password' || password
                          ? '-top-2.5 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Password
                    </label>
                    <input
                      id="password"
                      type="password"
                      autoComplete="current-password"
                      required
                      aria-label="Password"
                      aria-required="true"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      onFocus={() => setFocusedField('password')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full px-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                  </div>

                  {/* Forgot password link */}
                  <div className="flex justify-end">
                    <Link
                      to="/forgot-password"
                      className="text-sm text-white/40 hover:text-teal-400 transition-colors duration-200"
                    >
                      Forgot password?
                    </Link>
                  </div>

                  {/* Premium submit button */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="group w-full relative overflow-hidden px-6 py-4 rounded-xl font-semibold text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {/* Button gradient background */}
                    <div className="absolute inset-0 bg-gradient-to-r from-teal-600 to-teal-500 transition-all duration-300 group-hover:from-teal-500 group-hover:to-cyan-500" />

                    {/* Shimmer effect on hover */}
                    <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                      <div
                        className="absolute inset-0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700"
                        style={{
                          background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent)'
                        }}
                      />
                    </div>

                    {/* Button glow */}
                    <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 blur-xl bg-gradient-to-r from-teal-500/50 to-cyan-500/50" />

                    <span className="relative flex items-center justify-center gap-2">
                      {loading ? (
                        <>
                          <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                          </svg>
                          Signing in...
                        </>
                      ) : (
                        <>
                          Sign in
                          <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform duration-200" />
                        </>
                      )}
                    </span>
                  </button>
                </form>

                {/* Divider */}
                <div className="relative my-8">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-white/[0.06]" />
                  </div>
                  <div className="relative flex justify-center">
                    <span className="px-4 text-sm text-white/30 bg-black">or continue with</span>
                  </div>
                </div>

                {/* Google OAuth - Premium styling */}
                <button
                  type="button"
                  onClick={handleGoogleLogin}
                  className="w-full flex items-center justify-center gap-3 px-6 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white font-medium hover:bg-white/[0.06] hover:border-white/[0.12] transition-all duration-200 group"
                >
                  <svg className="w-5 h-5" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                  </svg>
                  <span className="text-white/70 group-hover:text-white/90 transition-colors">Sign in with Google</span>
                </button>

                {/* Register link */}
                <p className="mt-8 text-center text-white/40">
                  Don&apos;t have an account?{' '}
                  <Link to="/register" className="text-teal-400 hover:text-teal-300 font-medium transition-colors">
                    Create one
                  </Link>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
