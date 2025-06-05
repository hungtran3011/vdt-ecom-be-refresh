# Phân tích lựa chọn thiết kế tích hợp Viettel Money

Tài liệu này giải thích chi tiết về các quyết định thiết kế quan trọng trong quá trình tích hợp cổng thanh toán Viettel Money vào hệ thống của chúng ta.

## 1. Kiến trúc phân lớp

### Lựa chọn thiết kế
Chúng tôi đã lựa chọn kiến trúc phân lớp rõ ràng với ViettelPaymentService, ViettelApiClient, ViettelSignatureHandler và các lớp khác.

### Lý do
- **Tách biệt mối quan tâm**: Mỗi lớp có một trách nhiệm duy nhất, giúp dễ dàng bảo trì và mở rộng.
- **Giảm sự phụ thuộc**: Các lớp không trực tiếp phụ thuộc vào triển khai của nhau mà chỉ phụ thuộc vào giao diện.
- **Dễ kiểm thử**: Có thể dễ dàng tạo mock cho mỗi lớp khi viết unit test.
- **Khả năng mở rộng**: Dễ dàng thay thế triển khai của một lớp mà không ảnh hưởng đến các lớp khác.

### Phương án thay thế
- **Monolithic Design**: Đặt tất cả logic vào một lớp duy nhất - đơn giản hơn nhưng khó bảo trì và không linh hoạt.
- **Microservices**: Tách thành một service riêng biệt - quá phức tạp cho nhu cầu hiện tại, nhưng có thể cân nhắc trong tương lai nếu khối lượng giao dịch tăng đáng kể.

## 2. Quản lý cấu hình môi trường

### Lựa chọn thiết kế
Sử dụng một cấu hình duy nhất (viettel-payment.yml) cho tất cả các môi trường với khả năng chuyển đổi thông qua cài đặt activeEnvironment.

### Lý do
- **Đơn giản hóa triển khai**: Không cần các file cấu hình riêng cho mỗi môi trường.
- **Nhất quán cấu trúc**: Đảm bảo cấu trúc cấu hình giống nhau giữa các môi trường.
- **Chuyển đổi nhanh chóng**: Có thể dễ dàng chuyển đổi giữa các môi trường thông qua một cài đặt duy nhất.
- **Truy xuất hiệu quả**: Sử dụng Map lưu trữ cấu hình cho phép truy cập O(1) đến cấu hình của môi trường cần thiết.

### Phương án thay thế
- **File cấu hình riêng biệt**: application-sandbox.yml, application-production.yml - yêu cầu triển khai Spring Profiles.
- **Database Config**: Lưu cấu hình trong cơ sở dữ liệu - linh hoạt hơn nhưng phức tạp hơn và yêu cầu UI quản lý cấu hình.
- **External Config Server**: Sử dụng Spring Cloud Config - hữu ích cho hệ thống lớn nhưng quá phức tạp cho nhu cầu hiện tại.

## 3. Xử lý chữ ký số

### Lựa chọn thiết kế
Tạo một lớp riêng biệt (ViettelSignatureHandler) để xử lý tất cả các vấn đề liên quan đến chữ ký số.

### Lý do
- **Đóng gói logic phức tạp**: Tách biệt logic tạo và xác minh chữ ký khỏi logic nghiệp vụ.
- **Tái sử dụng**: Có thể sử dụng lại cùng một logic chữ ký trong các phần khác của ứng dụng.
- **Thay thổi thuật toán**: Nếu Viettel thay đổi thuật toán chữ ký, chỉ cần sửa ở một nơi duy nhất.
- **Chuẩn hóa xử lý**: Đảm bảo rằng mọi chữ ký đều được tạo và xác minh theo cùng một chuẩn.

### Phương án thay thế
- **Xử lý trực tiếp trong ApiClient**: Đơn giản hơn nhưng làm cho ApiClient trở nên phức tạp và khó bảo trì.
- **Thư viện bên thứ ba**: Sử dụng thư viện chuyên dụng - có thể tốt nhưng tạo sự phụ thuộc vào thư viện bên ngoài.
- **Delegate cho service khác**: Gọi một microservice riêng để ký và xác minh - quá phức tạp cho nhu cầu hiện tại.

## 4. Xử lý lỗi và retry

### Lựa chọn thiết kế
Sử dụng tham số cấu hình (maxRetryCount, retryIntervalMinutes) và theo dõi chi tiết các lỗi qua logging.

### Lý do
- **Khả năng phục hồi**: Hệ thống có thể tự động phục hồi từ lỗi tạm thời mà không cần can thiệp thủ công.
- **Kiểm soát hiệu suất**: Có thể điều chỉnh số lần thử lại và khoảng thời gian giữa các lần thử lại tùy theo điều kiện mạng và yêu cầu nghiệp vụ.
- **Theo dõi chi tiết**: Logging chi tiết giúp dễ dàng phân tích và khắc phục sự cố.
- **Tránh quá tải**: Khoảng thời gian giữa các lần thử lại giúp tránh làm quá tải hệ thống của Viettel Money.

### Phương án thay thế
- **Circuit Breaker Pattern**: Sử dụng Resilience4j hoặc Hystrix - tốt hơn cho hệ thống quy mô lớn nhưng phức tạp hơn.
- **Dead Letter Queue**: Lưu các giao dịch thất bại vào hàng đợi để xử lý sau - hữu ích cho xử lý bất đồng bộ.
- **Manual Retry**: Yêu cầu can thiệp thủ công - đơn giản nhưng không thân thiện với người dùng.

## 5. Luồng giao dịch

### Lựa chọn thiết kế
Sử dụng mô hình xử lý sự kiện, trong đó hệ thống khởi tạo giao dịch và sau đó chờ các callback (IPN và redirect) từ Viettel Money.

### Lý do
- **Không chặn người dùng**: Người dùng không phải đợi hệ thống của chúng ta kiểm tra kết quả giao dịch.
- **Phù hợp với API Viettel**: Tuân thủ quy trình giao dịch được Viettel Money định nghĩa.
- **Khả năng mở rộng**: Có thể xử lý lượng lớn giao dịch đồng thời mà không gây áp lực cho hệ thống.
- **Độ tin cậy cao**: IPN đảm bảo cập nhật trạng thái đơn hàng ngay cả khi người dùng không quay lại trang web.

### Phương án thay thế
- **Polling**: Định kỳ kiểm tra trạng thái giao dịch - đơn giản hơn nhưng không hiệu quả và có độ trễ.
- **Synchronous Processing**: Chờ kết quả thanh toán trong cùng một request - không khả thi với cổng thanh toán bên ngoài.
- **Webhook Registration**: Đăng ký webhook động thay vì cấu hình cố định - linh hoạt hơn nhưng phức tạp để triển khai.

## Kết luận

Các quyết định thiết kế được đưa ra dựa trên sự cân bằng giữa tính đơn giản, khả năng bảo trì, và độ tin cậy của hệ thống. Mục tiêu chính là tạo ra một tích hợp thanh toán đáng tin cậy, an toàn và dễ bảo trì, đồng thời đáp ứng các yêu cầu nghiệp vụ của dự án.

Thiết kế hiện tại được tối ưu hóa cho các yêu cầu hiện tại, nhưng cũng đã tính đến khả năng mở rộng trong tương lai khi khối lượng giao dịch tăng lên hoặc khi có yêu cầu về tích hợp với các cổng thanh toán khác.
