package com.example.repository;

import com.example.model.EventProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EventProposalRepository extends JpaRepository<EventProposal, Long> {
    List<EventProposal> findByDepartmentId(Long departmentId);
    List<EventProposal> findByStatus(String status);
    List<EventProposal> findByStatusIn(Collection<String> statuses, Sort sort);
    long countByStatus(String status);
    long countByStatusIn(Collection<String> statuses);
    long countByDepartmentId(Long departmentId);
    long countByDepartmentIdAndStatusIn(Long departmentId, Collection<String> statuses);

    long countByRoom_Id(Long roomId);

    long countByLocationIgnoreCase(String location);
}
