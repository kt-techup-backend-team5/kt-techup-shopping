package com.kt.repository.visitstat;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kt.domain.visitstat.VisitStat;

public interface VisitStatRepository extends JpaRepository<VisitStat, Long> {

}
