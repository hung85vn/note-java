Rất tốt 👍
Nếu **dùng `@PreAuthorize`** thì bạn đang đi **đúng con đường chuẩn nhất của Spring Security**.

Mình sẽ trình bày **TỪ ĐẦU → ĐẾN CUỐI → CHẠY ĐƯỢC**, đúng với yêu cầu của bạn:

> *Check quyền trước khi vào Controller action, có record-level (theo id)*

---

# 1️⃣ Bật Method Security

👉 **BẮT BUỘC**

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
}
```

> (Spring Boot 3+ chỉ cần `@EnableMethodSecurity`)

---

# 2️⃣ SecurityConfig (JWT / auth có hay không đều được)

Ví dụ tối thiểu:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

📌 Phần này **không liên quan trực tiếp đến phân quyền record**, chỉ cần auth OK.

---

# 3️⃣ Tạo Permission Service (trái tim)

```java
@Component("permission")
public class RecordPermissionService {

    private final JdbcTemplate jdbc;

    public RecordPermissionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean check(
            Authentication authentication,
            Long recordId,
            String table,
            String action
    ) {

        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        if (user.isAdmin()) {
            return true;
        }

        String sql = """
            SELECT COUNT(1)
            FROM %s
            WHERE id = ?
              AND owner_id = ?
        """.formatted(table);

        Integer count = jdbc.queryForObject(
                sql,
                Integer.class,
                recordId,
                user.getUserId()
        );

        return count != null && count > 0;
    }
}
```

📌 Bean name **bắt buộc** là `"permission"` để gọi trong SpEL.

---

# 4️⃣ Dùng `@PreAuthorize` trong Controller

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PreAuthorize(
      "@permission.check(authentication, #id, 'orders', 'READ')"
    )
    @GetMapping("/{id}")
    public OrderDTO get(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PreAuthorize(
      "@permission.check(authentication, #id, 'orders', 'UPDATE')"
    )
    @PutMapping("/{id}")
    public void update(
        @PathVariable Long id,
        @RequestBody UpdateOrderDTO dto
    ) {
        orderService.update(id, dto);
    }
}
```

### 🚀 Quan trọng:

* `#id` → chính là `@PathVariable Long id`
* Không cần `idParam`
* Không cần AOP
* Không cần Interceptor

---

# 5️⃣ Response khi không có quyền

Spring Security **tự động trả**:

```http
HTTP/1.1 403 Forbidden
```

Nếu muốn JSON chuẩn:

```java
@Component
public class SecurityExceptionHandler
        implements AccessDeniedHandler {

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException ex
    ) throws IOException {

        response.setStatus(403);
        response.setContentType("application/json");
        response.getWriter().write("""
        {
          "code": "ACCESS_DENIED",
          "message": "Bạn không có quyền"
        }
        """);
    }
}
```

```java
http.exceptionHandling(e ->
    e.accessDeniedHandler(securityExceptionHandler)
);
```

---

# 6️⃣ Dùng nhiều param (nâng cao)

```java
@PreAuthorize(
  "@permission.check(authentication, #orderId, 'orders', 'READ')"
)
@GetMapping("/{orderId}")
public OrderDTO get(@PathVariable Long orderId) { ... }
```

---

# 7️⃣ Kết hợp role + record

```java
@PreAuthorize("""
    hasRole('ADMIN')
    or @permission.check(authentication, #id, 'orders', 'READ')
""")
```

---

# 8️⃣ Khi nào KHÔNG dùng `@PreAuthorize`

| Trường hợp                     | Lý do            |
| ------------------------------ | ---------------- |
| Check từ request body phức tạp | SpEL khó đọc     |
| Legacy app không có Security   | Dùng AOP         |
| DB RLS đã chặn                 | Có thể không cần |

---

# 9️⃣ Kiến trúc chuẩn

```
Spring Security Filter
   ↓
@PreAuthorize (Method Security)
   ↓
Controller Action
   ↓
Service
```

---

# 🔥 Tóm tắt ngắn gọn

✔ `@EnableMethodSecurity`
✔ Tạo `RecordPermissionService`
✔ Gọi bằng `@PreAuthorize`
✔ Check quyền **trước Controller**
✔ Không cần Interceptor / AOP

---

Nếu bạn muốn, mình có thể:

* Viết version **ACL table**
* Đọc rule từ **YAML**
* Cache quyền bằng **Redis**
* Multi-tenant (company_id)

👉 Bạn đang dùng **JWT hay Session**?
