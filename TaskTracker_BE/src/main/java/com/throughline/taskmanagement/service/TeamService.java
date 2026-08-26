package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.CreateTeamRequest;
import com.throughline.taskmanagement.dto.response.TaskListResponse;
import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.dto.response.TeamStatisticsResponse;

import java.util.List;

public interface TeamService {
    TeamResponse createTeam(CreateTeamRequest request);
    TeamResponse getTeamById(Long id);
    List<TeamResponse> getAllTeams();
    TeamResponse updateTeam(Long id, CreateTeamRequest request);
    void deleteTeam(Long id);
    TeamResponse setTeamLeader(Long teamId, Long personId);
    TeamResponse addMember(Long teamId, Long personId);
    TeamResponse removeMember(Long teamId, Long personId);
    TeamStatisticsResponse getTeamStatistics(Long teamId);
    List<TaskListResponse> getTeamTasks(Long teamId);
}
