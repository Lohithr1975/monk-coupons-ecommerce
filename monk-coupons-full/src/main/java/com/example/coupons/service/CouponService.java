package com.example.coupons.service;

import com.example.coupons.dto.*;
import com.example.coupons.model.Coupon;
import com.example.coupons.repository.CouponRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponService {
    private final CouponRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public CouponService(CouponRepository repo) { this.repo = repo; }

    public Coupon create(Coupon coupon) { return repo.save(coupon); }
    public List<Coupon> list() { return repo.findAll(); }
    public Optional<Coupon> get(String id) { return repo.findById(id); }
    public Coupon update(String id, Coupon payload) {
        return repo.findById(id).map(existing -> {
            existing.setType(payload.getType());
            existing.setDetailsJson(payload.getDetailsJson());
            existing.setActive(payload.isActive());
            existing.setExpiresAt(payload.getExpiresAt());
            return repo.save(existing);
        }).orElseThrow(() -> new NoSuchElementException("Coupon not found"));
    }
    public void delete(String id) { repo.deleteById(id); }

    private double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private boolean isActive(Coupon c) {
        if (!c.isActive()) return false;
        if (c.getExpiresAt() != null) return c.getExpiresAt().isAfter(Instant.now());
        return true;
    }

    public List<ApplicableCouponDto> applicable(CartDto cart) {
        double total = cart.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        List<ApplicableCouponDto> out = new ArrayList<>();
        for (Coupon c : repo.findAll()) {
            if (!isActive(c)) continue;
            try {
                JsonNode d = mapper.readTree(c.getDetailsJson());
                if ("cart-wise".equals(c.getType())) {
                    double threshold = d.path("threshold").asDouble(0);
                    if (total >= threshold) {
                        if (d.has("percent")) {
                            double percent = d.path("percent").asDouble(0);
                            double discount = round2(total * percent / 100.0);
                            out.add(new ApplicableCouponDto(c.getId(), c.getType(), discount));
                        } else if (d.has("fixed")) {
                            double fixed = d.path("fixed").asDouble(0);
                            out.add(new ApplicableCouponDto(c.getId(), c.getType(), round2(Math.min(fixed, total))));
                        }
                    }
                } else if ("product-wise".equals(c.getType())) {
                    String pid = d.path("product_id").asText(null);
                    Optional<CartItemDto> match = cart.getItems().stream().filter(it -> pid != null && pid.equals(String.valueOf(it.getProductId()))).findFirst();
                    if (match.isPresent()) {
                        CartItemDto it = match.get();
                        if (d.has("percent")) {
                            double percent = d.path("percent").asDouble(0);
                            double discount = round2(it.getPrice() * it.getQuantity() * percent / 100.0);
                            out.add(new ApplicableCouponDto(c.getId(), c.getType(), discount));
                        } else if (d.has("fixed")) {
                            double fixed = d.path("fixed").asDouble(0);
                            out.add(new ApplicableCouponDto(c.getId(), c.getType(), round2(Math.min(fixed, it.getPrice() * it.getQuantity()))));
                        }
                    }
                } else if ("bxgy".equals(c.getType())) {
                    JsonNode buyArr = d.path("buy_products");
                    JsonNode getArr = d.path("get_products");
                    int repLimit = d.path("repetition_limit").asInt(1);
                    if (!buyArr.isArray() || !getArr.isArray()) continue;
                    int possible = Integer.MAX_VALUE;
                    for (JsonNode b : buyArr) {
                        String pid = b.path("product_id").asText();
                        int req = b.path("quantity").asInt(0);
                        int inCart = cart.getItems().stream().filter(it -> pid.equals(String.valueOf(it.getProductId()))).mapToInt(CartItemDto::getQuantity).sum();
                        if (req <= 0) { possible = 0; break; }
                        possible = Math.min(possible, inCart / req);
                    }
                    if (possible <= 0) continue;
                    possible = Math.min(possible, repLimit);
                    double discount = 0.0;
                    for (JsonNode g : getArr) {
                        String pid = g.path("product_id").asText();
                        int qtyPer = g.path("quantity").asInt(0);
                        int allowed = qtyPer * possible;
                        Optional<CartItemDto> cartIt = cart.getItems().stream().filter(it -> pid.equals(String.valueOf(it.getProductId()))).findFirst();
                        if (cartIt.isPresent()) {
                            int freeQty = Math.min(cartIt.get().getQuantity(), allowed);
                            discount += freeQty * cartIt.get().getPrice();
                        }
                    }
                    if (discount > 0) out.add(new ApplicableCouponDto(c.getId(), c.getType(), round2(discount)));
                }
            } catch (Exception ex) {
                // ignore malformed details
            }
        }
        return out;
    }

    public ApplyCouponResponse apply(String couponId, CartDto cart) {
        Coupon c = repo.findById(couponId).orElseThrow(() -> new NoSuchElementException("Coupon not found"));
        if (!isActive(c)) throw new IllegalStateException("Coupon inactive/expired");
        double total = cart.getItems().stream().mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();
        List<CartItemDto> itemsCopy = cart.getItems().stream().map(it -> {
            CartItemDto cpy = new CartItemDto();
            cpy.setProductId(it.getProductId());
            cpy.setPrice(it.getPrice());
            cpy.setQuantity(it.getQuantity());
            cpy.setTotalDiscount(0.0);
            return cpy;
        }).collect(Collectors.toList());
        try {
            JsonNode d = mapper.readTree(c.getDetailsJson());
            double totalDiscount = 0.0;
            if ("cart-wise".equals(c.getType())) {
                double threshold = d.path("threshold").asDouble(0);
                if (total < threshold) throw new IllegalStateException("Threshold not met");
                if (d.has("percent")) {
                    double percent = d.path("percent").asDouble(0);
                    totalDiscount = round2(total * percent / 100.0);
                    // distribute proportionally
                    double acc = 0.0;
                    for (CartItemDto it : itemsCopy) {
                        double share = (it.getPrice() * it.getQuantity()) / total;
                        double disc = round2(totalDiscount * share);
                        it.setTotalDiscount(disc);
                        acc += disc;
                    }
                    double diff = round2(totalDiscount - acc);
                    if (Math.abs(diff) > 0.001 && itemsCopy.size() > 0) {
                        itemsCopy.get(0).setTotalDiscount(round2(itemsCopy.get(0).getTotalDiscount() + diff));
                    }
                } else if (d.has("fixed")) {
                    double fixed = d.path("fixed").asDouble(0);
                    totalDiscount = round2(Math.min(fixed, total));
                    double acc = 0.0;
                    for (CartItemDto it : itemsCopy) {
                        double share = (it.getPrice() * it.getQuantity()) / total;
                        double disc = round2(totalDiscount * share);
                        it.setTotalDiscount(disc);
                        acc += disc;
                    }
                    double diff = round2(totalDiscount - acc);
                    if (Math.abs(diff) > 0.001 && itemsCopy.size() > 0) {
                        itemsCopy.get(0).setTotalDiscount(round2(itemsCopy.get(0).getTotalDiscount() + diff));
                    }
                } else throw new IllegalStateException("Invalid coupon details");
            } else if ("product-wise".equals(c.getType())) {
                String pid = d.path("product_id").asText(null);
                Optional<CartItemDto> match = itemsCopy.stream().filter(it -> pid != null && pid.equals(String.valueOf(it.getProductId()))).findFirst();
                if (!match.isPresent()) throw new IllegalStateException("Product not in cart");
                CartItemDto it = match.get();
                if (d.has("percent")) {
                    double percent = d.path("percent").asDouble(0);
                    double disc = round2(it.getPrice() * it.getQuantity() * percent / 100.0);
                    it.setTotalDiscount(disc);
                    totalDiscount = disc;
                } else if (d.has("fixed")) {
                    double fixed = d.path("fixed").asDouble(0);
                    double disc = round2(Math.min(fixed, it.getPrice() * it.getQuantity()));
                    it.setTotalDiscount(disc);
                    totalDiscount = disc;
                }
            } else if ("bxgy".equals(c.getType())) {
                JsonNode buyArr = d.path("buy_products");
                JsonNode getArr = d.path("get_products");
                int rep = d.path("repetition_limit").asInt(1);
                int possible = Integer.MAX_VALUE;
                for (JsonNode b : buyArr) {
                    String pid = b.path("product_id").asText();
                    int req = b.path("quantity").asInt(0);
                    int inCart = itemsCopy.stream().filter(it -> pid.equals(String.valueOf(it.getProductId()))).mapToInt(CartItemDto::getQuantity).sum();
                    if (req <= 0) { possible = 0; break; }
                    possible = Math.min(possible, inCart / req);
                }
                possible = Math.min(possible, rep);
                if (possible <= 0) throw new IllegalStateException("Buy conditions not met");
                for (JsonNode g : getArr) {
                    String pid = g.path("product_id").asText();
                    int qtyPer = g.path("quantity").asInt(0);
                    int allowed = qtyPer * possible;
                    Optional<CartItemDto> cartIt = itemsCopy.stream().filter(it -> pid.equals(String.valueOf(it.getProductId()))).findFirst();
                    if (cartIt.isPresent()) {
                        int freeQty = Math.min(cartIt.get().getQuantity(), allowed);
                        double disc = round2(freeQty * cartIt.get().getPrice());
                        cartIt.get().setTotalDiscount(disc);
                        totalDiscount += disc;
                    }
                }
                totalDiscount = round2(totalDiscount);
            } else throw new IllegalStateException("Unsupported coupon type");
            ApplyCouponResponse resp = new ApplyCouponResponse();
            resp.setItems(itemsCopy);
            resp.setTotalPrice(round2(total));
            resp.setTotalDiscount(round2(totalDiscount));
            resp.setFinalPrice(round2(total - totalDiscount));
            return resp;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to apply coupon: " + ex.getMessage());
        }
    }
}
