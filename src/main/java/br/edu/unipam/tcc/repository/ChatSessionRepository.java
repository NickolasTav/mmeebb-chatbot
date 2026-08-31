package br.edu.unipam.tcc.repository;

import br.edu.unipam.tcc.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    Optional<ChatSession> findByPhoneNumber(String phoneNumber);

    Optional<ChatSession> findByStudentId(UUID studentId);
}
