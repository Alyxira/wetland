package com.example.demo.repository;

import com.example.demo.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    interface PostImageRow {
        Long getPostId();
        Long getUserId();
        String getTitle();
        String getContent();
        Integer getLikesCount();
        Integer getCommentsCount();
        java.time.LocalDateTime getCreatedAt();
        String getImagePath();
        Integer getImageSort();
    }

    List<Post> findByTagOrderByCreatedAtDesc(String tag);

    List<Post> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<Post> findAllByOrderByCreatedAtDesc();

    List<Post> findAllByOrderByLikesCountDesc();

    List<Post> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(String titleKeyword, String contentKeyword);

    @Query(value = """
        SELECT
          p.id AS postId,
          p.user_id AS userId,
          p.title,
          p.content,
          p.likes_count AS likesCount,
          p.comments_count AS commentsCount,
          p.created_at AS createdAt,
          pi.image_path AS imagePath
          ,pi.image_sort AS imageSort
        FROM posts p
        LEFT JOIN post_images pi
          ON p.id = pi.post_id
        WHERE p.id = :postId
        ORDER BY pi.image_sort ASC, pi.id ASC
        """, nativeQuery = true)
    List<PostImageRow> findPostImageRowsByPostId(@Param("postId") Long postId);

    @Query(value = """
        SELECT
          p.id AS postId,
          p.user_id AS userId,
          p.title,
          p.content,
          p.likes_count AS likesCount,
          p.comments_count AS commentsCount,
          p.created_at AS createdAt,
          pi.image_path AS imagePath
          ,pi.image_sort AS imageSort
        FROM posts p
        LEFT JOIN post_images pi
          ON p.id = pi.post_id
        WHERE p.id IN (:postIds)
        ORDER BY p.created_at DESC, pi.image_sort ASC, pi.id ASC
        """, nativeQuery = true)
    List<PostImageRow> findPostImageRowsByPostIds(@Param("postIds") List<Long> postIds);
}
