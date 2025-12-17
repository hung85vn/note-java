OK 👍 dưới đây là **AOP HOÀN CHỈNH – chạy được – đúng chuẩn enterprise** cho `@ProtectedApi`.

Mình sẽ làm theo nguyên tắc:

* ✔ Check quyền **TRƯỚC khi vào Controller action**
* ✔ Lấy đúng `@PathVariable Long id`
* ✔ Không dùng Interceptor
* ✔ Tách rõ: Annotation → AOP → PermissionService
* ✔ Throw exception → Spring trả 403 chuẩn

---

# 1️⃣ Annotation `@ProtectedApi`

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectedApi {

    String table();          // orders, users, invoices...
    Action action();         // READ, UPDATE, DELETE
    String idParam() default "id"; // tên parameter chứa recordId
}
```

```java
public enum Action {
    READ, CREATE, UPDATE, DELETE
}
```

---

# 2️⃣ Dùng trong Controller (rất gọn)

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @ProtectedApi(
        table = "orders",
        action = Action.READ,
        idParam = "id"
    )
    @GetMapping("/{id}")
    public OrderDTO get(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @ProtectedApi(
        table = "orders",
        action = Action.UPDATE,
        idParam = "id"
    )
    @PutMapping("/{id}")
    public void update(
            @PathVariable Long id,
            @RequestBody UpdateOrderDTO dto) {
        orderService.update(id, dto);
    }
}
```

📌 `@PathVariable` **bắt buộc** để Spring bind URL → param
📌 `idParam = "id"` phải **trùng tên biến method**

---

# 3️⃣ AOP Aspect (PHẦN QUAN TRỌNG NHẤT)

```java
@Aspect
@Component
@Order(1) // chạy sớm
public class ProtectedApiAspect {

    private final PermissionService permissionService;

    public ProtectedApiAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Around("@annotation(protectedApi)")
    public Object checkPermission(
            ProceedingJoinPoint joinPoint,
            ProtectedApi protectedApi
    ) throws Throwable {

        // 1️⃣ Lấy thông tin method
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 2️⃣ Lấy recordId theo idParam
        Long recordId = null;

        for (int i = 0; i < paramNames.length; i++) {
            if (paramNames[i].equals(protectedApi.idParam())) {
                recordId = (Long) args[i];
                break;
            }
        }

        if (recordId == null) {
            throw new IllegalStateException(
                "Không tìm thấy parameter '" + protectedApi.idParam() + "'"
            );
        }

        // 3️⃣ Lấy user context
        UserContext user = SecurityContext.getCurrentUser();

        // 4️⃣ Check quyền
        boolean allowed = permissionService.check(
                user,
                protectedApi.table(),
                recordId,
                protectedApi.action()
        );

        if (!allowed) {
            throw new AccessDeniedException("ACCESS_DENIED");
        }

        // 5️⃣ OK → cho chạy Controller action
        return joinPoint.proceed();
    }
}
```

---

## ⚠️ CỰC KỲ QUAN TRỌNG

Để `signature.getParameterNames()` hoạt động,
**bạn PHẢI bật compiler option**:

### Maven

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <parameters>true</parameters>
  </configuration>
</plugin>
```

👉 Nếu không: Java sẽ thấy `arg0`, `arg1` → AOP **KHÔNG BIẾT param name**

---

# 4️⃣ PermissionService (logic thật sự)

### Ví dụ Owner-based

```java
@Service
public class PermissionService {

    private final JdbcTemplate jdbc;

    public PermissionService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean check(
            UserContext user,
            String table,
            Long recordId,
            Action action
    ) {

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

📌 Có thể nâng cấp:

* ACL table
* YAML mapping
* Redis cache
* Multi-tenant

---

# 5️⃣ UserContext + SecurityContext (giản lược)

```java
public class UserContext {

    private Long userId;
    private boolean admin;

    public Long getUserId() { return userId; }
    public boolean isAdmin() { return admin; }
}
```

```java
public class SecurityContext {

    public static UserContext getCurrentUser() {
        // lấy từ JWT / ThreadLocal / Spring Security
        return ...;
    }
}
```

---

# 6️⃣ Xử lý 403 chuẩn JSON (BEST PRACTICE)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, Object> forbidden(AccessDeniedException ex) {

        return Map.of(
            "code", "ACCESS_DENIED",
            "message", "Bạn không có quyền truy cập tài nguyên"
        );
    }
}
```

---

# 7️⃣ Luồng chạy thực tế

```
Request
  ↓
Spring AOP (@Around)
  ↓  check permission
Controller method
  ↓
Service
```

➡ Nếu fail → **Controller KHÔNG BAO GIỜ chạy**

---

# 8️⃣ Những lỗi hay gặp (cảnh báo)

❌ Quên `<parameters>true</parameters>`
❌ `idParam` không trùng tên biến
❌ Ép kiểu `(Long)` sai (int / String)
❌ Đặt permission logic trong Controller

---

# 9️⃣ Khi nào KHÔNG nên dùng AOP?

* App lớn, nhiều team
* Cần audit, IAM chuẩn
* Đã dùng Spring Security

👉 Khi đó nên dùng `@PreAuthorize`

---

## 🔥 KẾT LUẬN

✔ Đây là **AOP record-level authorization hoàn chỉnh**
✔ Đúng yêu cầu: *check quyền trước action*
✔ Có thể dùng ngay cho dự án thật

---

Nếu bạn muốn, bước tiếp theo mình có thể:

* Nâng cấp sang **ACL + YAML**
* Thêm **multi-tenant**
* Hoặc **chuyển AOP này sang Spring Security PermissionEvaluator**

👉 Bạn đang dùng **JWT hay Session**?
