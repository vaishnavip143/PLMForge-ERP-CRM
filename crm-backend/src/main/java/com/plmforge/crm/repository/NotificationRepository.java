package com.plmforge.crm.repository;

import com.plmforge.crm.entity.Notification;
import com.plmforge.crm.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipient_IdAndReadFalseOrderByCreatedAtDesc(Long recipientId);

    Optional<Notification> findByIdAndRecipient_Id(Long id, Long recipientId);

    boolean existsByRecipient_IdAndTypeAndRelatedTicket_Id(Long recipientId,
                                                          NotificationType type,
                                                          Long ticketId);
}
