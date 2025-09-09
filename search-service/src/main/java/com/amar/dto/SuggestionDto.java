package com.amar.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for search suggestions/autocomplete
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuggestionDto {
    
    private String text;
    private SuggestionType type;
    private Integer count;
    private String imageUrl;
    
    public enum SuggestionType {
        PRODUCT, BRAND, CATEGORY
    }
    
    public SuggestionDto() {}
    
    public SuggestionDto(String text, SuggestionType type) {
        this.text = text;
        this.type = type;
    }
    
    public SuggestionDto(String text, SuggestionType type, Integer count) {
        this.text = text;
        this.type = type;
        this.count = count;
    }
    
    public SuggestionDto(String text, SuggestionType type, Integer count, String imageUrl) {
        this.text = text;
        this.type = type;
        this.count = count;
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public SuggestionType getType() {
        return type;
    }
    
    public void setType(SuggestionType type) {
        this.type = type;
    }
    
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    @Override
    public String toString() {
        return "SuggestionDto{" +
                "text='" + text + '\'' +
                ", type=" + type +
                ", count=" + count +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}