package com.example.coupons.controller;

import com.example.coupons.dto.*;
import com.example.coupons.model.Coupon;
import com.example.coupons.repository.CouponRepository;
import com.example.coupons.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class CouponController {
    private final CouponService service;
    private final CouponRepository repo;

    public CouponController(CouponService service, CouponRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    @PostMapping("/coupons")
    public ResponseEntity<Coupon> create(@RequestBody Coupon c) {
        return ResponseEntity.status(201).body(service.create(c));
    }

    @GetMapping("/coupons")
    public List<Coupon> list() { return service.list(); }

    @GetMapping("/coupons/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        return service.get(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Coupon payload) {
        try { return ResponseEntity.ok(service.update(id, payload)); }
        catch (NoSuchElementException ex) { return ResponseEntity.notFound().build(); }
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) { repo.deleteById(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/applicable-coupons")
    public ResponseEntity<?> applicable(@RequestBody CartDto cart) { return ResponseEntity.ok(service.applicable(cart)); }

    @PostMapping("/apply-coupon/{id}")
    public ResponseEntity<?> apply(@PathVariable String id, @RequestBody CartDto cart) {
        try {
            ApplyCouponResponse resp = service.apply(id, cart);
            return ResponseEntity.ok(resp);
        } catch (NoSuchElementException ex) {
            return ResponseEntity.status(404).body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
