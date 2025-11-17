# Monk Coupons - Full Spring Boot Project

This is the full implementation (seeded) of the Coupons Management API:
- Cart-wise, Product-wise, BxGy coupons
- CRUD endpoints
- /applicable-coupons and /apply-coupon/{id}
- H2 in-memory DB with seeded coupons

Run:
- mvn spring-boot:run
- or import into IntelliJ and run MonkCouponsApplication

H2 console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:couponsdb)
