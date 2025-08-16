package com.smartstudy.service;

import com.smartstudy.model.Document;
import com.smartstudy.model.AnalyticsCounter;
import com.smartstudy.repository.DocumentRepository;
import com.smartstudy.repository.QuizRepository;
import com.smartstudy.repository.AnalyticsCounterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired(required = false)
    private QuizRepository quizRepository;

    @Autowired
    private AnalyticsCounterRepository analyticsCounterRepository;

    private AnalyticsCounter getOrCreateCounter() {
        return analyticsCounterRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> {
                    AnalyticsCounter c = new AnalyticsCounter();
                    return analyticsCounterRepository.save(c);
                });
    }

    public void incrementSearchCount() {
        AnalyticsCounter c = getOrCreateCounter();
        c.setTotalSearches(c.getTotalSearches() + 1);
        c.setUpdatedAt(java.time.LocalDateTime.now());
        analyticsCounterRepository.save(c);
    }

    public void incrementAiInteractions() {
        AnalyticsCounter c = getOrCreateCounter();
        c.setAiInteractions(c.getAiInteractions() + 1);
        c.setUpdatedAt(java.time.LocalDateTime.now());
        analyticsCounterRepository.save(c);
    }

    public Map<String, Object> getOverview(int activityPeriodDays, int metricsPeriodDays) {
        Map<String, Object> data = new HashMap<>();

        // Totals
        long totalDocuments = documentRepository.count();
        long totalQuizzes = quizRepository != null ? quizRepository.count() : 0L;
        AnalyticsCounter c = getOrCreateCounter();

        data.put("totalDocuments", totalDocuments);
        data.put("totalQuizzes", totalQuizzes);
        data.put("totalSearches", c.getTotalSearches());
        data.put("aiInteractions", c.getAiInteractions());

        // Activity data (uploads per day for activityPeriodDays)
        int[] activityData = new int[7];
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        List<Document> recent = documentRepository.findRecentDocuments(startDate.atStartOfDay());
        Map<LocalDate, Long> byDay = recent.stream().collect(Collectors.groupingBy(
                d -> d.getUploadDate().atZone(ZoneId.systemDefault()).toLocalDate(), Collectors.counting()));
        for (int i = 0; i < 7; i++) {
            LocalDate day = startDate.plusDays(i);
            activityData[i] = byDay.getOrDefault(day, 0L).intValue();
        }
        data.put("activityData", activityData);

        // Document types distribution
        Map<String, Integer> types = new HashMap<>();
        types.put("PDF", 0);
        types.put("DOC", 0);
        types.put("PPT", 0);
        types.put("ZIP", 0);
        types.put("OTHER", 0);
        List<Document> all = documentRepository.findAll();
        for (Document d : all) {
            String ft = (d.getFileType() != null ? d.getFileType().toLowerCase() : "");
            if (ft.contains("pdf")) types.put("PDF", types.get("PDF") + 1);
            else if (ft.contains("doc")) types.put("DOC", types.get("DOC") + 1);
            else if (ft.contains("ppt")) types.put("PPT", types.get("PPT") + 1);
            else if (ft.contains("zip")) types.put("ZIP", types.get("ZIP") + 1);
            else types.put("OTHER", types.get("OTHER") + 1);
        }
        data.put("documentTypes", types);

        // Extra basic metrics
        Long totalFileSize = documentRepository.getTotalFileSize();
        data.put("totalFileSize", totalFileSize != null ? totalFileSize : 0);

        return data;
    }
}
