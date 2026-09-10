package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.ChangeDepartmentHeadRequest;
import com.throughline.taskmanagement.dto.request.CreateDepartmentRequest;
import com.throughline.taskmanagement.dto.request.RenameDepartmentRequest;
import com.throughline.taskmanagement.dto.response.DepartmentResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.DuplicateResourceException;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.InvalidAssignmentException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.mapper.DepartmentMapper;
import com.throughline.taskmanagement.model.Department;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.repository.DepartmentRepository;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final PersonRepository personRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        if (departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Department name already exists: " + request.name());
        }

        Person createdBy = personRepository.findById(request.createdById())
                .orElseThrow(() -> new ResourceNotFoundException("createdById not found"));
        // Creating a department is the CEO's call too, not Super-Admin-only like the rest
        // of department administration (renaming, reassigning the head) — she's the one
        // handing whole-department work out in the first place, so standing up a new
        // department to receive it is hers to do. Super Admin still can, same as always.
        if (!Role.isAtLeastExecutive(createdBy.getRole())) {
            throw new ForbiddenActionException("Only an Executive or Super Admin can create a department.");
        }

        Person headDirector = requireDirectorPerson(request.headDirectorId());

        Department department = new Department();
        department.setName(request.name());
        department.setHeadDirector(headDirector);
        department.setCreatedBy(createdBy);

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll().stream().map(departmentMapper::toResponse).toList();
    }

    @Override
    public DepartmentResponse getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        return departmentMapper.toResponse(department);
    }

    @Override
    public DepartmentResponse renameDepartment(Long id, RenameDepartmentRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        requireSuperAdmin(changedBy, "Only a Super Admin can rename a department.");

        if (!department.getName().equals(request.name()) && departmentRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Department name already exists: " + request.name());
        }
        department.setName(request.name());

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    @Override
    public DepartmentResponse changeDepartmentHead(Long id, ChangeDepartmentHeadRequest request) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        Person changedBy = personRepository.findById(request.changedById())
                .orElseThrow(() -> new ResourceNotFoundException("changedById not found"));
        requireSuperAdmin(changedBy, "Only a Super Admin can change a department's head.");

        department.setHeadDirector(requireDirectorPerson(request.newHeadDirectorId()));

        return departmentMapper.toResponse(departmentRepository.save(department));
    }

    /** A department's head must actually hold DIRECTOR-or-above — heading a department
     *  with no real authority over it defeats the whole point of the tier. */
    private Person requireDirectorPerson(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResourceNotFoundException("headDirectorId not found"));
        if (!Role.isAtLeastDirector(person.getRole())) {
            throw new InvalidAssignmentException("A department's head must hold the Director role or above.");
        }
        return person;
    }

    private void requireSuperAdmin(Person person, String message) {
        if (person.getRole() != Role.SUPER_ADMIN) {
            throw new ForbiddenActionException(message);
        }
    }
}
