package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.ChangeDepartmentHeadRequest;
import com.throughline.taskmanagement.dto.request.CreateDepartmentRequest;
import com.throughline.taskmanagement.dto.request.RenameDepartmentRequest;
import com.throughline.taskmanagement.dto.response.DepartmentActivityResponse;
import com.throughline.taskmanagement.dto.response.DepartmentResponse;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read access (list/detail) is open to any authenticated caller — a Director/Executive
 *  needs to see departments to pick one when creating a task. Creating a department is
 *  Executive-or-above (the CEO can stand a new one up herself); renaming and reassigning a
 *  department's head stay Super-Admin-only. Every write is enforced server-side in
 *  DepartmentServiceImpl, not just hidden client-side. Every "who's doing this" field is
 *  re-derived from the caller's real login, same pattern as every other controller in this
 *  app. */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;
    private final CurrentPersonResolver currentPersonResolver;

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        CreateDepartmentRequest verified = new CreateDepartmentRequest(request.name(), request.headDirectorId(), actorId);
        return new ResponseEntity<>(departmentService.createDepartment(verified), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponse> renameDepartment(
            @PathVariable Long id, @Valid @RequestBody RenameDepartmentRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        RenameDepartmentRequest verified = new RenameDepartmentRequest(request.name(), actorId);
        return ResponseEntity.ok(departmentService.renameDepartment(id, verified));
    }

    @PutMapping("/{id}/head")
    public ResponseEntity<DepartmentResponse> changeDepartmentHead(
            @PathVariable Long id, @Valid @RequestBody ChangeDepartmentHeadRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        ChangeDepartmentHeadRequest verified = new ChangeDepartmentHeadRequest(request.newHeadDirectorId(), actorId);
        return ResponseEntity.ok(departmentService.changeDepartmentHead(id, verified));
    }

    /** Executive-or-above only — see DepartmentService.deleteDepartment for the
     *  "must already be empty of teams/people" safety rule. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        departmentService.deleteDepartment(id, actorId);
        return ResponseEntity.noContent().build();
    }

    /** Director or Super Admin only — every department deletion, org-wide. A static path,
     *  so it's matched ahead of GET /{id} the same way TaskController's GET /activity is. */
    @GetMapping("/activity")
    public ResponseEntity<Page<DepartmentActivityResponse>> getDepartmentActivity(
            Pageable pageable, Authentication authentication) {
        Long requesterId = currentPersonResolver.resolveId(authentication);
        return ResponseEntity.ok(departmentService.getDepartmentActivity(requesterId, pageable));
    }
}
