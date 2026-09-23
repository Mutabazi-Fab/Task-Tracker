import type { NavigateFunction } from 'react-router-dom'
import { ROUTES } from '../../app/routePaths'
import type { AuthResponse } from '../../types/auth.types'

/** After a successful login (or a TOTP verify/confirm), routes to the dashboard, TOTP
 *  enrollment, or a TOTP challenge depending on AuthResponse.status — shared by LoginPage,
 *  TotpSetupPage, and TotpVerifyPage since all three can land on any of these outcomes.
 *  remember carries the "Remember me" checkbox through to whichever TOTP page comes next,
 *  via router state, since that's a separate request from the original login. */
export function routeAfterAuthResponse(navigate: NavigateFunction, auth: AuthResponse, remember: boolean) {
  if (auth.status === 'TOTP_SETUP_REQUIRED') {
    navigate(ROUTES.totpSetup, {
      state: {
        pendingAuthToken: auth.pendingAuthToken,
        totpSecret: auth.totpSecret,
        totpQrCodeDataUri: auth.totpQrCodeDataUri,
        remember,
      },
    })
    return
  }
  if (auth.status === 'TOTP_REQUIRED') {
    navigate(ROUTES.totpVerify, { state: { pendingAuthToken: auth.pendingAuthToken, remember } })
    return
  }
  // AUTHENTICATED — never location.state?.from. That state is left over from
  // ProtectedRoute bouncing whoever was previously on this browser tab to /login when they
  // logged out; blindly reusing it here would send the NEXT person who logs in (a
  // different person entirely) straight to wherever the last person happened to be, rather
  // than a clean landing page.
  navigate(ROUTES.dashboard, { replace: true })
}
