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

export const axiosClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
})

axiosClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiErrorBody>) => {
    const body = error.response?.data
    const normalized: ApiError = {
      status: error.response?.status ?? 0,
      message: body?.message ?? error.message ?? 'Something went wrong.',
      fieldErrors: body?.fieldErrors,
    }
    return Promise.reject(normalized)
  },
)
