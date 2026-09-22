package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.ForgotPasswordRequest;
import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.ResendOtpRequest;
import com.throughline.taskmanagement.dto.request.ResetPasswordRequest;
import com.throughline.taskmanagement.dto.request.SignUpRequest;
import com.throughline.taskmanagement.dto.request.VerifyEmailRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.model.Person;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    /** Checks the code, marks the email verified, and logs them in (issues a token) —
     *  verifying and logging in are the same step from the user's point of view.
     *  Legacy path only: accounts created before Super-Admin-only creation started
     *  the account out passwordless. A new account uses signUp below instead. */
    AuthResponse verifyEmail(VerifyEmailRequest request);

    /** Completes an account a Super Admin already created: checks the sign-up code, sets
     *  the password the person is choosing for themselves, marks the email verified, and
     *  logs them in (issues a token) in the same step. Rejects an account that already has
     *  a password (nothing left to sign up for). */
    AuthResponse signUp(SignUpRequest request);

    /** Rate-limited — see AuthServiceImpl's cooldown check. Shared by the legacy
     *  verify-email flow and the sign-up flow — both just need a fresh OTP resent. */
    void resendOtp(ResendOtpRequest request);

    /** Deliberately silent: whether or not this responds with anything real (unknown email,
     *  never-claimed account, still within the resend cooldown), the caller always gets the
     *  same "if an account exists, a code was sent" outcome — this endpoint can't be used to
     *  probe which emails are registered or already claimed. */
    void forgotPassword(ForgotPasswordRequest request);

    /** Checks the reset code, sets the new password, and logs them in (issues a token) —
     *  same "verifying is logging in" shape as verifyEmail. */
    AuthResponse resetPassword(ResetPasswordRequest request);

    /** Generates and emails a fresh reset code for an already-claimed account. Shared by the
     *  public forgotPassword flow above and by PersonService's Super-Admin-only "send this
     *  person a password reset" action — unlike forgotPassword, callers here already know
     *  the person exists and is claimed, so this doesn't re-check either. */
    void sendPasswordResetCode(Person person);

    /** Generates and emails a fresh sign-up code for a passwordless account — shared by
     *  PersonService's createPerson (the first send) and resendOtp above (a re-send). */
    void sendSignUpCode(Person person);

    PersonResponse getCurrentPerson(String email);
}
