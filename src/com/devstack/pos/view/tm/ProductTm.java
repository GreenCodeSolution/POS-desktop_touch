package com.devstack.pos.view.tm;

public class ProductTm {
    private int code;
    private String description;
    private String category;

    public ProductTm() {
    }

    public ProductTm(int code, String description, String category) {
        this.code = code;
        this.description = description;
        this.category = category;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "ProductTm{" +
                "code=" + code +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}
