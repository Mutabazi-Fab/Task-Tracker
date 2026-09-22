package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.AddTaskSourceEntryRequest;
import com.throughline.taskmanagement.dto.response.TaskSourceEntryResponse;
import com.throughline.taskmanagement.enums.TaskSource;

import java.util.List;

public interface TaskSourceEntryService {
    /** Open to any authenticated caller — everyone creating a task should see existing
     *  suggestions for the category they picked, same as Department's read access. */
    List<TaskSourceEntryResponse> getEntries(TaskSource source);

    /** Executive-or-above, enforced here. Idempotent: adding a label that already exists
     *  (case-insensitive) for this source just returns the existing entry instead of
     *  erroring or duplicating it. */
    TaskSourceEntryResponse addEntry(AddTaskSourceEntryRequest request);
}
