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
    <div className="min-h-screen relative overflow-hidden bg-[#030712]">
      {/* Animated gradient background */}
      <div className="absolute inset-0 overflow-hidden">
        {/* Primary gradient orb */}
        <div className="absolute -top-1/2 -left-1/2 w-full h-full bg-gradient-to-br from-blue-600/20 via-transparent to-transparent rounded-full blur-3xl animate-pulse-slow" />
        {/* Secondary gradient orb */}
        <div className="absolute -bottom-1/2 -right-1/2 w-full h-full bg-gradient-to-tl from-emerald-600/15 via-transparent to-transparent rounded-full blur-3xl animate-pulse-slow" style={{ animationDelay: '2s' }} />
        {/* Accent orb */}
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-gradient-to-br from-violet-600/10 to-transparent rounded-full blur-3xl animate-pulse-slow" style={{ animationDelay: '4s' }} />

        {/* Grid pattern overlay */}
        <div
          className="absolute inset-0 opacity-[0.02]"
          style={{
            backgroundImage: `linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px),
                              linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px)`,
            backgroundSize: '64px 64px'
          }}
        />
      </div>

      <div className="relative min-h-screen flex">
        {/* Left side - Branding */}
        <div className="hidden lg:flex lg:w-1/2 xl:w-3/5 flex-col justify-between p-12 xl:p-16">
          {/* Logo */}
          <div className="flex items-center gap-3">
            <div className="relative">
              <div className="absolute inset-0 bg-blue-500/30 blur-xl rounded-full" />
              <Activity className="relative h-10 w-10 text-blue-400" />
            </div>
            <span className="text-2xl font-bold text-white tracking-tight">SensorVision</span>
          </div>

          {/* Hero content */}
          <div className="max-w-xl">
            <h1 className="text-5xl xl:text-6xl font-bold text-white leading-[1.1] tracking-tight mb-6">
              Industrial IoT
              <br />
              <span className="bg-gradient-to-r from-blue-400 via-emerald-400 to-blue-400 bg-clip-text text-transparent">
                Intelligence
              </span>
            </h1>
            <p className="text-lg text-gray-400 leading-relaxed mb-10">
              Real-time monitoring, predictive analytics, and intelligent alerting
              for your entire device fleet. Scale from 10 to 10,000 sensors effortlessly.
            </p>

            {/* Feature pills */}
            <div className="flex flex-wrap gap-3">
              <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 backdrop-blur-sm">
                <Zap className="h-4 w-4 text-amber-400" />
                <span className="text-sm text-gray-300">Real-time Streaming</span>
              </div>
              <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 backdrop-blur-sm">
                <Shield className="h-4 w-4 text-emerald-400" />
                <span className="text-sm text-gray-300">Enterprise Security</span>
              </div>
              <div className="flex items-center gap-2 px-4 py-2 rounded-full bg-white/5 border border-white/10 backdrop-blur-sm">
                <BarChart3 className="h-4 w-4 text-blue-400" />
                <span className="text-sm text-gray-300">Advanced Analytics</span>
              </div>
            </div>
          </div>

          {/* Footer stats */}
          <div className="flex gap-12">
            <div>
              <div className="text-3xl font-bold text-white">99.9%</div>
              <div className="text-sm text-gray-500">Uptime SLA</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-white">&lt;100ms</div>
              <div className="text-sm text-gray-500">Latency</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-white">1M+</div>
              <div className="text-sm text-gray-500">Data points/day</div>
            </div>
          </div>
        </div>

        {/* Right side - Login form */}
        <div className="w-full lg:w-1/2 xl:w-2/5 flex items-center justify-center p-6 sm:p-12">
          <div className="w-full max-w-md">
            {/* Mobile logo */}
            <div className="lg:hidden flex items-center justify-center gap-3 mb-10">
              <Activity className="h-8 w-8 text-blue-400" />
              <span className="text-xl font-bold text-white">SensorVision</span>
            </div>

            {/* Glass card */}
            <div className="relative">
              {/* Glow effect behind card */}
              <div className="absolute -inset-1 bg-gradient-to-r from-blue-600/20 via-emerald-600/20 to-blue-600/20 rounded-2xl blur-xl opacity-50" />

              <div className="relative bg-white/[0.03] backdrop-blur-xl border border-white/10 rounded-2xl p-8 shadow-2xl">
                <div className="mb-8">
                  <h2 className="text-2xl font-bold text-white mb-2">Welcome back</h2>
                  <p className="text-gray-400">
                    Sign in to access your dashboard
                  </p>
                </div>

                {error && (
                  <div
                    role="alert"
                    aria-live="assertive"
                    className="mb-6 p-4 rounded-xl bg-red-500/10 border border-red-500/20 backdrop-blur-sm"
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
                          ? '-top-2.5 text-xs text-blue-400 bg-[#030712] px-2'
                          : 'top-3.5 text-gray-500'
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
                      className="w-full px-4 py-3.5 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/20 focus-visible:ring-offset-2 focus-visible:ring-offset-[#030712] transition-all duration-200"
                    />
                  </div>

                  {/* Password field */}
                  <div className="relative">
                    <label
                      htmlFor="password"
                      className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                        focusedField === 'password' || password
                          ? '-top-2.5 text-xs text-blue-400 bg-[#030712] px-2'
                          : 'top-3.5 text-gray-500'
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
                      className="w-full px-4 py-3.5 bg-white/[0.03] border border-white/10 rounded-xl text-white placeholder-transparent focus:outline-none focus:border-blue-500/50 focus:ring-2 focus:ring-blue-500/20 focus-visible:ring-offset-2 focus-visible:ring-offset-[#030712] transition-all duration-200"
                    />
                  </div>

                  {/* Forgot password link */}
                  <div className="flex justify-end">
                    <Link
                      to="/forgot-password"
                      className="text-sm text-gray-400 hover:text-blue-400 transition-colors"
                    >
                      Forgot password?
                    </Link>
                  </div>

                  {/* Submit button */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="group w-full relative overflow-hidden px-6 py-3.5 rounded-xl font-medium text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {/* Button gradient background */}
                    <div className="absolute inset-0 bg-gradient-to-r from-blue-600 to-blue-500 transition-all duration-300 group-hover:from-blue-500 group-hover:to-emerald-500" />
                    {/* Button glow */}
                    <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-gradient-to-r from-blue-600/50 to-emerald-600/50 blur-xl" />

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
                          <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform" />
                        </>
                      )}
                    </span>
                  </button>
                </form>

                {/* Divider */}
                <div className="relative my-8">
                  <div className="absolute inset-0 flex items-center">
                    <div className="w-full border-t border-white/10" />
                  </div>
                  <div className="relative flex justify-center">
                    <span className="px-4 text-sm text-gray-500 bg-[#030712]">or continue with</span>
                  </div>
                </div>

                {/* Google OAuth */}
                <button
                  type="button"
                  onClick={handleGoogleLogin}
                  className="w-full flex items-center justify-center gap-3 px-6 py-3.5 bg-white/[0.03] border border-white/10 rounded-xl text-white font-medium hover:bg-white/[0.06] hover:border-white/20 transition-all duration-200"
                >
                  <svg className="w-5 h-5" viewBox="0 0 24 24">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                  </svg>
                  Sign in with Google
                </button>

                {/* Register link */}
                <p className="mt-8 text-center text-gray-400">
                  Don&apos;t have an account?{' '}
                  <Link to="/register" className="text-blue-400 hover:text-blue-300 font-medium transition-colors">
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
