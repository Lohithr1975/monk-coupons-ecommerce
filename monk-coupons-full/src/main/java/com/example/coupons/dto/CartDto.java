package com.example.coupons.dto;

import java.util.List;

public class CartDto {
    private List<CartItemDto> items;
    public List<CartItemDto> getItems() { return items; }
    public void setItems(List<CartItemDto> items) { this.items = items; }
}
