package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.LoginRequest;
import com.throughline.taskmanagement.dto.request.ResendOtpRequest;
import com.throughline.taskmanagement.dto.request.SignupRequest;
import com.throughline.taskmanagement.dto.request.VerifyEmailRequest;
import com.throughline.taskmanagement.dto.response.AuthResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;

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

    PersonResponse getCurrentPerson(String email);
}
