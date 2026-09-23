# SS16_HW04 - Chiến lược cập nhật cache: xóa hay ghi đè?

## 1. Input và Output

Thao tác cập nhật nhận:

- `productId`: String, bắt buộc khác null/rỗng.
- `UpdateProductRequest`: `name` không rỗng, `price >= 0`, `description` tối đa 500 ký tự.
- Output: `ProductDTO(productId, name, price, description, version)` phản ánh bản ghi DB vừa cập nhật.

API minh họa:

```text
GET /api/products/{productId}
PUT /api/products/{productId}
```

## 2. Cơ chế @CachePut

Method luôn được thực thi để cập nhật DB. Sau khi trả kết quả, Spring ghi chính kết quả đó đè lên key cache. Lần đọc tiếp theo thường hit cache ngay, không cần một lượt truy vấn DB để làm ấm lại.

```text
Update request -> UPDATE DB -> SET cache bằng DTO mới -> response
```

Điểm khó là một request phải thành công ở hai hệ thống. Nếu DB commit nhưng Redis SET lỗi, cache cũ có thể tiếp tục tồn tại. Nếu hai update chạy song song, thứ tự hoàn thành ghi cache có thể khác thứ tự phiên bản DB cuối cùng, khiến cache chứa giá trị cũ hơn.

## 3. Cơ chế @CacheEvict

Method cập nhật DB trước. Chỉ khi method thành công, Spring xóa key cũ. Lần đọc kế tiếp cache miss, lấy dữ liệu đã commit từ DB và nạp lại cache.

```text
Update request -> UPDATE DB -> DELETE cache -> response
Next read -> cache miss -> SELECT DB -> SET cache
```

Bài dùng `beforeInvocation=false`, là giá trị mặc định. Nếu DB lỗi, evict không chạy nên không xóa cache vô ích.

## 4. Bảng so sánh

| Tiêu chí | `@CachePut` | `@CacheEvict` |
|---|---|---|
| Race condition | Hai update có thể SET cache sai thứ tự; cần version/CAS hoặc lock | Các update chỉ xóa key; lượt đọc sau lấy phiên bản DB đã commit |
| Độ phức tạp | Phải bảo đảm DTO ghi cache đúng key, đúng phiên bản | Đơn giản, chỉ xóa key sau update thành công |
| Nhất quán | Cache mới ngay nếu SET thành công; lỗi SET có thể giữ cache cũ | Eventual consistency; cache tự tái tạo từ DB |
| Response time ghi | Có thêm thao tác ghi payload vào Redis | DELETE thường nhẹ hơn SET payload |
| Lượt đọc đầu sau update | Hit ngay | Miss một lần và đọc DB |
| Redis down | DB cập nhật nhưng cache cũ có thể còn đến TTL | DB cập nhật nhưng evict lỗi; cache cũ cũng còn đến TTL |
| Phù hợp | Cần cache nóng ngay, dữ liệu ít tranh chấp | Ưu tiên đơn giản, DB là nguồn sự thật, update hiếm |

## 5. Lựa chọn cho tỷ lệ đọc/ghi 100:1

Bài chọn **`@CacheEvict`**. Cứ 100 lượt đọc mới có một update, nên chi phí một cache miss sau mỗi lần cập nhật rất nhỏ. Đổi lại, code đơn giản hơn và không phải quyết định cách xử lý ghi cache sai thứ tự khi nhiều request update song song.

`@CachePut` cũng có lợi cho hệ thống đọc nhiều vì cache luôn nóng, nhưng lợi ích thực tế chỉ tiết kiệm đúng một DB read sau mỗi update. Với tỷ lệ 100:1, khoản tiết kiệm đó không đáng để tăng coupling giữa DTO update và cache.

## 6. Race condition và optimistic locking

Entity có trường `@Version`. Nếu hai transaction cùng đọc một version rồi cùng ghi, JPA phát hiện update trễ và từ chối thay vì âm thầm mất dữ liệu. Đây là bảo vệ ở tầng DB; cache vẫn được evict sau method thành công.

Một race điển hình của Cache-Aside:

1. Request đọc cache miss và bắt đầu SELECT.
2. Request update commit DB rồi evict cache.
3. Request đọc cũ ghi giá trị cũ vào cache sau evict.

Để giảm cửa sổ này trong production có thể dùng delayed double delete, version trong cache, invalidate event sau commit hoặc TTL ngắn. `sync=true` chỉ chống nhiều loader trong cùng instance, không thay thế distributed lock.

## 7. Khi evict thất bại

Nếu Redis down/timeout sau khi DB commit:

1. `CacheErrorHandler.handleCacheEvictError` ghi log ERROR nhưng không rollback dữ liệu DB đã đúng.
2. Cache có TTL 60 giây nên dữ liệu cũ tự biến mất.
3. Metric/alert thông báo cho vận hành về tỷ lệ lỗi evict.
4. Production nên ghi sự kiện `ProductUpdated(productId, version)` vào transactional outbox.
5. Worker hoặc message consumer retry xóa key khi Redis phục hồi. Event cần idempotent vì DELETE lặp lại là an toàn.

TTL là lớp an toàn cuối, không phải cơ chế đồng bộ duy nhất. Với dữ liệu nhạy cảm hơn, outbox + message queue giúp giảm thời gian stale mà không buộc request người dùng chờ Redis phục hồi.

## 8. Cấu trúc source

- `ProductService.getProductById`: `@Cacheable(cacheNames="products")`.
- `ProductService.updateProduct`: cập nhật DB và `@CacheEvict` sau thành công.
- `RedisCacheConfig`: JSON serializer, TTL 60 giây và CacheErrorHandler.
- `Product`: entity có `@Version` chống lost update.
- `ProductServiceTest`: kiểm tra annotation chiến lược, validation và cập nhật DB.

## 9. Cài đặt và chạy

```bash
docker compose up -d
./gradlew clean test
./gradlew bootRun
```

Chạy kịch bản:

```bash
chmod +x demo/test-cache-strategy.sh
./demo/test-cache-strategy.sh
```

Dữ liệu ban đầu là `P001 - iPhone 15 - 19.990.000 VND`. Script đọc hai lần, cập nhật sản phẩm và đọc lại để chứng minh cache đã bị xóa rồi tái nạp.

## 10. Kết luận

Không có chiến lược tốt tuyệt đối. `@CachePut` phù hợp khi phải giữ cache nóng và có cơ chế kiểm soát version tốt. Với hệ thống này, update hiếm và DB là nguồn dữ liệu chuẩn, `@CacheEvict` đem lại cân bằng tốt hơn giữa hiệu năng, tính dễ hiểu và khả năng phục hồi khi Redis gặp sự cố.
