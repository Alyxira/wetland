package com.example.demo.repository;

import com.example.demo.entity.WetlandInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WetlandInfoRepository extends JpaRepository<WetlandInfo, Long> {

    Optional<WetlandInfo> findByWetlandName(String wetlandName);

    List<WetlandInfo> findByTagsContainingAndActiveTrueOrderByCreatedTimeDesc(String tag);

    List<WetlandInfo> findByWetlandNameContainingAndActiveTrueOrderByCreatedTimeDesc(String keyword);

    boolean existsByWetlandName(String wetlandName);

    @Query("SELECT w FROM WetlandInfo w WHERE w.tags LIKE %:tag% AND w.wetlandName LIKE %:nameKeyword% AND w.active = true")
    List<WetlandInfo> findByTagAndNameKeyword(@Param("tag") String tag, @Param("nameKeyword") String nameKeyword);

    @Query("SELECT w FROM WetlandInfo w WHERE w.active = true ORDER BY w.createdTime DESC")
    List<WetlandInfo> findAllActiveWetlands();

    List<WetlandInfo> findByCoordinateRangeContainingAndActiveTrueOrderByCreatedTimeDesc(String coordinateKeyword);

    @Query("""
        SELECT w FROM WetlandInfo w
        WHERE w.active = true
        AND (
            w.wetlandName LIKE %:keyword%
            OR w.tags LIKE %:keyword%
            OR w.coordinateRange LIKE %:keyword%
            OR w.description LIKE %:keyword%
            OR w.floraFaunaInfo LIKE %:keyword%
        )
        ORDER BY w.createdTime DESC
        """)
    List<WetlandInfo> searchByKeyword(@Param("keyword") String keyword);
}
