import { axiosClient } from '../../../api/axiosClient'
import { endpoints } from '../../../api/endpoints'
import type { AuthResponse, LoginRequest, SignupRequest } from '../../../types/auth.types'
import type { Person } from '../../../types/person.types'

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.login(), request)
  return data
}

export async function signup(request: SignupRequest): Promise<AuthResponse> {
  const { data } = await axiosClient.post<AuthResponse>(endpoints.auth.signup(), request)
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
