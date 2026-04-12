package com.example.ordernotification.repository;

import com.example.ordernotification.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByOrderIdOrderByReceivedAtDesc(String orderId);
    List<AuditLog> findAllByOrderByReceivedAtDesc();
}
