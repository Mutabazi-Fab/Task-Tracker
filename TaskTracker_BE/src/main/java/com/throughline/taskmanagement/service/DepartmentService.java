package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.ChangeDepartmentHeadRequest;
import com.throughline.taskmanagement.dto.request.CreateDepartmentRequest;
import com.throughline.taskmanagement.dto.request.RenameDepartmentRequest;
import com.throughline.taskmanagement.dto.response.DepartmentResponse;

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
}
