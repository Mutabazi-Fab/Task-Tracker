package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.model.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        if (team == null) {
            return null;
        }
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getTeamLeader() != null ? team.getTeamLeader().getFullName() : null,
                team.getTeamLeader() != null ? team.getTeamLeader().getId() : null,
                team.getMembers() != null ? team.getMembers().size() : 0
        );
    }
}
