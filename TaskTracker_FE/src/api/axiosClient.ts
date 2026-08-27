import axios, { AxiosError } from 'axios'

/**
 * Shape of every error body the backend's GlobalExceptionHandler returns.
 * fieldErrors is only present on @Valid validation failures.
 */
export interface ApiErrorBody {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  fieldErrors?: Record<string, string>
}

/** Normalized error every query/mutation in the app can rely on. */
export interface ApiError {
  status: number
  message: string
  fieldErrors?: Record<string, string>
}

/** Read directly rather than through AuthContext — axiosClient is a plain module with no
 *  access to React context, and importing AuthContext here would be a circular dependency
 *  (AuthContext's login/signup calls go through this same client). */
export const AUTH_TOKEN_STORAGE_KEY = 'throughline-auth-token'

/** Dispatched on any 401 response so AuthProvider (which owns the actual auth state) can
 *  clear itself and send the user back to /login, without axiosClient needing to import
 *  React Router or AuthContext. */
export const UNAUTHORIZED_EVENT = 'throughline:unauthorized'

export const axiosClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

axiosClient.interceptors.request.use((config) => {
  try {
    const token = localStorage.getItem(AUTH_TOKEN_STORAGE_KEY)
    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }
  } catch {
    // localStorage unavailable — request just goes out unauthenticated.
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    const body = error.response?.data
    const status = error.response?.status ?? 0

    if (status === 401) {
      window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT))
    }

    const normalized: ApiError = {
      status,
      message: body?.message ?? error.message ?? 'Something went wrong.',
      fieldErrors: body?.fieldErrors,
    }
    return Promise.reject(normalized)
  },
)
