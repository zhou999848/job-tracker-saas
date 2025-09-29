package com.example.jobtracker.repository;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.dto.TenantDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByName(String name);

    Optional<Tenant> findById(UUID id);


    Page<Tenant> findAllByName(String q, Pageable pageable);

    @Query(
            value = """
            select new map(
              t.id as id,
              t.name as name,
              t.status as status,
              (select count(u.id) from User u where u.tenant = t) as memberCount
            )
            from Tenant t
            where (:q is null or :q = '' or t.name like concat('%', :q, '%'))
            """,
            countQuery = """
            select count(t)
            from Tenant t
            where (:q is null or :q = '' or t.name like concat('%', :q, '%'))
            """
    )
    Page<Map<String,Object>> findTenantAdminPage(@Param("q") String q, Pageable pageable);
}
