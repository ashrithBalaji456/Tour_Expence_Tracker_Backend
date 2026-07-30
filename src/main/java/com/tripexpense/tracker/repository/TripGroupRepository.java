package com.tripexpense.tracker.repository;

import com.tripexpense.tracker.entity.TripGroup;
import com.tripexpense.tracker.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripGroupRepository extends JpaRepository<TripGroup, Long> {
    List<TripGroup> findByCreator(User creator);
    
    @Query("SELECT tg FROM TripGroup tg WHERE LOWER(tg.creator.username) = LOWER(:username) OR LOWER(:username) IN (SELECT LOWER(m) FROM tg.memberUsernames m) OR LOWER(:email) IN (SELECT LOWER(m) FROM tg.memberUsernames m)")
    List<TripGroup> findAssociatedGroups(@Param("username") String username, @Param("email") String email);
}
