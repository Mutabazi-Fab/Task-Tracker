package com.throughline.taskmanagement.service.impl;

import com.throughline.taskmanagement.dto.request.AddTaskSourceEntryRequest;
import com.throughline.taskmanagement.dto.response.TaskSourceEntryResponse;
import com.throughline.taskmanagement.enums.Role;
import com.throughline.taskmanagement.enums.TaskSource;
import com.throughline.taskmanagement.exception.ForbiddenActionException;
import com.throughline.taskmanagement.exception.ResourceNotFoundException;
import com.throughline.taskmanagement.model.Person;
import com.throughline.taskmanagement.model.TaskSourceEntry;
import com.throughline.taskmanagement.repository.PersonRepository;
import com.throughline.taskmanagement.repository.TaskSourceEntryRepository;
import com.throughline.taskmanagement.service.TaskSourceEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TaskSourceEntryServiceImpl implements TaskSourceEntryService {

    private final TaskSourceEntryRepository taskSourceEntryRepository;
    private final PersonRepository personRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskSourceEntryResponse> getEntries(TaskSource source) {
        return taskSourceEntryRepository.findBySourceOrderByLabelAsc(source).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TaskSourceEntryResponse addEntry(AddTaskSourceEntryRequest request) {
        Person addedBy = personRepository.findById(request.addedById())
                .orElseThrow(() -> new ResourceNotFoundException("addedById not found"));
        if (!Role.isAtLeastExecutive(addedBy.getRole())) {
            throw new ForbiddenActionException("Only an Executive or Super Admin can add a new source entry.");
        }

        String label = request.label().trim();
        TaskSourceEntry existing = taskSourceEntryRepository
                .findBySourceAndLabelIgnoreCase(request.source(), label)
                .orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        TaskSourceEntry entry = new TaskSourceEntry();
        entry.setSource(request.source());
        entry.setLabel(label);
        entry.setAddedBy(addedBy);

        return toResponse(taskSourceEntryRepository.save(entry));
    }

    private TaskSourceEntryResponse toResponse(TaskSourceEntry entry) {
        return new TaskSourceEntryResponse(entry.getId(), entry.getSource(), entry.getLabel());
    }
}
