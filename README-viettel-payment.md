# Tích Hợp Cổng Thanh Toán Viettel Money

## Tổng quan

Tài liệu này mô tả chi tiết quá trình tích hợp và sử dụng cổng thanh toán Viettel Money trong ứng dụng. Việc tích hợp cho phép khách hàng thanh toán đơn hàng thông qua Viettel Money, hỗ trợ web, mã QR và deep link trên thiết bị di động.

## Nguyên tắc thiết kế

Việc tích hợp cổng thanh toán Viettel Money đã được thiết kế dựa trên các nguyên tắc sau:

1. **Thiết kế theo lớp (Layered Design)**: Hệ thống được phân tách thành các lớp riêng biệt với trách nhiệm cụ thể:
   - Lớp Controller: Xử lý các yêu cầu HTTP từ người dùng và từ cổng thanh toán.
   - Lớp Service: Chứa logic nghiệp vụ chính cho việc xử lý thanh toán.
   - Lớp API Client: Chịu trách nhiệm giao tiếp với API bên ngoài (Viettel Money).
   - Lớp Configuration: Quản lý cấu hình cho các môi trường khác nhau.

2. **Nguyên tắc đơn trách nhiệm (Single Responsibility Principle)**: Mỗi lớp và phương thức chỉ thực hiện một nhiệm vụ cụ thể:
   - ViettelPaymentService: Quản lý quy trình thanh toán và hoàn tiền.
   - ViettelApiClient: Chỉ tập trung vào việc giao tiếp HTTP với API Viettel.
   - ViettelSignatureHandler: Chuyên về tạo và xác minh chữ ký số.

3. **Đa hình môi trường (Environment Polymorphism)**: Hệ thống hỗ trợ chuyển đổi linh hoạt giữa các môi trường (sandbox, preprod, production) thông qua cấu hình, không cần thay đổi mã nguồn.

4. **Xử lý lỗi mạnh mẽ (Robust Error Handling)**: Mọi tương tác với API đều có cơ chế xử lý lỗi chi tiết, ghi log đầy đủ, và thử lại nếu cần thiết.

5. **Bảo mật theo chiều sâu (Defense in Depth)**: Áp dụng nhiều lớp bảo mật:
   - Sử dụng HTTPS cho giao tiếp.
   - Xác thực bằng access token.
   - Toàn vẹn dữ liệu bằng chữ ký số ECDSA với SHA256.

## Kiến trúc hệ thống

### Tổng quan kiến trúc

Tích hợp cổng thanh toán Viettel Money được thiết kế theo kiến trúc phân lớp (layered architecture) với các thành phần sau:

1. **ViettelPaymentService**: Lớp dịch vụ core, triển khai logic nghiệp vụ thanh toán như khởi tạo giao dịch, hoàn tiền, và cập nhật trạng thái đơn hàng.
2. **ViettelApiClient**: Lớp giao tiếp, đóng gói tất cả logic HTTP để giao tiếp với API của Viettel Money.
3. **ViettelSignatureHandler**: Lớp bảo mật, quản lý việc ký và xác minh chữ ký số trong giao tiếp API.
4. **ViettelPaymentConfig**: Lớp cấu hình, chứa cấu hình cho các môi trường khác nhau (sandbox, preprod, production).
5. **ViettelPaymentController**: Lớp controller, cung cấp API endpoints cho ứng dụng frontend.
6. **ViettelPartnerController**: Lớp controller callback, xử lý các callback từ Viettel Money (IPN và redirect).

### Mẫu thiết kế được áp dụng

Hệ thống áp dụng nhiều mẫu thiết kế phổ biến để đảm bảo tính mở rộng, bảo trì và an toàn:

1. **Facade Pattern** (ViettelPaymentService): Cung cấp giao diện đơn giản hóa cho hệ thống phức tạp, ẩn đi chi tiết giao tiếp API.
2. **Gateway Pattern** (ViettelApiClient): Tạo một lớp trừu tượng hóa cho việc giao tiếp với API bên ngoài.
3. **Strategy Pattern** (ViettelPaymentConfig): Cho phép chuyển đổi linh hoạt giữa các cấu hình môi trường.
4. **Value Object Pattern** (EnvironmentConfig): Đóng gói các thông tin cấu hình liên quan mà không có phương thức thay đổi trạng thái.
5. **Inversion of Control** (Partner Callbacks): Cho phép Viettel Money chủ động gửi thông báo đến hệ thống.
6. **Event-Driven Design** (updateOrderPaymentStatus): Xử lý kết quả thanh toán như các sự kiện độc lập.

### Chiến lược bảo mật

Tích hợp áp dụng chiến lược bảo mật theo chiều sâu (defense in depth) với nhiều lớp bảo vệ:

1. **Bảo mật kênh truyền**: Sử dụng HTTPS cho tất cả giao tiếp API.
2. **Xác thực API**: Áp dụng xác thực Bearer Token trong mọi request.
3. **Toàn vẹn dữ liệu**: Sử dụng chữ ký số ECDSA với SHA256 cho mọi request và response.
4. **Kiểm soát truy cập**: Phân quyền chi tiết cho các API endpoint theo vai trò.
5. **Bảo vệ khóa**: Khóa bảo mật được lưu trữ trong cấu hình ngoài mã nguồn.
6. **Ghi log bảo mật**: Ghi log chi tiết mọi hoạt động thanh toán để phát hiện và điều tra sự cố.

### Khả năng mở rộng và bảo trì

Thiết kế hệ thống đặc biệt chú trọng đến khả năng mở rộng và bảo trì:

1. **Cấu hình ngoài**: Tất cả cấu hình được tách biệt khỏi mã nguồn, cho phép thay đổi không cần build lại.
2. **Tách biệt mối quan tâm**: Mỗi lớp chỉ tập trung vào một trách nhiệm cụ thể.
3. **Xử lý lỗi mạnh mẽ**: Chiến lược xử lý lỗi toàn diện với logging và retry.
4. **Khả năng theo dõi**: Ghi log chi tiết cho mọi bước trong quy trình thanh toán.
5. **Thiết kế theo giao diện**: Giảm sự phụ thuộc giữa các lớp, cho phép thay thế triển khai.

## Cấu hình

### Cấu hình trong file `viettel-payment.yml`

```yaml
viettel:
  payment:
    # Cấu hình môi trường
    environments:
      sandbox:
        apiUrl: "http://125.235.38.299:8080/paybiz/payment-gateway/public/api"
        merchantCode: "SANDBOX_MERCHANT_001"
        accessToken: "sandbox_access_token_here"
        publicKey: "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEwj3ByQp85F4u9wudk8UPS7p3ETS1jU9HJ1001hFV4PMlw7Zb0HKlnZ8ol6GbkxYTne7NReCexpc8p1be0ewCaA=="
        privateKey: "sandbox_private_key_base64_here"
        partnerPublicKey: "partner_public_key_base64_here"
      preprod:
        apiUrl: "https://api24cdn.vtmoney.vn/uatmm/paybiz/payment-gateway/public/api"
        merchantCode: "PREPROD_MERCHANT_001"
        accessToken: "preprod_access_token_here"
        publicKey: "preprod_public_key_base64_here"
        privateKey: "preprod_private_key_base64_here"
        partnerPublicKey: "partner_public_key_base64_here"
      production:
        apiUrl: "https://api23.vtmoney.vn/paybiz/payment-gateway/public/api"
        merchantCode: "PROD_MERCHANT_001"
        accessToken: "production_access_token_here"
        publicKey: "production_public_key_base64_here"
        privateKey: "production_private_key_base64_here"
        partnerPublicKey: "partner_public_key_base64_here"
    
    # Môi trường hoạt động hiện tại
    activeEnvironment: "sandbox"
    
    # URL của đối tác (được gọi bởi Viettel)
    partner:
      baseUrl: "https://yourapp.example.com"
      orderConfirmationPath: "/api/viettel/order-confirmation"
      ipnPath: "/api/viettel/ipn"
      redirectPath: "/payment/viettel/result"
    
    # Cài đặt chung
    settings:
      defaultReturnType: "WEB"
      defaultExpireAfterMinutes: 15
      maxRetryCount: 3
      retryIntervalMinutes: 10
```

### Cấu hình khóa bảo mật

Để bảo mật giao tiếp giữa hệ thống và Viettel Money, cần cấu hình các khóa sau:

1. **privateKey**: Khóa riêng tư của bạn, được sử dụng để ký các request gửi đến Viettel.
2. **publicKey**: Khóa công khai của bạn, được chia sẻ với Viettel.
3. **partnerPublicKey**: Khóa công khai của Viettel, được sử dụng để xác minh các response từ Viettel.

Các khóa cần được mã hóa Base64 trước khi đưa vào file cấu hình.

## Luồng thanh toán

### 1. Khởi tạo thanh toán

1. Khách hàng chọn phương thức thanh toán Viettel Money.
2. Hệ thống gọi `ViettelPaymentService.initiatePayment()` để tạo giao dịch.
3. `ViettelApiClient` gửi request tới Viettel Money API, bao gồm chữ ký số được tạo bởi `ViettelSignatureHandler`.
4. Tùy thuộc vào `returnType` (WEB, QR, DEEPLINK), hệ thống trả về thông tin thanh toán phù hợp.
5. Đơn hàng được cập nhật với `paymentId` và trạng thái "INITIATED".

### 2. Xử lý kết quả thanh toán

1. Sau khi khách hàng hoàn thành thanh toán, Viettel Money gửi thông báo đến endpoint IPN (Instant Payment Notification).
2. `ViettelPartnerController` xử lý thông báo, xác minh chữ ký và cập nhật trạng thái đơn hàng.
3. Nếu thanh toán thành công, đơn hàng được cập nhật trạng thái thành "PAID".
4. Email xác nhận được gửi đến khách hàng.

### 3. Xử lý hoàn tiền

1. Quản trị viên khởi tạo yêu cầu hoàn tiền.
2. Hệ thống gọi `ViettelPaymentService.processRefund()`.
3. `ViettelApiClient` gửi request hoàn tiền tới Viettel Money API.
4. Sau khi xử lý thành công, đơn hàng được cập nhật trạng thái thành "REFUNDED".
5. Email thông báo hoàn tiền được gửi đến khách hàng.

## API Endpoints

### API Frontend

- **POST /api/payment/viettel/initiate/{orderId}**: Khởi tạo thanh toán.
- **GET /api/payment/viettel/status/{orderId}**: Kiểm tra trạng thái thanh toán.
- **POST /api/payment/viettel/refund/{orderId}**: Yêu cầu hoàn tiền.

### API Callback từ Viettel

- **POST /api/viettel/ipn**: Xử lý thông báo thanh toán tức thời (IPN).
- **GET /api/viettel/redirect**: Xử lý redirect sau khi khách hàng hoàn thành thanh toán.

## Bảo mật và xác thực

### Chữ ký số

Giao tiếp giữa hệ thống và Viettel Money được bảo mật bằng chữ ký số ECDSA với SHA256:

1. **Nguyên lý hoạt động**: 
   - Mỗi bên (Merchant và Viettel) có một cặp khóa: khóa riêng tư (dùng để ký) và khóa công khai (dùng để xác minh).
   - Bên gửi sử dụng khóa riêng tư của mình để ký nội dung request/response.
   - Bên nhận sử dụng khóa công khai của bên gửi để xác minh chữ ký.

2. **Quy trình ký request**:
   - Hệ thống ký chính xác chuỗi JSON của request body (không định dạng lại).
   - Chữ ký được đặt trong header "Signature" của HTTP request.
   - Đối với request GET (như redirect URL), ký chuỗi query parameter.

3. **Quy trình xác minh response**:
   - Lấy chữ ký từ header "Signature" của HTTP response.
   - Sử dụng khóa công khai của Viettel để xác minh chữ ký với chuỗi JSON response body.
   - Tương tự đối với response từ redirect URL, xác minh signature query parameter.

4. **Bảo vệ khóa**:
   - Khóa riêng tư phải được bảo vệ cẩn thận, không được hardcode trong mã nguồn.
   - Sử dụng cơ chế như Vault hoặc biến môi trường để lưu trữ và truy cập khóa an toàn.

### Xác thực API

Ngoài chữ ký số, hệ thống còn áp dụng các lớp xác thực bổ sung:

1. **Bearer Token**: Mọi request đến API Viettel Money đều phải kèm header "Authorization: Bearer {accessToken}".
2. **HTTPS**: Giao tiếp qua kênh mã hóa HTTPS trong môi trường preprod và production.
3. **Kiểm tra IP**: Viettel Money có thể hạn chế chỉ chấp nhận request từ các IP đã đăng ký.
4. **Xác thực hai chiều**: Cả hai bên đều xác thực lẫn nhau thông qua chữ ký số.

## Xử lý lỗi

1. **Timeout**: Nếu không nhận được phản hồi sau khoảng thời gian chờ, hệ thống sẽ đánh dấu giao dịch là "PENDING" và kiểm tra lại sau.
2. **Lỗi mạng**: Hệ thống sẽ thử lại tối đa `maxRetryCount` lần, với khoảng thời gian giữa các lần là `retryIntervalMinutes`.
3. **Lỗi xác minh chữ ký**: Hệ thống sẽ từ chối xử lý các request hoặc response có chữ ký không hợp lệ.

## Môi trường

1. **Sandbox**: Môi trường thử nghiệm, không liên quan đến tiền thật.
2. **Preprod**: Môi trường tiền sản phẩm, sử dụng tiền thật nhưng trong phạm vi hạn chế.
3. **Production**: Môi trường sản phẩm chính thức, sử dụng tiền thật.

## Hướng dẫn triển khai

1. Cập nhật `publicKey`, `privateKey`, và `partnerPublicKey` trong file `viettel-payment.yml` theo thông tin được cung cấp bởi Viettel Money.
2. Cập nhật `partner.baseUrl` và các endpoints theo URL thực tế của ứng dụng.
3. Cài đặt `activeEnvironment` theo môi trường hiện tại (sandbox, preprod, production).
4. Kiểm tra hoạt động của hệ thống bằng cách tạo giao dịch thử trong môi trường sandbox.

## Lưu ý

- Luôn xác minh chữ ký của các phản hồi từ Viettel Money.
- Lưu trữ các khóa bảo mật một cách an toàn, không đưa chúng vào mã nguồn.
- Ghi log đầy đủ các giao dịch để dễ dàng xử lý sự cố.
- Thực hiện kiểm tra định kỳ để đảm bảo kết nối với Viettel Money hoạt động bình thường.
