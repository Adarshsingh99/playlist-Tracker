package com.example.playlisttracker.repository;

import com.example.playlisttracker.model.Feedback;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends MongoRepository<Feedback, String> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
}
