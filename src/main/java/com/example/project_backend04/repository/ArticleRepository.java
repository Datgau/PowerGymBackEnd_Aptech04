package com.example.project_backend04.repository;

import com.example.project_backend04.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    List<Article> findByIsPublishedTrueOrderByPublishedDateDesc();
    
    List<Article> findByCategoryAndIsPublishedTrueOrderByPublishedDateDesc(Article.ArticleCategory category);
    
    List<Article> findByIsFeaturedTrueAndIsPublishedTrueOrderByPublishedDateDesc();
    
    @Query("SELECT a FROM Article a WHERE a.isPublished = true AND (a.title LIKE %:keyword% OR a.description LIKE %:keyword% OR a.content LIKE %:keyword%) ORDER BY a.publishedDate DESC")
    List<Article> findByKeywordAndIsPublishedTrue(@Param("keyword") String keyword);
    
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t = :tag AND a.isPublished = true ORDER BY a.publishedDate DESC")
    List<Article> findByTagAndIsPublishedTrue(@Param("tag") String tag);
    
    Optional<Article> findByArticleIdAndIsPublishedTrue(String articleId);
    
    @Query("SELECT a FROM Article a WHERE a.author = :author AND a.isPublished = true ORDER BY a.publishedDate DESC")
    List<Article> findByAuthorAndIsPublishedTrue(@Param("author") String author);
    
    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);
    
    @Query("SELECT a FROM Article a WHERE a.isPublished = true ORDER BY a.viewCount DESC")
    List<Article> findMostViewedArticles();
    
    @Query("SELECT DISTINCT a.author FROM Article a WHERE a.isPublished = true AND a.author IS NOT NULL ORDER BY a.author ASC")
    List<String> findAllAuthors();
    
    @Query("SELECT DISTINCT t FROM Article a JOIN a.tags t WHERE a.isPublished = true ORDER BY t ASC")
    List<String> findAllTags();
}