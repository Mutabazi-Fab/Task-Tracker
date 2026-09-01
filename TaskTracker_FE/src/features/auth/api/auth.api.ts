import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  AuthResponse,
  ForgotPasswordRequest,
  LoginRequest,
  ResendOtpRequest,
  ResetPasswordRequest,
  SignupRequest,
  VerifyEmailRequest,
} from '../../../types/auth.types'
import type { Person } from '../../../types/person.types'

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.login(), request)
  return data
}

/** May come back with a null token — see AuthResponse.emailVerified. */
export async function signup(request: SignupRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.signup(), request)
  return data
}

/** Checks the code and, on success, logs the person in (issues a real token) in the same step. */
export async function verifyEmail(request: VerifyEmailRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.verifyEmail(), request)
  return data
}

/** Rate-limited server-side — a resend before the cooldown elapses is rejected. */
export async function resendOtp(request: ResendOtpRequest): Promise<void> {
  await axiosClient.post(endpoints.auth.resendOtp(), request)
}

/** Always resolves regardless of whether the email is registered — see
 *  ForgotPasswordRequest. Never throws for "unknown email". */
export async function forgotPassword(request: ForgotPasswordRequest): Promise<void> {
  await axiosClient.post(endpoints.auth.forgotPassword(), request)
}

/** Checks the reset code and, on success, logs the person in (issues a real token) in the
 *  same step — same shape as verifyEmail. */
export async function resetPassword(request: ResetPasswordRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.resetPassword(), request)
  return data
}

/** Stateless JWT — nothing to invalidate server-side, this just exists for API symmetry. */
export async function logout(): Promise<void> {
  await axiosClient.post(endpoints.auth.logout())
}

/** Full profile (job title, rank, teams) for whoever the current token belongs to —
 *  AuthResponse itself only carries the bare minimum from login/signup. */
export async function fetchCurrentPerson(): Promise<Person> {
  const { data } = await axiosClient.get<Person>(endpoints.auth.me())
  return data
}
