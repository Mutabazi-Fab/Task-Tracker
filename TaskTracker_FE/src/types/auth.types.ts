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
