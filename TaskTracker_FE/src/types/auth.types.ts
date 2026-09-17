import type { Role } from './person.types'

/** Body for POST /auth/login. */
export interface LoginRequest {
  email: string
  password: string
}

/** Body for POST /auth/verify-email. */
export interface VerifyEmailRequest {
  email: string
  otp: string
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

/**
 * What POST /auth/login, /auth/verify-email, and /auth/reset-password all return. token is
 * null when a still-unverified legacy account needs OTP verification before it can log in
 * — emailVerified tells the caller which case this is, rather than treating a null token as
 * an error. Every account a Super Admin creates now starts emailVerified=true, so this only
 * matters for accounts that predate that.
 */
export interface AuthResponse {
  token: string | null
  personId: number
  fullName: string
  email: string
  role: Role | null
  emailVerified: boolean
}
