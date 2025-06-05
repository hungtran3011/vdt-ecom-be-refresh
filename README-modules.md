# Tài liệu Kiến trúc Hệ thống VDT E-Commerce

## Giới thiệu

Tài liệu này mô tả chi tiết về kiến trúc, nguyên lý thiết kế và lựa chọn công nghệ của các module trong hệ thống VDT E-Commerce. Khác với cách tiếp cận chỉ mô tả chức năng, tài liệu này tập trung giải thích *lý do tồn tại* của từng module và *các quyết định thiết kế* quan trọng đằng sau chúng.

## Nguyên tắc thiết kế tổng thể

Kiến trúc hệ thống được xây dựng dựa trên những nguyên tắc cốt lõi sau:

1. **Thiết kế hướng miền (Domain-Driven Design)**: Tách biệt logic nghiệp vụ thành các miền rõ ràng, phản ánh các khái niệm trong thế giới thực của thương mại điện tử.

2. **Kiến trúc phân lớp (Layered Architecture)**: Mỗi module được tổ chức thành các lớp với trách nhiệm rõ ràng (Controller, Service, Repository, Entity), giảm thiểu sự phụ thuộc và tăng khả năng bảo trì.

3. **Tách biệt mối quan tâm (Separation of Concerns)**: Mỗi module chỉ tập trung vào một khía cạnh cụ thể của hệ thống, tránh chồng chéo chức năng.

4. **RESTful API**: Giao tiếp giữa frontend và backend tuân theo nguyên tắc REST, cung cấp interface nhất quán và dễ hiểu.

5. **Bảo mật theo chiều sâu (Defense in Depth)**: Nhiều lớp bảo mật được áp dụng từ xác thực người dùng đến mã hóa dữ liệu nhạy cảm.

## Kiến trúc module

### 1. Module Auth (Xác thực)

#### Lý do tồn tại

Module Auth được tạo ra không đơn thuần để xác thực người dùng, mà còn để:
- **Tách biệt logic bảo mật**: Giải quyết vấn đề an toàn thông tin tập trung, tránh phân tán logic xác thực qua nhiều module
- **Linh hoạt trong xác thực**: Cho phép dễ dàng chuyển đổi giữa các cơ chế xác thực khác nhau (JWT, OAuth2, Keycloak) mà không ảnh hưởng đến các module khác
- **Quản lý phân quyền tập trung**: Đơn giản hóa việc kiểm soát quyền truy cập vào tài nguyên hệ thống

#### Lựa chọn thiết kế

1. **Tích hợp Keycloak**: Được chọn thay vì tự xây dựng hệ thống xác thực vì:
   - Giảm thời gian phát triển
   - Cung cấp sẵn các tính năng nâng cao (MFA, SSO)
   - Đáp ứng các tiêu chuẩn bảo mật hiện đại
   - Khả năng mở rộng cao

2. **Sử dụng PKCE (Proof Key for Code Exchange)**:
   - Bảo vệ khỏi tấn công CSRF trong dòng xác thực
   - An toàn hơn so với flow OAuth chuẩn cho ứng dụng SPA
   - Phù hợp cho cả môi trường web và mobile

3. **Phân tách giữa xác thực và ủy quyền**:
   - Authentication: Xác định người dùng là ai
   - Authorization: Xác định người dùng có quyền làm gì
   - Giúp dễ dàng mở rộng hệ thống phân quyền

### 2. Module Profile (Hồ sơ người dùng)

#### Lý do tồn tại

Module Profile được tách biệt khỏi Auth vì:
- **Tách biệt mối quan tâm**: Thông tin xác thực (auth) và thông tin cá nhân (profile) có vòng đời và mục đích sử dụng khác nhau
- **Bảo mật dữ liệu**: Cho phép áp dụng các chính sách bảo mật khác nhau cho dữ liệu xác thực và dữ liệu cá nhân
- **Mở rộng linh hoạt**: Dữ liệu hồ sơ người dùng thường xuyên thay đổi và mở rộng theo yêu cầu kinh doanh, trong khi dữ liệu xác thực ít thay đổi hơn

#### Lựa chọn thiết kế

1. **Phân tách Entity và DTO**:
   - Entity: Đại diện cho cấu trúc dữ liệu trong cơ sở dữ liệu
   - DTO: Đại diện cho dữ liệu trao đổi với client
   - Giúp kiểm soát chính xác thông tin gì được hiển thị cho người dùng

2. **Sử dụng UUID thay vì ID tự tăng**:
   - Tránh lộ thông tin về số lượng người dùng
   - Không thể đoán được ID của người dùng khác
   - Dễ dàng hợp nhất dữ liệu từ nhiều nguồn

3. **Lưu trữ thông tin địa chỉ riêng biệt**:
   - Hỗ trợ nhiều địa chỉ cho một người dùng
   - Tái sử dụng cho cả địa chỉ giao hàng và thanh toán
   - Chuẩn hóa dữ liệu địa lý

### 3. Module Product (Sản phẩm)

#### Lý do tồn tại

Module Product là trung tâm của hệ thống thương mại điện tử, được thiết kế để:
- **Quản lý thông tin sản phẩm phức tạp**: Xử lý các sản phẩm với nhiều biến thể, thuộc tính và mối quan hệ
- **Tối ưu hiệu suất truy vấn**: Cung cấp khả năng tìm kiếm và lọc sản phẩm nhanh chóng
- **Hỗ trợ SEO**: Cấu trúc dữ liệu sản phẩm thân thiện với công cụ tìm kiếm
- **Tách biệt khỏi inventory**: Cho phép quản lý thông tin sản phẩm độc lập với số lượng tồn kho

#### Lựa chọn thiết kế

1. **Mô hình sản phẩm phân cấp**:
   - Product: Thông tin chung về sản phẩm
   - ProductVariant: Biến thể cụ thể của sản phẩm (kích thước, màu sắc)
   - Giúp quản lý sản phẩm ở nhiều mức chi tiết khác nhau

2. **Lưu trữ hình ảnh tách biệt**:
   - Sử dụng bảng riêng cho hình ảnh sản phẩm
   - Hỗ trợ nhiều hình ảnh cho một sản phẩm
   - Dễ dàng mở rộng với CDN hoặc dịch vụ lưu trữ bên ngoài

3. **Thuộc tính động**:
   - Lưu trữ thuộc tính sản phẩm dưới dạng cấu trúc linh hoạt
   - Cho phép thêm thuộc tính mới mà không cần thay đổi schema
   - Hỗ trợ tìm kiếm theo thuộc tính

### 4. Module Category (Danh mục)

#### Lý do tồn tại

Module Category được tạo ra để:
- **Tổ chức sản phẩm**: Cung cấp cấu trúc phân loại rõ ràng cho catalog sản phẩm
- **Hỗ trợ điều hướng**: Giúp người dùng dễ dàng tìm kiếm sản phẩm theo phân cấp danh mục
- **Phân loại linh hoạt**: Cho phép một sản phẩm thuộc nhiều danh mục
- **Tối ưu SEO**: Tạo URL thân thiện và cấu trúc nội dung tốt cho SEO

#### Lựa chọn thiết kế

1. **Cấu trúc cây đa cấp**:
   - Cho phép danh mục lồng nhau không giới hạn độ sâu
   - Áp dụng Closure Table pattern để truy vấn hiệu quả
   - Cân bằng giữa tính linh hoạt và hiệu suất

2. **Quan hệ nhiều-nhiều với sản phẩm**:
   - Một sản phẩm có thể thuộc nhiều danh mục
   - Một danh mục có thể chứa nhiều sản phẩm
   - Sử dụng bảng trung gian để quản lý mối quan hệ

3. **Metadata phong phú**:
   - Mỗi danh mục có thể có banner, mô tả, SEO metadata riêng
   - Hỗ trợ các chiến dịch marketing theo danh mục
   - Dễ dàng tùy chỉnh hiển thị cho từng danh mục

### 5. Module Cart (Giỏ hàng)

#### Lý do tồn tại

Module Cart được thiết kế để:
- **Quản lý trạng thái tạm thời**: Lưu trữ ý định mua hàng của người dùng trước khi chuyển thành đơn hàng
- **Hỗ trợ mua sắm liên tục**: Cho phép người dùng thêm sản phẩm vào giỏ hàng trong nhiều phiên
- **Tối ưu trải nghiệm người dùng**: Cung cấp thông tin giỏ hàng ngay lập tức mà không cần tải lại trang
- **Phân tách khỏi Order**: Giảm phức tạp của quá trình đặt hàng bằng cách tách riêng giai đoạn giỏ hàng

#### Lựa chọn thiết kế

1. **Giỏ hàng lưu trữ theo người dùng**:
   - Giỏ hàng duy nhất cho mỗi người dùng
   - Hỗ trợ cả người dùng đã đăng nhập và khách
   - Hợp nhất giỏ hàng khi khách đăng nhập

2. **Cập nhật số lượng tồn kho tại thời điểm chuyển đổi**:
   - Chỉ kiểm tra và cập nhật tồn kho khi chuyển từ giỏ hàng sang đơn hàng
   - Ngăn chặn tình trạng "race condition" khi nhiều người dùng đặt hàng cùng lúc
   - Cân bằng giữa UX và tính chính xác của dữ liệu

3. **Tự động cập nhật giá**:
   - Giá trong giỏ hàng được cập nhật theo giá hiện tại của sản phẩm
   - Thông báo cho người dùng khi có thay đổi giá
   - Hỗ trợ các chính sách giá đặc biệt và khuyến mãi

### 6. Module Order (Đơn hàng)

#### Lý do tồn tại

Module Order được phát triển để:
- **Xử lý chu trình đặt hàng**: Quản lý toàn bộ vòng đời của đơn hàng từ khi tạo đến khi hoàn tất
- **Lưu trữ lịch sử giao dịch**: Cung cấp hồ sơ đầy đủ về các giao dịch mua hàng
- **Hỗ trợ quy trình kinh doanh**: Tích hợp với các quy trình khác như quản lý tồn kho, thanh toán, giao hàng
- **Cung cấp dữ liệu phân tích**: Thu thập thông tin quan trọng cho báo cáo kinh doanh và phân tích

#### Lựa chọn thiết kế

1. **Máy trạng thái đơn hàng (Order State Machine)**:
   - Định nghĩa rõ ràng các trạng thái đơn hàng và chuyển đổi hợp lệ
   - Ngăn chặn các chuyển đổi trạng thái không hợp lệ
   - Cho phép hook vào các sự kiện thay đổi trạng thái

2. **Snapshot giá và thông tin sản phẩm**:
   - Lưu trữ bản sao của thông tin sản phẩm tại thời điểm đặt hàng
   - Bảo vệ đơn hàng khỏi thay đổi thông tin sản phẩm sau này
   - Hỗ trợ xử lý tranh chấp và kiểm tra đơn hàng

3. **Quản lý đa địa chỉ**:
   - Hỗ trợ địa chỉ giao hàng và địa chỉ thanh toán riêng biệt
   - Lưu trữ lịch sử địa chỉ cho mỗi đơn hàng
   - Tích hợp với dịch vụ địa lý để xác thực địa chỉ

4. **Kiến trúc sự kiện (Event-driven Architecture)**:
   - Phát hành sự kiện cho các thay đổi quan trọng của đơn hàng
   - Cho phép các module khác phản ứng với thay đổi đơn hàng
   - Hỗ trợ xử lý bất đồng bộ như gửi email, cập nhật tồn kho

### 7. Module Stock (Tồn kho)

#### Lý do tồn tại

Module Stock được tạo ra để:
- **Quản lý tồn kho chính xác**: Theo dõi số lượng sản phẩm có sẵn trong thời gian thực
- **Ngăn chặn overselling**: Đảm bảo không bán nhiều hơn số lượng có sẵn
- **Hỗ trợ đa kho hàng**: Quản lý tồn kho trên nhiều địa điểm vật lý
- **Tối ưu hóa quản lý hàng tồn kho**: Giảm chi phí lưu trữ và tránh hết hàng

#### Lựa chọn thiết kế

1. **Quản lý tồn kho theo biến thể sản phẩm**:
   - Theo dõi tồn kho ở cấp độ biến thể (SKU) chứ không phải sản phẩm
   - Cho phép quản lý chính xác các biến thể khác nhau của cùng một sản phẩm
   - Hỗ trợ các quy tắc tồn kho riêng cho từng biến thể

2. **Giao dịch tồn kho**:
   - Ghi lại tất cả các thay đổi tồn kho dưới dạng giao dịch
   - Cung cấp khả năng kiểm tra và lịch sử đầy đủ
   - Hỗ trợ điều chỉnh và kiểm kê tồn kho

3. **Dự báo tồn kho**:
   - Tính toán điểm đặt hàng lại dựa trên lịch sử tiêu thụ
   - Cảnh báo khi hàng tồn kho xuống thấp
   - Hỗ trợ ra quyết định mua hàng

4. **Quản lý tồn kho đa kênh**:
   - Đồng bộ hóa tồn kho giữa nhiều kênh bán hàng
   - Ưu tiên phân bổ tồn kho cho các kênh khác nhau
   - Xử lý đặt hàng từ nhiều nguồn

### 8. Module Search (Tìm kiếm)

#### Lý do tồn tại

Module Search được phát triển để:
- **Cải thiện khả năng khám phá sản phẩm**: Giúp người dùng nhanh chóng tìm thấy sản phẩm họ quan tâm
- **Xử lý truy vấn phức tạp**: Hỗ trợ tìm kiếm theo nhiều tiêu chí và bộ lọc
- **Tối ưu hiệu suất**: Giảm tải cho cơ sở dữ liệu chính bằng cách tách riêng chức năng tìm kiếm
- **Cung cấp chức năng tìm kiếm nâng cao**: Hỗ trợ tìm kiếm đa ngôn ngữ, sửa lỗi chính tả, gợi ý

#### Lựa chọn thiết kế

1. **Tích hợp Elasticsearch**:
   - Cung cấp khả năng tìm kiếm toàn văn mạnh mẽ
   - Hỗ trợ tìm kiếm mờ và sửa lỗi chính tả
   - Mở rộng theo chiều ngang dễ dàng khi catalog sản phẩm tăng lên

2. **Đánh chỉ mục không đồng bộ**:
   - Cập nhật chỉ mục tìm kiếm bất đồng bộ khi dữ liệu sản phẩm thay đổi
   - Giảm thiểu tác động đến hiệu suất hệ thống chính
   - Đảm bảo tính nhất quán cuối cùng của dữ liệu tìm kiếm

3. **Bộ lọc và sắp xếp động**:
   - Cho phép người dùng lọc theo nhiều thuộc tính sản phẩm
   - Hỗ trợ các tiêu chí sắp xếp khác nhau (giá, độ phổ biến, đánh giá)
   - Tự động tạo facet dựa trên thuộc tính sản phẩm

### 9. Module Media (Phương tiện)

#### Lý do tồn tại

Module Media được thiết kế để:
- **Quản lý tài nguyên đa phương tiện**: Xử lý các tệp hình ảnh, video liên quan đến sản phẩm và nội dung
- **Tối ưu hóa hiệu suất**: Cung cấp các phiên bản tối ưu của tài nguyên cho các thiết bị khác nhau
- **Quản lý không gian lưu trữ**: Tổ chức và theo dõi việc sử dụng không gian lưu trữ
- **Bảo mật tài nguyên**: Kiểm soát quyền truy cập vào tài nguyên đa phương tiện

#### Lựa chọn thiết kế

1. **Xử lý hình ảnh động**:
   - Tự động tạo nhiều phiên bản của hình ảnh (thumbnail, trung bình, lớn)
   - Tối ưu hóa hình ảnh cho web với nén thông minh
   - Hỗ trợ định dạng hiện đại (WebP) với fallback

2. **Lưu trữ phân tán**:
   - Hỗ trợ lưu trữ cục bộ và dịch vụ đám mây (S3, Google Cloud Storage)
   - Chiến lược chuyển đổi dựa trên kích thước và loại tệp
   - Cache thông minh để cải thiện thời gian tải

3. **Metadata phong phú**:
   - Lưu trữ thông tin chi tiết về tài nguyên (kích thước, định dạng, nguồn gốc)
   - Hỗ trợ thẻ và phân loại để dễ dàng tìm kiếm
   - Theo dõi sử dụng và mối quan hệ với các entity khác

### 10. Module Mail (Email)

#### Lý do tồn tại

Module Mail được phát triển để:
- **Giao tiếp với người dùng**: Cung cấp kênh giao tiếp chính thức với khách hàng
- **Tự động hóa thông báo**: Gửi thông báo tự động cho các sự kiện hệ thống
- **Tuỳ chỉnh nội dung**: Cung cấp email được cá nhân hóa dựa trên dữ liệu người dùng
- **Theo dõi tương tác**: Phân tích hiệu quả của chiến dịch email

#### Lựa chọn thiết kế

1. **Kiến trúc template**:
   - Sử dụng Thymeleaf để tạo template email động
   - Hỗ trợ đa ngôn ngữ với i18n
   - Tách biệt nội dung và định dạng cho dễ bảo trì

2. **Xử lý bất đồng bộ**:
   - Gửi email qua hàng đợi để không chặn quy trình chính
   - Hỗ trợ thử lại tự động cho email thất bại
   - Giám sát và báo cáo tình trạng gửi

3. **Tích hợp dịch vụ email**:
   - Hỗ trợ nhiều nhà cung cấp dịch vụ email (SMTP, SendGrid, Amazon SES)
   - Chiến lược chuyển đổi dự phòng tự động
   - Quản lý giới hạn gửi và danh tiếng

## Mối quan hệ giữa các module

Hệ thống được thiết kế với sự phụ thuộc một chiều rõ ràng giữa các module:

1. **Core Modules**: Auth, Profile
   - Các module cốt lõi không phụ thuộc vào các module khác
   - Cung cấp dịch vụ nền tảng cho toàn bộ hệ thống

2. **Product Domain**: Product, Category, Stock, Media, Search
   - Quản lý thông tin sản phẩm và khả năng hiển thị
   - Media phục vụ Product nhưng không phụ thuộc vào nó
   - Search phụ thuộc vào Product và Category để đánh chỉ mục

3. **Order Processing**: Cart, Order, Payment
   - Cart phụ thuộc vào Product và Stock để hiển thị thông tin có sẵn
   - Order chuyển đổi từ Cart và phụ thuộc vào Profile cho thông tin người dùng
   - Payment phụ thuộc vào Order nhưng tách biệt về logic xử lý

4. **Cross-cutting Modules**: Mail
   - Phục vụ nhiều module khác nhau (Auth, Order, Payment)
   - Không chứa logic nghiệp vụ, chỉ cung cấp dịch vụ gửi thông báo

## Nguyên tắc mở rộng

Khi mở rộng hệ thống với các module mới, hãy tuân thủ các nguyên tắc sau:

1. **Duy trì ranh giới miền**: Mỗi module mới nên có ranh giới rõ ràng và tập trung vào một khía cạnh cụ thể của nghiệp vụ

2. **Tối thiểu hóa phụ thuộc**: Module mới nên có ít phụ thuộc nhất có thể, sử dụng interface thay vì implementation

3. **Event-driven khi cần thiết**: Sử dụng sự kiện để giao tiếp giữa các module không trực tiếp liên quan

4. **Tuân thủ quy ước đặt tên**: Giữ nhất quán trong cấu trúc package và quy ước đặt tên

5. **Cung cấp tài liệu đầy đủ**: Mỗi module mới phải có JSDoc đầy đủ giải thích lựa chọn thiết kế và lý do tồn tại

## Hướng dẫn thêm tài liệu JSDoc

Khi thêm JSDoc cho các lớp trong một module mới, hãy tuân theo mẫu sau:

```java
/**
 * [Mô tả ngắn gọn về lớp]
 * 
 * <p>Lớp này được thiết kế để [giải thích lý do tồn tại], giải quyết 
 * [vấn đề/thách thức] trong hệ thống. Cách tiếp cận này giúp [lợi ích].</p>
 * 
 * <p>Các quyết định thiết kế chính bao gồm:</p>
 * <ul>
 *   <li><strong>[Quyết định 1]</strong>: [Giải thích lý do]</li>
 *   <li><strong>[Quyết định 2]</strong>: [Giải thích lý do]</li>
 * </ul>
 * 
 * <p>Lớp này tương tác với [các lớp/module liên quan] để [mục đích tương tác].</p>
 */
```

Tương tự, với các phương thức quan trọng:

```java
/**
 * [Mô tả ngắn gọn về phương thức]
 * 
 * <p>Phương thức này được thiết kế để [lý do tồn tại], giải quyết [vấn đề cụ thể].
 * Cách tiếp cận [mô tả cách tiếp cận] được chọn thay vì [phương án thay thế] vì [lý do].</p>
 * 
 * <p>Quy trình xử lý bao gồm:</p>
 * <ol>
 *   <li>[Bước 1]</li>
 *   <li>[Bước 2]</li>
 * </ol>
 * 
 * @param paramName [Mô tả tham số và lý do cần nó]
 * @return [Mô tả giá trị trả về và ý nghĩa của nó]
 * @throws ExceptionType [Mô tả khi nào và tại sao ngoại lệ được ném]
 */
```

## Kết luận

Kiến trúc module của hệ thống VDT E-Commerce được thiết kế với mục tiêu cân bằng giữa tính module hóa cao và khả năng tích hợp mượt mà. Mỗi module tồn tại vì một lý do cụ thể và giải quyết một tập hợp các thách thức riêng biệt. Khi tiếp tục phát triển, việc duy trì ranh giới module rõ ràng và tuân thủ các nguyên tắc thiết kế đã đề ra sẽ giúp hệ thống dễ dàng mở rộng và bảo trì.
