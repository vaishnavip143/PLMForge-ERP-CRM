package com.plmforge.crm.repository;

import com.plmforge.crm.entity.Ticket;
import com.plmforge.crm.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    List<Ticket> findAllByOrderByCreatedAtDesc();

    List<Ticket> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<Ticket> findByPotentialCustomer_EmailIgnoreCaseOrderByCreatedAtDesc(String email);

    List<Ticket> findByAssignedTo_IdOrderByCreatedAtDesc(Long representativeId);

    List<Ticket> findByStatusInOrderByDueAtAsc(Collection<TicketStatus> statuses);

    long countByStatus(TicketStatus status);
}
