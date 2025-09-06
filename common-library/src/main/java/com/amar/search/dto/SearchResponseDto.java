package com.amar.search.dto;

import java.util.List;

public class SearchResponseDto {
    
    private List<ProductSearchDto> products;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
    private String query;
    private long searchDuration; // in milliseconds
    private boolean hasError;
    private String errorMessage;

    // Default constructor
    public SearchResponseDto() {}

    // Constructor for successful search
    public SearchResponseDto(List<ProductSearchDto> products, long totalElements, int totalPages, 
                           int currentPage, int size, String query, long searchDuration) {
        this.products = products;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.size = size;
        this.query = query;
        this.searchDuration = searchDuration;
        this.hasError = false;
    }

    // Constructor for error response
    public SearchResponseDto(String errorMessage, String query) {
        this.errorMessage = errorMessage;
        this.query = query;
        this.hasError = true;
        this.products = List.of();
        this.totalElements = 0;
        this.totalPages = 0;
        this.currentPage = 0;
        this.size = 0;
        this.searchDuration = 0;
    }

    // Static factory methods
    public static SearchResponseDto success(List<ProductSearchDto> products, long totalElements, 
                                          int totalPages, int currentPage, int size, 
                                          String query, long searchDuration) {
        return new SearchResponseDto(products, totalElements, totalPages, currentPage, size, query, searchDuration);
    }

    public static SearchResponseDto error(String errorMessage, String query) {
        return new SearchResponseDto(errorMessage, query);
    }

    // Getters and Setters
    public List<ProductSearchDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductSearchDto> products) {
        this.products = products;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getSearchDuration() {
        return searchDuration;
    }

    public void setSearchDuration(long searchDuration) {
        this.searchDuration = searchDuration;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "SearchResponseDto{" +
                "totalElements=" + totalElements +
                ", totalPages=" + totalPages +
                ", currentPage=" + currentPage +
                ", size=" + size +
                ", query='" + query + '\'' +
                ", searchDuration=" + searchDuration +
                ", hasError=" + hasError +
                ", productsCount=" + (products != null ? products.size() : 0) +
                '}';
    }
}