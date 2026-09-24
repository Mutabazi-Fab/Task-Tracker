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

/** Response for POST /auth/password-reset-requests/check. */
export interface CheckEmailResponse {
  exists: boolean
}

/** Response for POST /auth/password-reset-requests. */
export interface PasswordResetRequestOutcome {
  status: 'CREATED' | 'ALREADY_PENDING'
}

/** One of three shapes, discriminated by status — only one of (token) vs (pendingAuthToken [+
 *  totpSecret/totpQrCodeDataUri]) is ever populated: - 'AUTHENTICATED': token is set, fully logged in.
 *  recoveryCodes is set too, exactly once, the moment TOTP enrollment is first confirmed — show them
 *  prominently then, they're never shown again after this response. - 'TOTP_SETUP_REQUIRED': never
 *  finished TOTP enrollment. pendingAuthToken, totpSecret (manual-entry fallback), and
 *  totpQrCodeDataUri are set; token is null. */
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
