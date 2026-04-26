package com.example.demo.repository;

import com.example.demo.entity.WetlandFloraFauna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WetlandFloraFaunaRepository extends JpaRepository<WetlandFloraFauna, Long> {

    @Query(value = """
        SELECT
            f.id,
            f.wetland_id,
            f.name,
            f.description,
            f.image_path,
            f.created_time,
            f.active
        FROM wetland_flora_fauna f
        WHERE ',' + f.wetland_id + ',' LIKE '%,' + CAST(:wetlandId AS VARCHAR(20)) + ',%'
        ORDER BY f.created_time DESC
        """, nativeQuery = true)
    List<WetlandFloraFauna> findAllByRelatedWetlandIdOrderByCreatedTimeDesc(@Param("wetlandId") Long wetlandId);

    Optional<WetlandFloraFauna> findByIdAndActiveTrue(Long id);

    @Query("""
        SELECT f FROM WetlandFloraFauna f
        WHERE f.active = true
        AND (
            f.name LIKE %:keyword%
            OR f.description LIKE %:keyword%
        )
        ORDER BY f.createdTime DESC
        """)
    List<WetlandFloraFauna> searchByKeyword(@Param("keyword") String keyword);
}
