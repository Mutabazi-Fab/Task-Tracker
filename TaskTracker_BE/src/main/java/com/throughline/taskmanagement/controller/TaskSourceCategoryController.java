package com.throughline.taskmanagement.controller;

import com.throughline.taskmanagement.dto.request.AddTaskSourceCategoryRequest;
import com.throughline.taskmanagement.dto.response.TaskSourceCategoryResponse;
import com.throughline.taskmanagement.security.CurrentPersonResolver;
import com.throughline.taskmanagement.service.TaskSourceCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The Source dropdown's own category list (e.g. "Initiative", "Regulator") — read access
 *  open to any authenticated caller, adding a new one is Executive-or-above, enforced
 *  server-side in TaskSourceCategoryServiceImpl. addedById is never accepted from the
 *  client — always the caller's own real, logged-in identity. */
@RestController
@RequestMapping("/api/v1/task-source-categories")
@RequiredArgsConstructor
public class TaskSourceCategoryController {

    private final TaskSourceCategoryService taskSourceCategoryService;
    private final CurrentPersonResolver currentPersonResolver;

    @GetMapping
    public ResponseEntity<List<TaskSourceCategoryResponse>> getCategories() {
        return ResponseEntity.ok(taskSourceCategoryService.getCategories());
    }

    @PostMapping
    public ResponseEntity<TaskSourceCategoryResponse> addCategory(
            @Valid @RequestBody AddTaskSourceCategoryRequest request, Authentication authentication) {
        Long actorId = currentPersonResolver.resolveId(authentication);
        AddTaskSourceCategoryRequest verified = new AddTaskSourceCategoryRequest(request.name(), actorId);
        return new ResponseEntity<>(taskSourceCategoryService.addCategory(verified), HttpStatus.CREATED);
    }
}
