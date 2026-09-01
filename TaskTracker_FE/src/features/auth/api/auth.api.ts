import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type {
  AuthResponse,
  LoginRequest,
  ResendOtpRequest,
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
