package com.library.model;

public class Book {

    private int bookId;
    private String isbn;
    private String title;
    private String publisher;
    private Integer publishYear;
    private int totalCopies;
    private int availableCopies;
    private String shelfLocation;
    private String authorsSummary;
    private String categoriesSummary;

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Integer getPublishYear() {
        return publishYear;
    }

    public void setPublishYear(Integer publishYear) {
        this.publishYear = publishYear;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public String getAuthorsSummary() {
        return authorsSummary;
    }

    public void setAuthorsSummary(String authorsSummary) {
        this.authorsSummary = authorsSummary;
    }

    public String getCategoriesSummary() {
        return categoriesSummary;
    }

    public void setCategoriesSummary(String categoriesSummary) {
        this.categoriesSummary = categoriesSummary;
    }

    @Override
    public String toString() {
        return title + " (" + isbn + ")";
    }
}
