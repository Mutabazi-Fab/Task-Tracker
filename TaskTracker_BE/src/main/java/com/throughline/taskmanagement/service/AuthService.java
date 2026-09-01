package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.ForgotPasswordRequest;
import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.ResendOtpRequest;
import com.throughline.taskmanagement.dto.request.ResetPasswordRequest;
import com.throughline.taskmanagement.dto.request.SignupRequest;
import com.throughline.taskmanagement.dto.request.VerifyEmailRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.model.Person;

public interface AuthService {
    /** Returns a null token (emailVerified=false) if this email still needs OTP
     *  verification — a brand-new signup always does; a pre-existing/seeded record being
     *  claimed here may already be exempted, in which case this logs them straight in. */
    AuthResponse signup(SignupRequest request);

    AuthResponse login(LoginRequest request);

    /** Checks the code, marks the email verified, and logs them in (issues a token) —
     *  verifying and logging in are the same step from the user's point of view. */
    AuthResponse verifyEmail(VerifyEmailRequest request);

    /** Rate-limited — see AuthServiceImpl's cooldown check. */
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

    PersonResponse getCurrentPerson(String email);
}
