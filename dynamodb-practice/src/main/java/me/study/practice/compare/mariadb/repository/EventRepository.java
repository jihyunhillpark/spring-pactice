package me.study.practice.compare.mariadb.repository;

import me.study.practice.compare.mariadb.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, String> {
}
