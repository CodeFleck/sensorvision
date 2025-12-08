import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Activity, ArrowRight, Building2, User, Mail, Lock, Check, Zap, Shield, BarChart3 } from 'lucide-react';

export const Register: React.FC = () => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    firstName: '',
    lastName: '',
    organizationName: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [focusedField, setFocusedField] = useState<string | null>(null);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters');
      return;
    }

    setLoading(true);

    try {
      await register({
        username: formData.username,
        email: formData.email,
        password: formData.password,
        firstName: formData.firstName,
        lastName: formData.lastName,
        organizationName: formData.organizationName || undefined,
      });
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed');
    } finally {
      setLoading(false);
    }
  };

  const passwordsMatch = formData.password && formData.confirmPassword && formData.password === formData.confirmPassword;

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
              Start your
              <br />
              <span className="bg-gradient-to-r from-teal-400 via-cyan-400 to-teal-400 bg-clip-text text-transparent">
                IoT Journey
              </span>
            </h1>
            <p className="text-lg text-white/50 leading-relaxed mb-12 max-w-md font-light">
              Join thousands of engineers monitoring their devices with real-time
              analytics and intelligent alerting.
            </p>

            {/* Feature pills with premium styling */}
            <div className="flex flex-wrap gap-3">
              {[
                { icon: Zap, label: 'Free Tier Available', color: 'amber' },
                { icon: Shield, label: 'SOC2 Compliant', color: 'emerald' },
                { icon: BarChart3, label: 'Unlimited Devices', color: 'blue' },
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
              { value: '10K+', label: 'Active Users' },
              { value: '50M+', label: 'Events/Month' },
              { value: '99.9%', label: 'Uptime' },
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

        {/* Right side - Register form (Premium glass card) */}
        <div className="w-full lg:w-1/2 xl:w-2/5 flex items-center justify-center p-6 sm:p-12">
          <div className="w-full max-w-md">
            {/* Mobile logo */}
            <div className="lg:hidden flex items-center justify-center gap-3 mb-8">
              <div className="p-2 rounded-xl bg-gradient-to-br from-teal-500/20 to-teal-600/10 border border-teal-500/20">
                <Activity className="h-6 w-6 text-teal-400" />
              </div>
              <span className="text-lg font-semibold text-white/90">SensorVision</span>
            </div>

            {/* Premium glass card */}
            <div className="relative">
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
                <div className="mb-6">
                  <h2 className="text-2xl font-bold text-white tracking-tight mb-2">Create your account</h2>
                  <p className="text-white/40 font-light">
                    Start monitoring your IoT devices in minutes
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

                <form onSubmit={handleSubmit} className="space-y-4">
                  {/* Name fields */}
                  <div className="grid grid-cols-2 gap-4">
                    <div className="relative">
                      <label
                        htmlFor="firstName"
                        className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                          focusedField === 'firstName' || formData.firstName
                            ? '-top-2.5 text-xs text-teal-400 bg-black px-2'
                            : 'top-3.5 text-white/30'
                        }`}
                      >
                        First Name
                      </label>
                      <input
                        id="firstName"
                        name="firstName"
                        type="text"
                        required
                        value={formData.firstName}
                        onChange={handleChange}
                        onFocus={() => setFocusedField('firstName')}
                        onBlur={() => setFocusedField(null)}
                        className="w-full px-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                      />
                    </div>
                    <div className="relative">
                      <label
                        htmlFor="lastName"
                        className={`absolute left-4 transition-all duration-200 pointer-events-none ${
                          focusedField === 'lastName' || formData.lastName
                            ? '-top-2.5 text-xs text-teal-400 bg-black px-2'
                            : 'top-3.5 text-white/30'
                        }`}
                      >
                        Last Name
                      </label>
                      <input
                        id="lastName"
                        name="lastName"
                        type="text"
                        required
                        value={formData.lastName}
                        onChange={handleChange}
                        onFocus={() => setFocusedField('lastName')}
                        onBlur={() => setFocusedField(null)}
                        className="w-full px-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                      />
                    </div>
                  </div>

                  {/* Username */}
                  <div className="relative">
                    <label
                      htmlFor="username"
                      className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                        focusedField === 'username' || formData.username
                          ? '-top-2.5 left-4 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Username
                    </label>
                    <div className="absolute left-3.5 top-4 text-white/30">
                      <User className="h-4 w-4" />
                    </div>
                    <input
                      id="username"
                      name="username"
                      type="text"
                      autoComplete="username"
                      required
                      value={formData.username}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('username')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full pl-10 pr-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                  </div>

                  {/* Email */}
                  <div className="relative">
                    <label
                      htmlFor="email"
                      className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                        focusedField === 'email' || formData.email
                          ? '-top-2.5 left-4 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Email address
                    </label>
                    <div className="absolute left-3.5 top-4 text-white/30">
                      <Mail className="h-4 w-4" />
                    </div>
                    <input
                      id="email"
                      name="email"
                      type="email"
                      autoComplete="email"
                      required
                      value={formData.email}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('email')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full pl-10 pr-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                  </div>

                  {/* Organization */}
                  <div className="relative">
                    <label
                      htmlFor="organizationName"
                      className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                        focusedField === 'organizationName' || formData.organizationName
                          ? '-top-2.5 left-4 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Organization (optional)
                    </label>
                    <div className="absolute left-3.5 top-4 text-white/30">
                      <Building2 className="h-4 w-4" />
                    </div>
                    <input
                      id="organizationName"
                      name="organizationName"
                      type="text"
                      value={formData.organizationName}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('organizationName')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full pl-10 pr-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                  </div>

                  {/* Password */}
                  <div className="relative">
                    <label
                      htmlFor="password"
                      className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                        focusedField === 'password' || formData.password
                          ? '-top-2.5 left-4 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Password
                    </label>
                    <div className="absolute left-3.5 top-4 text-white/30">
                      <Lock className="h-4 w-4" />
                    </div>
                    <input
                      id="password"
                      name="password"
                      type="password"
                      autoComplete="new-password"
                      required
                      value={formData.password}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('password')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full pl-10 pr-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                  </div>

                  {/* Confirm Password */}
                  <div className="relative">
                    <label
                      htmlFor="confirmPassword"
                      className={`absolute left-10 transition-all duration-200 pointer-events-none ${
                        focusedField === 'confirmPassword' || formData.confirmPassword
                          ? '-top-2.5 left-4 text-xs text-teal-400 bg-black px-2'
                          : 'top-3.5 text-white/30'
                      }`}
                    >
                      Confirm Password
                    </label>
                    <div className="absolute left-3.5 top-4 text-white/30">
                      <Lock className="h-4 w-4" />
                    </div>
                    <input
                      id="confirmPassword"
                      name="confirmPassword"
                      type="password"
                      autoComplete="new-password"
                      required
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      onFocus={() => setFocusedField('confirmPassword')}
                      onBlur={() => setFocusedField(null)}
                      className="w-full pl-10 pr-4 py-3.5 bg-white/[0.03] border border-white/[0.08] rounded-xl text-white placeholder-transparent focus:outline-none focus:border-teal-500/50 focus:ring-2 focus:ring-teal-500/20 focus:bg-white/[0.05] transition-all duration-200"
                    />
                    {passwordsMatch && (
                      <div className="absolute right-3.5 top-4 text-teal-400">
                        <Check className="h-4 w-4" />
                      </div>
                    )}
                  </div>

                  {/* Premium submit button */}
                  <button
                    type="submit"
                    disabled={loading}
                    className="group w-full relative overflow-hidden px-6 py-4 rounded-xl font-semibold text-white transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed mt-6"
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
                          Creating account...
                        </>
                      ) : (
                        <>
                          Create account
                          <ArrowRight className="h-4 w-4 group-hover:translate-x-1 transition-transform duration-200" />
                        </>
                      )}
                    </span>
                  </button>
                </form>

                {/* Sign in link */}
                <p className="mt-6 text-center text-white/40">
                  Already have an account?{' '}
                  <Link to="/login" className="text-teal-400 hover:text-teal-300 font-medium transition-colors">
                    Sign in
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
