package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.AddTaskSourceCategoryRequest;
import com.throughline.taskmanagement.dto.response.TaskSourceCategoryResponse;

import java.util.List;

public interface TaskSourceCategoryService {
    /** Open to any authenticated caller — everyone creating a task needs the full list for
     *  the Source dropdown, same as Department's read access. Alphabetical. */
    List<TaskSourceCategoryResponse> getCategories();

    /** Executive-or-above, enforced here. Idempotent: adding a name that already exists
     *  (case-insensitive) just returns the existing category instead of erroring or
     *  duplicating it. */
    TaskSourceCategoryResponse addCategory(AddTaskSourceCategoryRequest request);
}
