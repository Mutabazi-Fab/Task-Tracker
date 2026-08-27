package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.TeamResponse;
import com.throughline.taskmanagement.model.Team;
import com.throughline.taskmanagement.model.TeamMember;
import org.springframework.stereotype.Component;

@Component
public class TeamMapper {

    public TeamResponse toResponse(Team team) {
        if (team == null) {
            return null;
        }

        TeamMember leader = team.getMembers() == null ? null : team.getMembers().stream()
                .filter(TeamMember::isLeader)
                .findFirst()
                .orElse(null);

        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getCreatedBy() != null ? team.getCreatedBy().getFullName() : null,
                team.getCreatedBy() != null ? team.getCreatedBy().getId() : null,
                leader != null ? leader.getPerson().getFullName() : null,
                leader != null ? leader.getPerson().getId() : null,
                team.getMembers() != null ? team.getMembers().size() : 0,
                team.getCreatedAt()
        );
    }
}
