package com.cinema.testcinema.dto;

import java.util.List;

public class AdminAnalyticsDto {

    private long totalViews;
    private long totalUsers;
    private List<ViewsTrendItem> viewsTrend;
    private List<LanguageCount> subtitleLanguages;
    private DomesticVsGlobal domesticVsGlobal;
    private List<SearchQueryItem> topSearchQueries;

    public long getTotalViews() { return totalViews; }
    public void setTotalViews(long totalViews) { this.totalViews = totalViews; }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public List<ViewsTrendItem> getViewsTrend() { return viewsTrend; }
    public void setViewsTrend(List<ViewsTrendItem> viewsTrend) { this.viewsTrend = viewsTrend; }

    public List<LanguageCount> getSubtitleLanguages() { return subtitleLanguages; }
    public void setSubtitleLanguages(List<LanguageCount> subtitleLanguages) { this.subtitleLanguages = subtitleLanguages; }

    public DomesticVsGlobal getDomesticVsGlobal() { return domesticVsGlobal; }
    public void setDomesticVsGlobal(DomesticVsGlobal domesticVsGlobal) { this.domesticVsGlobal = domesticVsGlobal; }

    public List<SearchQueryItem> getTopSearchQueries() { return topSearchQueries; }
    public void setTopSearchQueries(List<SearchQueryItem> topSearchQueries) { this.topSearchQueries = topSearchQueries; }

    public static class ViewsTrendItem {
        private String date;
        private long count;

        public ViewsTrendItem(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() { return date; }
        public long getCount() { return count; }
    }

    public static class LanguageCount {
        private String language;
        private long count;

        public LanguageCount(String language, long count) {
            this.language = language;
            this.count = count;
        }

        public String getLanguage() { return language; }
        public long getCount() { return count; }
    }

    public static class DomesticVsGlobal {
        private long domestic;
        private long global;

        public DomesticVsGlobal(long domestic, long global) {
            this.domestic = domestic;
            this.global = global;
        }

        public long getDomestic() { return domestic; }
        public long getGlobal() { return global; }
    }

    public static class SearchQueryItem {
        private String query;
        private int count;

        public SearchQueryItem(String query, int count) {
            this.query = query;
            this.count = count;
        }

        public String getQuery() { return query; }
        public int getCount() { return count; }
    }
}
