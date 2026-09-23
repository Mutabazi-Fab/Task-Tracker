import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  AuthResponse,
  CheckEmailResponse,
  LoginRequest,
  PasswordResetEmailRequest,
  PasswordResetRequestOutcome,
  TotpConfirmRequest,
  TotpVerifyRequest,
} from '../../../types/auth.types'
import type { Person } from '../../../types/person.types'

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.login(), request)
  return data
}

/** Completes TOTP enrollment. On success the response also carries the recovery codes —
 *  shown exactly once, never retrievable again after this call. */
export async function confirmTotpSetup(request: TotpConfirmRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.totpConfirm(), request)
  return data
}

/** Completes a login for an already-enrolled account — code may be a live TOTP code or an
 *  unused recovery code. */
export async function verifyTotp(request: TotpVerifyRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.totpVerify(), request)
  return data
}

/** Whether an account exists for this email — deliberately not silent (see
 *  CheckEmailResponse). Rate-limited server-side (5 per email per 15 minutes); a
 *  TooManyAttemptsException surfaces as a normal thrown ApiError. */
export async function checkEmailForPasswordReset(request: PasswordResetEmailRequest): Promise<CheckEmailResponse> {
  const { data } = await axiosClient.post<CheckEmailResponse>(endpoints.auth.passwordResetRequestCheck(), request)
  return data
}

/** Only reachable after checkEmailForPasswordReset already confirmed the account exists
 *  and the person explicitly confirmed they want to proceed. */
export async function createPasswordResetRequest(request: PasswordResetEmailRequest): Promise<PasswordResetRequestOutcome> {
  const { data } = await axiosClient.post<PasswordResetRequestOutcome>(endpoints.auth.passwordResetRequestCreate(), request)
  return data
}

/** Stateless JWT — nothing to invalidate server-side, this just exists for API symmetry. */
export async function logout(): Promise<void> {
  await axiosClient.post(endpoints.auth.logout())
}

/** Full profile (job title, rank, teams) for whoever the current token belongs to —
 *  AuthResponse itself only carries the bare minimum from login. */
export async function fetchCurrentPerson(): Promise<Person> {
  const { data } = await axiosClient.get<Person>(endpoints.auth.me())
  return data
}
