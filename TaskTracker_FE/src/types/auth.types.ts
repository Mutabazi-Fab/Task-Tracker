import type { Role } from './person.types'

/** Body for POST /auth/login. */
export interface LoginRequest {
  email: string
  password: string
}

/**
 * Body for POST /auth/signup. No role field — self-service signup always
 * creates a MEMBER; Director accounts are provisioned out of band.
 */
export interface SignupRequest {
  fullName: string
  email: string
  password: string
  jobTitle: string
  rank?: string
}

/** What POST /auth/login and /auth/signup both return. */
export interface AuthResponse {
  token: string
  personId: number
  fullName: string
  email: string
  role: Role | null
}
