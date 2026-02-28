package com.devstack.pos.view.tm;

import javafx.scene.control.Button;

public class ProductDetailTm {
    private String code;
    private double qty; // Changed to double to support decimal quantities (quantity on hand)
    private double initialQty; // Initial quantity when batch was created
    private double sellingQty; // Quantity sold (initialQty - qty)
    private double sellingPrice;
    private double buyingPrice;
    private boolean discountAvailability;
    // private double showPrice; // Commented out - Show Price logic removed
    private String supplierName;
    private Button viewBarcode;
    private Button delete;

    public ProductDetailTm() {
    }

    public ProductDetailTm(String code, double qty, double initialQty, double sellingQty, double sellingPrice, double buyingPrice, boolean discountAvailability, String supplierName, Button viewBarcode, Button delete) {
        this.code = code;
        this.qty = qty;
        this.initialQty = initialQty;
        this.sellingQty = sellingQty;
        this.sellingPrice = sellingPrice;
        this.buyingPrice = buyingPrice;
        this.discountAvailability = discountAvailability;
        // this.showPrice = showPrice; // Commented out
        this.supplierName = supplierName;
        this.viewBarcode = viewBarcode;
        this.delete = delete;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getInitialQty() {
        return initialQty;
    }

    public void setInitialQty(double initialQty) {
        this.initialQty = initialQty;
    }

    public double getSellingQty() {
        return sellingQty;
    }

    public void setSellingQty(double sellingQty) {
        this.sellingQty = sellingQty;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public double getBuyingPrice() {
        return buyingPrice;
    }

    public void setBuyingPrice(double buyingPrice) {
        this.buyingPrice = buyingPrice;
    }

    public boolean isDiscountAvailability() {
        return discountAvailability;
    }

    public void setDiscountAvailability(boolean discountAvailability) {
        this.discountAvailability = discountAvailability;
    }

    // public double getShowPrice() {
    //     return showPrice;
    // }
    //
    // public void setShowPrice(double showPrice) {
    //     this.showPrice = showPrice;
    // }

    public String getSupplierName() {
        return supplierName != null ? supplierName : "";
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Button getViewBarcode() {
        return viewBarcode;
    }

    public void setViewBarcode(Button viewBarcode) {
        this.viewBarcode = viewBarcode;
    }

    public Button getDelete() {
        return delete;
    }

    public void setDelete(Button delete) {
        this.delete = delete;
    }
}
