package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.ChangeDepartmentHeadRequest;
import com.throughline.taskmanagement.dto.request.CreateDepartmentRequest;
import com.throughline.taskmanagement.dto.request.RenameDepartmentRequest;
import com.throughline.taskmanagement.dto.response.DepartmentActivityResponse;
import com.throughline.taskmanagement.dto.response.DepartmentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DepartmentService {
    /** Executive-or-above, enforced here — a Director cannot, but the CEO who hands work
     *  out to whole departments can stand a new one up herself, same as Super Admin. */
    DepartmentResponse createDepartment(CreateDepartmentRequest request);

    /** Anyone authenticated can read the department list/detail — read access is
     *  intentionally open to Director/Executive, only writes are Super-Admin-gated. */
    List<DepartmentResponse> getAllDepartments();

    DepartmentResponse getDepartmentById(Long id);

    /** Super-Admin-only. */
    DepartmentResponse renameDepartment(Long id, RenameDepartmentRequest request);

    /** Super-Admin-only. newHeadDirectorId must already hold the DIRECTOR role or above. */
    DepartmentResponse changeDepartmentHead(Long id, ChangeDepartmentHeadRequest request);

    /** Executive-or-above, same tier as createDepartment — the CEO who can stand a department up can take
     *  one down too, not just Super Admin. */
    void deleteDepartment(Long id, Long actorId);

    /** Director or Super Admin only — every department deletion, org-wide. Same visibility
     *  tier as TaskService.getTaskActivity. */
    Page<DepartmentActivityResponse> getDepartmentActivity(Long requesterId, Pageable pageable);
}
