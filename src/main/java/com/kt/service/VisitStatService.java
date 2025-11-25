package com.kt.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kt.domain.visitstat.VisitStat;
import com.kt.repository.visitstat.VisitStatRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class VisitStatService {
	private final VisitStatRepository visitStatRepository;

	public void create(Long userId, String ip, String userAgent) {
		visitStatRepository.save(new VisitStat(ip, userAgent, userId));
	}
}