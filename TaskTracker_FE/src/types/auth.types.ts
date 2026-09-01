import type { Role } from './person.types'

/** Body for POST /auth/login. */
export interface LoginRequest {
  email: string
  password: string
}

/**
 * Body for POST /auth/signup. No role field — self-service signup always
 * creates a MEMBER; Director/Super Admin accounts are provisioned by someone
 * who already holds the right role.
 */
export interface SignupRequest {
  fullName: string
  email: string
  password: string
  jobTitle: string
  rank?: string
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
 * What POST /auth/login, /auth/signup, and /auth/verify-email all return. token is null
 * when signup succeeds but the email still needs OTP verification — emailVerified tells
 * the caller which case this is, rather than treating a null token as an error.
 */
export interface AuthResponse {
  token: string | null
  personId: number
  fullName: string
  email: string
  role: Role | null
  emailVerified: boolean
}
