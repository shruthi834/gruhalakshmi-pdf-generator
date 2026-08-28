package com.example.pdfgen.dto;

import jakarta.validation.constraints.NotBlank;

public class PdfRequest {

    @NotBlank(message = "Date is required")
    private String date;

    @NotBlank(message = "Ration Card Number is required")
    private String rationCardNumber;

    @NotBlank(message = "Name is required")
    private String name;

    public PdfRequest() {
    }

    public PdfRequest(String date, String rationCardNumber, String name) {
        this.date = date;
        this.rationCardNumber = rationCardNumber;
        this.name = name;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRationCardNumber() {
        return rationCardNumber;
    }

    public void setRationCardNumber(String rationCardNumber) {
        this.rationCardNumber = rationCardNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
