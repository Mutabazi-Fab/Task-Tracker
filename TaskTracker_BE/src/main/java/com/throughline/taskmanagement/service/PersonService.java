package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.ChangeRoleRequest;
import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.request.DismissPasswordResetRequestRequest;
import com.throughline.taskmanagement.dto.request.ResetTotpRequest;
import com.throughline.taskmanagement.dto.request.SetAccountActiveRequest;
import com.throughline.taskmanagement.dto.request.SetPasswordRequest;
import com.throughline.taskmanagement.dto.response.AccountStatusChangeResponse;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.PersonTeamStatisticsResponse;
import com.throughline.taskmanagement.dto.response.RoleChangeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** No assignToTeam here — team membership is exclusively managed through TeamService
 *  (addMember/removeMember), since a person can now belong to multiple teams at once. */
public interface PersonService {
    /** createdById must be a Director or Super Admin; only a Super Admin may set role to
     *  anything other than Member (null defaults to Member). Sends a best-effort invite
     *  email. */
    PersonResponse createPerson(CreatePersonRequest request);

    /** A Director/Super Admin can view anyone; anyone else can only view themself or a
     *  teammate (someone who shares at least one team with them) — forbidden otherwise,
     *  not just hidden by the frontend. Same rule for getPersonStatistics and
     *  getPersonTaskHistory below. */
    PersonResponse getPersonById(Long id, Long viewerId);

    /** A Director/Super Admin sees everyone; anyone else sees only people who share at
     *  least one team with them (nothing at all if they belong to no team). */
    Page<PersonResponse> getAllPeople(Long viewerId, Pageable pageable);

    PersonResponse updatePerson(Long id, CreatePersonRequest request);
    void deletePerson(Long id);
    PersonStatisticsResponse getPersonStatistics(Long personId, Long viewerId);
    Page<PersonTaskHistoryResponse> getPersonTaskHistory(Long personId, Long viewerId, Pageable pageable);

    /** This person's stats broken down per team they belong to, rather than one blended
     *  number — reused by getPersonStatistics and by DashboardService.globalSearch. */
    List<PersonTeamStatisticsResponse> getPersonTeamBreakdown(Long personId);

    /** Super-Admin-only. Guards against removing the last Super Admin. */
    PersonResponse changeRole(Long personId, ChangeRoleRequest request);

    /** Super-Admin-only, and not on your own account if you're deactivating it. Guards
     *  against deactivating the last Super Admin. */
    PersonResponse setActive(Long personId, SetAccountActiveRequest request);

    /** Super-Admin-only — every role change ever made, org-wide, newest first. */
    Page<RoleChangeResponse> getRoleChangeActivity(Long requesterId, Pageable pageable);

    /** Super-Admin-only — every account activation/deactivation ever made, org-wide,
     *  newest first. */
    Page<AccountStatusChangeResponse> getAccountStatusChangeActivity(Long requesterId, Pageable pageable);

    /** Super-Admin-only. Sets the password directly — no code, no email, entirely
     *  offline. Never touches totpSecret/totpEnabledAt (password and TOTP stay two
     *  independent Super-Admin actions). If this person has a PENDING PasswordResetRequest,
     *  it's auto-marked FULFILLED as a side effect of resolving it this way. */
    void setPasswordDirectly(Long personId, SetPasswordRequest request);

    /** Super-Admin-only. Dismisses a PENDING PasswordResetRequest without changing the
     *  person's password — for a mistaken/duplicate/already-otherwise-resolved request. */
    void dismissPasswordResetRequest(Long personId, DismissPasswordResetRequestRequest request);

    /** Super-Admin-only — for a lost/replaced phone. Clears totpSecret/totpEnabledAt and
     *  every recovery code; the person's next login re-enters TOTP enrollment from scratch
     *  with a fresh QR code. Fails if this person was never enrolled in the first place. */
    void resetTotp(Long personId, ResetTotpRequest request);

    /** Self-only — a purely personal "what I'm focused on today" pointer, up to 3 at once.
     *  taskId must already be one of this person's own assigned tasks. Rejects a 4th with a
     *  clear message rather than auto-evicting the oldest — the caller removes one first. */
    PersonStatisticsResponse addDailyGoal(Long personId, Long taskId, Long actorId);

    /** Self-only. Silently fine if the task wasn't a daily goal to begin with. */
    PersonStatisticsResponse removeDailyGoal(Long personId, Long taskId, Long actorId);
}
