package com.throughline.taskmanagement.service;

import com.throughline.taskmanagement.dto.request.CreatePersonRequest;
import com.throughline.taskmanagement.dto.response.PersonResponse;
import com.throughline.taskmanagement.dto.response.PersonStatisticsResponse;
import com.throughline.taskmanagement.dto.response.PersonTaskHistoryResponse;
import com.throughline.taskmanagement.dto.response.PersonTeamStatisticsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** No assignToTeam here — team membership is exclusively managed through TeamService
 *  (addMember/removeMember), since a person can now belong to multiple teams at once. */
public interface PersonService {
    PersonResponse createPerson(CreatePersonRequest request);
    PersonResponse getPersonById(Long id);
    Page<PersonResponse> getAllPeople(Pageable pageable);
    PersonResponse updatePerson(Long id, CreatePersonRequest request);
    void deletePerson(Long id);
    PersonStatisticsResponse getPersonStatistics(Long personId);
    Page<PersonTaskHistoryResponse> getPersonTaskHistory(Long personId, Pageable pageable);

    /** This person's stats broken down per team they belong to, rather than one blended
     *  number — reused by getPersonStatistics and by DashboardService.globalSearch. */
    List<PersonTeamStatisticsResponse> getPersonTeamBreakdown(Long personId);
}
