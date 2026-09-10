package com.throughline.taskmanagement.mapper;

import com.throughline.taskmanagement.dto.response.DepartmentResponse;
import com.throughline.taskmanagement.model.Department;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {
    public DepartmentResponse toResponse(Department department) {
        if (department == null) return null;
        return new DepartmentResponse(
                department.getId(),
                department.getName(),
                department.getHeadDirector() != null ? department.getHeadDirector().getFullName() : null,
                department.getHeadDirector() != null ? department.getHeadDirector().getId() : null,
                department.getTeams() != null ? department.getTeams().size() : 0,
                department.getCreatedBy() != null ? department.getCreatedBy().getFullName() : null,
                department.getCreatedAt()
        );
    }
}
