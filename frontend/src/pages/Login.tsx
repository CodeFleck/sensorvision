import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Activity, ArrowRight, Zap, Shield, BarChart3, Eye, EyeOff } from 'lucide-react';

export const Login: React.FC = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const [autofilledFields, setAutofilledFields] = useState<Set<string>>(new Set());
  const { login } = useAuth();
  const navigate = useNavigate();

  // Detect browser autofill via CSS animation
  const handleAnimationStart = (e: React.AnimationEvent<HTMLInputElement>, fieldName: string) => {
    if (e.animationName === 'onAutoFillStart') {
      setAutofilledFields(prev => new Set(prev).add(fieldName));
    } else if (e.animationName === 'onAutoFillCancel') {
      setAutofilledFields(prev => {
        const next = new Set(prev);
        next.delete(fieldName);
        return next;
      });
    }
  };

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
    <div className="min-h-screen relative overflow-hidden bg-[#f7f8fa]">
      {/* Elegant Light Background with Subtle Gradient Mesh */}
      <div className="absolute inset-0">
        {/* Primary gradient - soft blue-teal wash */}
        <div
          className="absolute inset-0"
          style={{
            background: `
              radial-gradient(ellipse 100% 100% at 0% 0%, rgba(9, 105, 218, 0.08), transparent 50%),
              radial-gradient(ellipse 80% 80% at 100% 0%, rgba(20, 184, 166, 0.06), transparent 50%),
              radial-gradient(ellipse 60% 60% at 50% 100%, rgba(139, 92, 246, 0.04), transparent 50%)
            `
          }}
        />

        {/* Floating accent orb - top left */}
        <div
          className="absolute -top-40 -left-40 w-[600px] h-[600px] rounded-full opacity-40 animate-pulse-slow"
          style={{
            background: 'radial-gradient(circle, rgba(9, 105, 218, 0.12) 0%, transparent 70%)',
            filter: 'blur(80px)',
          }}
        />

        {/* Floating accent orb - bottom right */}
        <div
          className="absolute -bottom-32 -right-32 w-[500px] h-[500px] rounded-full opacity-30 animate-pulse-slow"
          style={{
            background: 'radial-gradient(circle, rgba(20, 184, 166, 0.15) 0%, transparent 70%)',
            filter: 'blur(60px)',
            animationDelay: '1.5s',
          }}
        />

        {/* Subtle grid pattern */}
        <div
          className="absolute inset-0 opacity-[0.35]"
          style={{
            backgroundImage: `
              linear-gradient(rgba(0,0,0,0.015) 1px, transparent 1px),
              linear-gradient(90deg, rgba(0,0,0,0.015) 1px, transparent 1px)
            `,
            backgroundSize: '64px 64px'
          }}
        />
      </div>

      <div className="relative min-h-screen flex">
        {/* Left Panel - Hero Branding */}
        <div className="hidden lg:flex lg:w-[55%] xl:w-[58%] flex-col justify-between p-12 xl:p-16 2xl:p-20">
          {/* Logo */}
          <div className="flex items-center gap-3 group cursor-pointer">
            <div className="relative">
              <div className="absolute inset-0 bg-[#0969da]/20 blur-xl rounded-full opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
              <div className="relative p-2.5 rounded-xl bg-gradient-to-br from-[#0969da]/10 to-[#14b8a6]/10 border border-[#0969da]/15 shadow-sm">
                <Activity className="h-6 w-6 text-[#0969da]" />
              </div>
            </div>
            <span className="text-xl font-semibold text-[#1f2328] tracking-tight">Industrial Cloud</span>
          </div>

          {/* Hero Content */}
          <div className="max-w-xl -mt-16">
            {/* Eyebrow */}
            <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#0969da]/8 border border-[#0969da]/12 mb-6">
              <div className="w-1.5 h-1.5 rounded-full bg-[#14b8a6] animate-pulse" />
              <span className="text-xs font-medium text-[#0969da] uppercase tracking-wider">IoT Platform</span>
            </div>

            <h1 className="text-[clamp(2.75rem,4.5vw,4rem)] font-bold text-[#1f2328] leading-[1.08] tracking-[-0.025em] mb-6">
              Industrial-grade
              <br />
              <span className="bg-gradient-to-r from-[#0969da] via-[#14b8a6] to-[#0969da] bg-clip-text text-transparent bg-[length:200%_auto] animate-gradient">
                device intelligence
              </span>
            </h1>

            <p className="text-lg text-[#4a535c] leading-relaxed mb-10 max-w-md">
              Monitor, analyze, and optimize your entire device fleet with real-time
              telemetry and predictive insights.
            </p>

            {/* Feature Cards - Horizontal layout */}
            <div className="flex flex-col sm:flex-row gap-4">
              {[
                { icon: Zap, label: 'Real-time', desc: 'Live streaming', color: '#f59e0b' },
                { icon: Shield, label: 'Secure', desc: 'Enterprise-grade', color: '#10b981' },
                { icon: BarChart3, label: 'Analytics', desc: 'Deep insights', color: '#0969da' },
              ].map(({ icon: Icon, label, desc, color }) => (
                <div
                  key={label}
                  className="group flex-1 p-4 rounded-2xl bg-white/70 border border-[#d0d7de]/60 hover:border-[#d0d7de] hover:bg-white/90 hover:shadow-lg transition-all duration-300 cursor-default backdrop-blur-sm"
                >
                  <div
                    className="w-10 h-10 rounded-xl flex items-center justify-center mb-3 transition-transform duration-300 group-hover:scale-110"
                    style={{ backgroundColor: `${color}12` }}
                  >
                    <Icon className="h-5 w-5" style={{ color }} />
                  </div>
                  <div className="text-sm font-semibold text-[#1f2328] mb-0.5">{label}</div>
                  <div className="text-xs text-[#4a535c]">{desc}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Bottom Stats */}
          <div className="flex items-center gap-12">
            {[
              { value: '99.9%', label: 'Uptime SLA' },
              { value: '<100ms', label: 'Latency' },
              { value: '1M+', label: 'Daily events' },
            ].map(({ value, label }) => (
              <div key={label} className="group cursor-default">
                <div
                  className="text-3xl font-bold text-[#1f2328] tracking-tight mb-1 transition-colors duration-300 group-hover:text-[#0969da]"
                  style={{ fontVariantNumeric: 'tabular-nums' }}
                >
                  {value}
                </div>
                <div className="text-sm text-[#4a535c] font-medium">{label}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Right Panel - Login Form */}
        <div className="w-full lg:w-[45%] xl:w-[42%] flex items-center justify-center p-6 sm:p-10 lg:p-12">
          <div className="w-full max-w-[420px]">
            {/* Mobile Logo */}
            <div className="lg:hidden flex items-center justify-center gap-3 mb-10">
              <div className="p-2 rounded-xl bg-gradient-to-br from-[#0969da]/10 to-[#14b8a6]/10 border border-[#0969da]/15">
                <Activity className="h-6 w-6 text-[#0969da]" />
              </div>
              <span className="text-xl font-semibold text-[#1f2328]">Industrial Cloud</span>
            </div>

            {/* Form Card */}
            <div
              className="relative rounded-3xl p-8 sm:p-10"
              style={{
                background: 'rgba(255, 255, 255, 0.85)',
                backdropFilter: 'blur(20px)',
                WebkitBackdropFilter: 'blur(20px)',
                border: '1px solid rgba(208, 215, 222, 0.6)',
                boxShadow: `
                  0 0 0 1px rgba(255, 255, 255, 0.8) inset,
                  0 1px 2px rgba(31, 35, 40, 0.04),
                  0 4px 16px rgba(31, 35, 40, 0.08),
                  0 12px 40px rgba(31, 35, 40, 0.12)
                `
              }}
            >
              {/* Header */}
              <div className="mb-8">
                <h2 className="text-2xl font-bold text-[#1f2328] tracking-tight mb-2">
                  Welcome back
                </h2>
                <p className="text-[#4a535c]">
                  Sign in to your dashboard
                </p>
              </div>

              {/* Error Alert */}
              {error && (
                <div
                  role="alert"
                  aria-live="assertive"
                  className="mb-6 p-4 rounded-xl bg-[#ffebe9] border border-[#cf222e]/20 animate-fadeIn"
                >
                  <p className="text-sm text-[#a40e26] font-medium">{error}</p>
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-5">
                {/* Username/Email Field */}
                <div className="relative">
                  <label
                    htmlFor="username"
                    className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                      focusedField === 'username' || username || autofilledFields.has('username')
                        ? '-top-2.5 text-xs font-medium text-[#0969da] bg-white px-2 rounded'
                        : 'top-3.5 text-[#4a535c]'
                    }`}
                  >
                    Email or Username
                  </label>
                  <input
                    id="username"
                    type="text"
                    autoComplete="username"
                    required
                    aria-label="Email or Username"
                    aria-required="true"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    onFocus={() => setFocusedField('username')}
                    onBlur={() => setFocusedField(null)}
                    onAnimationStart={(e) => handleAnimationStart(e, 'username')}
                    className="w-full px-4 py-3.5 rounded-xl text-[#1f2328] placeholder-transparent focus:outline-none focus:border-[#0969da] focus:ring-2 focus:ring-[#0969da]/20 transition-all duration-200"
                    style={{ backgroundColor: '#ffffff', border: '1px solid #d0d7de' }}
                  />
                </div>

                {/* Password Field */}
                <div className="relative">
                  <label
                    htmlFor="password"
                    className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                      focusedField === 'password' || password || autofilledFields.has('password')
                        ? '-top-2.5 text-xs font-medium text-[#0969da] bg-white px-2 rounded'
                        : 'top-3.5 text-[#4a535c]'
                    }`}
                  >
                    Password
                  </label>
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    aria-label="Password"
                    aria-required="true"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onFocus={() => setFocusedField('password')}
                    onBlur={() => setFocusedField(null)}
                    onAnimationStart={(e) => handleAnimationStart(e, 'password')}
                    className="w-full px-4 py-3.5 pr-12 rounded-xl text-[#1f2328] placeholder-transparent focus:outline-none focus:border-[#0969da] focus:ring-2 focus:ring-[#0969da]/20 transition-all duration-200"
                    style={{ backgroundColor: '#ffffff', border: '1px solid #d0d7de' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-4 top-1/2 -translate-y-1/2 text-[#4a535c] hover:text-[#1f2328] transition-colors"
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>

                {/* Forgot Password */}
                <div className="flex justify-end">
                  <Link
                    to="/forgot-password"
                    className="text-sm text-[#0969da] hover:text-[#0550ae] font-medium transition-colors duration-200"
                  >
                    Forgot password?
                  </Link>
                </div>

                {/* Submit Button */}
                <button
                  type="submit"
                  disabled={loading}
                  aria-busy={loading}
                  className="group w-full relative overflow-hidden px-6 py-4 rounded-xl font-semibold text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-[#0969da]/25 hover:shadow-xl hover:shadow-[#14b8a6]/30 hover:scale-[1.02]"
                >
                  {/* Animated gradient background - matches "device intelligence" text */}
                  <div className="absolute inset-0 bg-gradient-to-r from-[#0969da] via-[#14b8a6] to-[#0969da] bg-[length:200%_auto] animate-gradient rounded-xl" />

                  {/* Shimmer effect on hover */}
                  <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                    <div
                      className="absolute inset-0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000"
                      style={{
                        background: 'linear-gradient(90deg, transparent, rgba(255,255,255,0.3), transparent)'
                      }}
                    />
                  </div>

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
                  <div className="w-full border-t border-[#d0d7de]" />
                </div>
                <div className="relative flex justify-center">
                  <span className="px-4 text-sm text-[#4a535c]" style={{ backgroundColor: '#ffffff' }}>or continue with</span>
                </div>
              </div>

              {/* Google OAuth */}
              <button
                type="button"
                onClick={handleGoogleLogin}
                className="w-full flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl text-[#1f2328] font-medium hover:border-[#afb8c1] transition-all duration-200 group shadow-sm hover:shadow"
                style={{ backgroundColor: '#ffffff', border: '1px solid #d0d7de' }}
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                </svg>
                <span className="group-hover:text-[#1f2328] transition-colors">Continue with Google</span>
              </button>

              {/* Register Link */}
              <p className="mt-8 text-center text-[#4a535c]">
                Don&apos;t have an account?{' '}
                <Link
                  to="/register"
                  className="text-[#0969da] hover:text-[#0550ae] font-semibold transition-colors"
                >
                  Create one
                </Link>
              </p>
            </div>

            {/* Trust Badges */}
            <div className="mt-8 flex items-center justify-center gap-6 opacity-80">
              <div className="flex items-center gap-2 text-xs text-[#4a535c]">
                <Shield className="h-4 w-4" />
                <span>256-bit SSL</span>
              </div>
              <div className="w-px h-4 bg-[#d0d7de]" />
              <div className="flex items-center gap-2 text-xs text-[#4a535c]">
                <span>SOC 2 Compliant</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
