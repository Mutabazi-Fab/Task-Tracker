package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.AddTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.dto.request.RemoveTeamMemberRequest;
import com.throughline.taskmanagement.dto.request.SetTeamLeaderRequest;
import com.throughline.taskmanagement.dto.request.UpdateTeamRequest;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TeamMemberResponse;
import com.throughline.taskmanagement.dto.response.TeamMembershipChangeResponse;
import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.dto.response.TeamStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeamService {
    TeamResponse createTeam(CreateTeamRequest request);
    TeamResponse getTeamById(Long id);
    List<TeamResponse> getAllTeams();
    TeamResponse updateTeam(Long id, UpdateTeamRequest request);
    void deleteTeam(Long id);
    TeamResponse setTeamLeader(Long teamId, Long personId, SetTeamLeaderRequest request);

    /** viewerId must be a Director/Super Admin, or a member of this team — everyone else
     *  is forbidden, not just shown less. */
    List<TeamMemberResponse> getTeamMembers(Long teamId, Long viewerId);

    TeamResponse addMember(Long teamId, AddTeamMemberRequest request);
    TeamResponse removeMember(Long teamId, Long personId, RemoveTeamMemberRequest request);

    /** Same viewer rule as getTeamMembers. */
    Page<TeamMembershipChangeResponse> getMembershipHistory(Long teamId, Long viewerId, Pageable pageable);

    /** Director/Super-Admin-only — the cross-team audit feed. */
    Page<TeamMembershipChangeResponse> getAllMembershipActivity(Long viewerId, Pageable pageable);

    /** Same viewer rule as getTeamMembers. */
    TeamStatisticsResponse getTeamStatistics(Long teamId, Long viewerId);

    /** Same viewer rule as getTeamMembers. */
    List<TaskListResponse> getTeamTasks(Long teamId, Long viewerId);
}
