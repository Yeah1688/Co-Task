package com.cotask.repository;
import com.cotask.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, String> {
    Optional<Workspace> findByInviteCode(String inviteCode);
}
