package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;

import java.util.List;

public interface PersonService {
    PersonResponse createPerson(CreatePersonRequest request);
    PersonResponse getPersonById(Long id);
    List<PersonResponse> getAllPeople();
    PersonResponse updatePerson(Long id, CreatePersonRequest request);
    void deletePerson(Long id);
    PersonResponse assignToTeam(Long personId, Long teamId);
    PersonStatisticsResponse getPersonStatistics(Long personId);
    List<PersonTaskHistoryResponse> getPersonTaskHistory(Long personId);
}
