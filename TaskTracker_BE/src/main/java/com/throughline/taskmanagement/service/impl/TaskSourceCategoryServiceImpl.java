package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddTaskSourceCategoryRequest;
import com.throughline.taskmanagement.dto.response.TaskSourceCategoryResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.TaskSourceCategory;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskSourceCategoryRepository;
import com.throughline.taskmanagement.service.TaskSourceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskSourceCategoryServiceImpl implements TaskSourceCategoryService {

    private final TaskSourceCategoryRepository taskSourceCategoryRepository;
    private final PersonRepository personRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskSourceCategoryResponse> getCategories() {
        return taskSourceCategoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TaskSourceCategoryResponse addCategory(AddTaskSourceCategoryRequest request) {
        Person addedBy = personRepository.findById(request.addedById())
                .orElseThrow(() -> new ResourceNotFoundException("addedById not found"));
        if (!Role.isAtLeastExecutive(addedBy.getRole())) {
            throw new ForbiddenActionException("Only an Executive or Super Admin can add a new source category.");
        }

        String name = request.name().trim();
        TaskSourceCategory existing = taskSourceCategoryRepository.findByNameIgnoreCase(name).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        TaskSourceCategory category = new TaskSourceCategory();
        category.setName(name);
        category.setAddedBy(addedBy);

        return toResponse(taskSourceCategoryRepository.save(category));
    }

    private TaskSourceCategoryResponse toResponse(TaskSourceCategory category) {
        return new TaskSourceCategoryResponse(category.getId(), category.getName());
    }
}
