import type { Role } from './person.types'

/** Body for POST /auth/login. */
export interface LoginRequest {
  email: string
  password: string
}

/** Body for POST /auth/totp/confirm — completes enrollment for an account that's never
 *  finished TOTP setup. */
export interface TotpConfirmRequest {
  pendingAuthToken: string
  code: string
}

/** Body for POST /auth/totp/verify — completes a login for an already-enrolled account.
 *  code may be a live 6-digit TOTP code or an unused recovery code. */
export interface TotpVerifyRequest {
  pendingAuthToken: string
  code: string
}

/** Body for both POST /auth/password-reset-requests/check and POST
 *  /auth/password-reset-requests. */
export interface PasswordResetEmailRequest {
  email: string
}

/** Response for POST /auth/password-reset-requests/check. Rate-limited server-side (5
 *  checks per email per 15 minutes) — this app deliberately tells the truth about whether
 *  an account exists (an internal/offline tool, not a public one), which is exactly why
 *  that guess limit exists. */
export interface CheckEmailResponse {
  exists: boolean
}

/** Response for POST /auth/password-reset-requests. 'ALREADY_PENDING' means this person
 *  already had an open request — the frontend should say so plainly rather than implying
 *  a fresh one just went out. */
export interface PasswordResetRequestOutcome {
  status: 'CREATED' | 'ALREADY_PENDING'
}

/** One of three shapes, discriminated by status — only one of (token) vs
 *  (pendingAuthToken [+ totpSecret/totpQrCodeDataUri]) is ever populated:
 *
 *  - 'AUTHENTICATED': token is set, fully logged in. recoveryCodes is set too, exactly
 *    once, the moment TOTP enrollment is first confirmed — show them prominently then,
 *    they're never shown again after this response.
 *  - 'TOTP_SETUP_REQUIRED': never finished TOTP enrollment. pendingAuthToken, totpSecret
 *    (manual-entry fallback), and totpQrCodeDataUri are set; token is null. Submit
 *    pendingAuthToken + a code to POST /auth/totp/confirm.
 *  - 'TOTP_REQUIRED': already enrolled, needs a code to finish logging in. Only
 *    pendingAuthToken is set; token is null. Submit it to POST /auth/totp/verify. */
export interface AuthResponse {
  token: string | null
  personId: number
  fullName: string
  email: string
  role: Role | null
  status: 'AUTHENTICATED' | 'TOTP_SETUP_REQUIRED' | 'TOTP_REQUIRED'
  pendingAuthToken: string | null
  totpSecret: string | null
  totpQrCodeDataUri: string | null
  recoveryCodes: string[] | null
}
