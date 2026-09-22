import type { Role } from './person.types'

/** Body for POST /auth/login. */
export interface LoginRequest {
  email: string
  password: string
}

/** Body for POST /auth/verify-email. Legacy path only, for accounts that predate
 *  Super-Admin-only creation — a new account uses SignUpRequest below instead. */
export interface VerifyEmailRequest {
  email: string
  otp: string
}

/** Body for POST /auth/sign-up. Completes an account a Super Admin already created: the
 *  code emailed to them plus the password they're choosing for themselves. */
export interface SignUpRequest {
  email: string
  otp: string
  newPassword: string
}

/** Body for POST /auth/resend-otp. */
export interface ResendOtpRequest {
  email: string
}

/** Body for POST /auth/forgot-password. Always resolves — the backend responds the same
 *  way whether or not the email is registered, so the frontend always shows the same
 *  generic "if an account exists, a code was sent" message. */
export interface ForgotPasswordRequest {
  email: string
}

/** Body for POST /auth/reset-password. On success, logs the person in the same way
 *  verify-email does. */
export interface ResetPasswordRequest {
  email: string
  code: string
  newPassword: string
}

/** What POST /auth/login, /auth/verify-email, and /auth/reset-password all return. token
 *  is null when a still-unverified legacy account needs OTP verification first —
 *  emailVerified tells the caller which case this is, not an error. */
export interface AuthResponse {
  token: string | null
  personId: number
  fullName: string
  email: string
  role: Role | null
  emailVerified: boolean
}
