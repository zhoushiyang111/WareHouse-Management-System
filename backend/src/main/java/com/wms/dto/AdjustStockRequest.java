package com.wms.dto;

import javax.validation.constraints.Min;

public class AdjustStockRequest {
  @Min(1)
  private int quantity;

  private String note;

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}

