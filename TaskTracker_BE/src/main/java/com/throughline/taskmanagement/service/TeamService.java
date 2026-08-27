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
    List<TeamMemberResponse> getTeamMembers(Long teamId);
    TeamResponse addMember(Long teamId, AddTeamMemberRequest request);
    TeamResponse removeMember(Long teamId, Long personId, RemoveTeamMemberRequest request);
    Page<TeamMembershipChangeResponse> getMembershipHistory(Long teamId, Pageable pageable);
    Page<TeamMembershipChangeResponse> getAllMembershipActivity(Pageable pageable);
    TeamStatisticsResponse getTeamStatistics(Long teamId);
    List<TaskListResponse> getTeamTasks(Long teamId);
}
