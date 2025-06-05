package com.hungng3011.vdtecomberefresh.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Cấu hình cho tích hợp thanh toán Viettel Money.
 * 
 * Lớp này đọc cấu hình từ file viettel-payment.yml và cung cấp các
 * thông tin cần thiết cho việc kết nối với API Viettel Money.
 * 
 * <p>Thiết kế này sử dụng mẫu thiết kế cấu hình ngoài (Externalized Configuration Pattern),
 * cho phép thay đổi cấu hình mà không cần thay đổi mã nguồn. Việc thiết kế 
 * với nhiều môi trường khác nhau (sandbox, preprod, production) trong một cấu hình 
 * cho phép chuyển đổi môi trường một cách dễ dàng thông qua cấu hình activeEnvironment.</p>
 * 
 * <p>Cấu trúc cấu hình được chia thành 3 phần chính để đảm bảo tính tổ chức và mở rộng:</p>
 * <ul>
 *   <li>EnvironmentConfig: Cấu hình đặc thù cho từng môi trường (API URL, token, khóa)</li>
 *   <li>PartnerConfig: Cấu hình endpoint của đối tác để Viettel gọi ngược lại</li>
 *   <li>SettingsConfig: Cài đặt chung cho toàn bộ tích hợp</li>
 * </ul>
 * 
 * <p>Thiết kế này tuân theo nguyên tắc đơn trách nhiệm, giúp tách biệt việc
 * quản lý cấu hình khỏi logic xử lý thanh toán.</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "viettel.payment")
public class ViettelPaymentConfig {
    
    private Map<String, EnvironmentConfig> environments;
    private String activeEnvironment;
    private PartnerConfig partner;
    private SettingsConfig settings;
    
    /**
     * Cấu hình cho một môi trường cụ thể (sandbox, preprod, production).
     * 
     * <p>Lớp này được thiết kế dựa trên mẫu Value Object, chứa toàn bộ cấu hình
     * cho một môi trường Viettel Money cụ thể. Cách thiết kế này cho phép:</p>
     * 
     * <ul>
     *   <li>Chuyển đổi nhanh chóng giữa các môi trường mà không cần thay đổi mã nguồn</li>
     *   <li>Dễ dàng thêm môi trường mới (ví dụ: môi trường kiểm thử đặc biệt)</li>
     *   <li>Cách ly cấu hình giữa các môi trường, ngăn ngừa rủi ro trộn lẫn</li>
     * </ul>
     * 
     * <p>Việc sử dụng privateKey và publicKey tuân theo chuẩn bảo mật ECDSA để 
     * đảm bảo tính toàn vẹn và xác thực của dữ liệu trao đổi.</p>
     */
    @Data
    public static class EnvironmentConfig {
        /**
         * URL cơ sở của API Viettel Money cho môi trường này.
         * 
         * <p>Mỗi môi trường Viettel Money sẽ có một endpoint API khác nhau.
         * Định dạng URL phụ thuộc vào môi trường:
         * <ul>
         *   <li>Sandbox: http://125.235.38.xxx:8080/paybiz/payment-gateway/public/api</li>
         *   <li>Preprod: https://api24cdn.vtmoney.vn/uatmm/paybiz/payment-gateway/public/api</li>
         *   <li>Production: https://api23.vtmoney.vn/paybiz/payment-gateway/public/api</li>
         * </ul></p>
         */
        private String apiUrl;
        
        /**
         * Mã merchant được Viettel cấp.
         * 
         * <p>Đây là mã định danh duy nhất cho đối tác, được Viettel cung cấp
         * khi đăng ký sử dụng dịch vụ. Mỗi môi trường sẽ có một mã merchant riêng.</p>
         */
        private String merchantCode;
        
        /**
         * Token xác thực cho API.
         * 
         * <p>Access token được sử dụng trong header Authorization của mọi request
         * gửi đến Viettel Money API. Token này cần được bảo mật nghiêm ngặt và
         * không được hardcode trong mã nguồn.</p>
         */
        private String accessToken;
        
        /**
         * Khóa công khai của merchant, dùng để xác thực với Viettel.
         * 
         * <p>Đây là khóa công khai ECDSA được mã hóa Base64, được chia sẻ với Viettel
         * để họ có thể xác minh chữ ký số trong các request từ hệ thống của chúng ta.</p>
         */
        private String publicKey;
        
        /**
         * Khóa riêng tư của merchant, dùng để ký các request.
         * 
         * <p>Đây là khóa riêng tư ECDSA được mã hóa Base64, được sử dụng để tạo
         * chữ ký số cho các request gửi đến Viettel Money API. Khóa này phải được
         * bảo mật cao độ và không bao giờ được chia sẻ.</p>
         */
        private String privateKey;
        
        /**
         * Khóa công khai của Viettel, dùng để xác minh phản hồi.
         * 
         * <p>Đây là khóa công khai ECDSA được mã hóa Base64 do Viettel cung cấp,
         * được sử dụng để xác minh tính xác thực của các phản hồi từ Viettel Money API.</p>
         */
        private String partnerPublicKey;
    }
    
    /**
     * Cấu hình cho các endpoint của đối tác (ứng dụng của chúng ta)
     * mà Viettel Money sẽ gọi đến.
     * 
     * <p>Thiết kế này tuân theo mẫu thiết kế Inversion of Control, cho phép
     * Viettel Money (hệ thống bên ngoài) chủ động gửi thông báo đến các
     * endpoint được cấu hình, thay vì hệ thống của chúng ta phải liên tục
     * kiểm tra trạng thái thanh toán.</p>
     * 
     * <p>Việc tách biệt baseUrl và các đường dẫn chi tiết cho phép:</p>
     * <ul>
     *   <li>Dễ dàng chuyển đổi giữa các môi trường (develop, staging, production)</li>
     *   <li>Tái sử dụng một baseUrl chung cho nhiều endpoint</li>
     *   <li>Thay đổi cấu trúc URL mà không ảnh hưởng đến toàn bộ hệ thống</li>
     * </ul>
     */
    @Data
    public static class PartnerConfig {
        /**
         * URL cơ sở của ứng dụng.
         * 
         * <p>Đây là địa chỉ gốc của ứng dụng web, nơi Viettel Money sẽ gửi
         * các callback và redirect. URL này phải có thể truy cập được từ internet
         * và thường có dạng https://yourdomain.com.</p>
         */
        private String baseUrl;
        
        /**
         * Đường dẫn để nhận xác nhận đơn hàng.
         * 
         * <p>Endpoint này được Viettel gọi đến trước khi hiển thị trang thanh toán
         * để xác minh thông tin đơn hàng. Hệ thống cần kiểm tra và phản hồi về
         * tính hợp lệ của đơn hàng.</p>
         * 
         * <p>Ví dụ: /api/viettel/order-confirmation</p>
         */
        private String orderConfirmationPath;
        
        /**
         * Đường dẫn để nhận thông báo thanh toán tức thời (IPN).
         * 
         * <p>Endpoint này được Viettel gọi đến ngay sau khi khách hàng hoàn tất thanh toán,
         * kể cả khi khách hàng không quay lại trang web của chúng ta. Đây là cơ chế
         * đáng tin cậy nhất để cập nhật trạng thái đơn hàng.</p>
         * 
         * <p>Ví dụ: /api/viettel/ipn</p>
         */
        private String ipnPath;
        
        /**
         * Đường dẫn để chuyển hướng sau khi thanh toán.
         * 
         * <p>Endpoint này là nơi khách hàng sẽ được chuyển hướng đến sau khi
         * hoàn tất quá trình thanh toán trên trang web của Viettel. Đây là cơ chế
         * để cung cấp trải nghiệm người dùng liền mạch.</p>
         * 
         * <p>Ví dụ: /payment/viettel/result</p>
         */
        private String redirectPath;
    }
    
    /**
     * Các cài đặt chung cho tích hợp thanh toán.
     * 
     * <p>Lớp này chứa các thông số chung ảnh hưởng đến hoạt động của cổng thanh toán.
     * Việc tách các cài đặt này thành một lớp riêng (thay vì đặt trong lớp chính)
     * tuân theo nguyên tắc phân tách mối quan tâm (Separation of Concerns) và 
     * cho phép thay đổi cấu hình mà không ảnh hưởng đến logic xử lý.</p>
     * 
     * <p>Thiết kế này còn giúp dễ dàng mở rộng thêm các cài đặt mới trong tương lai
     * mà không cần thay đổi cấu trúc cấu hình hiện tại.</p>
     */
    @Data
    public static class SettingsConfig {
        /**
         * Loại giao diện trả về mặc định (WEB, QR, DEEPLINK).
         * 
         * <p>Xác định cách thức thanh toán mặc định khi không có chỉ định cụ thể:</p>
         * <ul>
         *   <li>WEB: Chuyển hướng người dùng đến trang thanh toán của Viettel</li>
         *   <li>QR: Trả về mã QR để hiển thị trên trang web của đối tác</li>
         *   <li>DEEPLINK: Trả về deep link để mở ứng dụng Viettel Money trên thiết bị di động</li>
         * </ul>
         */
        private String defaultReturnType;
        
        /**
         * Thời gian hết hạn mặc định cho giao dịch (phút).
         * 
         * <p>Xác định thời gian tối đa mà khách hàng có thể hoàn tất thanh toán
         * kể từ khi khởi tạo giao dịch. Sau thời gian này, giao dịch sẽ tự động bị hủy
         * và khách hàng phải bắt đầu lại từ đầu.</p>
         * 
         * <p>Giá trị này cần được cân nhắc kỹ lưỡng: quá ngắn sẽ gây bất tiện cho khách hàng,
         * quá dài có thể gây ra vấn đề về quản lý hàng tồn kho.</p>
         */
        private Integer defaultExpireAfterMinutes;
        
        /**
         * Số lần thử lại tối đa khi gặp lỗi.
         * 
         * <p>Xác định số lần tối đa mà hệ thống sẽ tự động thử lại khi gặp lỗi
         * kết nối đến API Viettel Money. Cơ chế này giúp tăng tính ổn định
         * và giảm thiểu tác động của các lỗi tạm thời.</p>
         */
        private Integer maxRetryCount;
        
        /**
         * Thời gian giữa các lần thử lại (phút).
         * 
         * <p>Xác định khoảng thời gian chờ giữa các lần thử lại khi gặp lỗi kết nối.
         * Giá trị này nên đủ lớn để cho phép khắc phục các lỗi tạm thời, nhưng không
         * quá lớn để tránh trì hoãn quá trình thanh toán.</p>
         */
        private Integer retryIntervalMinutes;
    }
    
    /**
     * Lấy cấu hình cho môi trường đang hoạt động hiện tại.
     * 
     * <p>Phương thức này áp dụng mẫu thiết kế Strategy, cho phép ứng dụng
     * hoạt động với các cấu hình khác nhau mà không cần biết chi tiết
     * về môi trường cụ thể đang được sử dụng.</p>
     * 
     * @return Đối tượng EnvironmentConfig chứa cấu hình của môi trường hiện tại
     */
    public EnvironmentConfig getCurrentEnvironment() {
        return environments.get(activeEnvironment);
    }
    
    /**
     * Xây dựng URL đầy đủ cho API xác nhận đơn hàng.
     * 
     * <p>Phương thức này kết hợp baseUrl và orderConfirmationPath để tạo
     * URL hoàn chỉnh mà Viettel sẽ sử dụng để gọi API xác nhận đơn hàng 
     * của đối tác.</p>
     * 
     * <p>Cách thiết kế này giúp tránh lỗi khi nối URL và đảm bảo
     * tính nhất quán trong toàn bộ hệ thống.</p>
     * 
     * @return URL đầy đủ cho API xác nhận đơn hàng
     */
    public String getOrderConfirmationUrl() {
        return partner.getBaseUrl() + partner.getOrderConfirmationPath();
    }
    
    /**
     * Xây dựng URL đầy đủ cho API nhận thông báo thanh toán tức thời (IPN).
     * 
     * <p>Phương thức này kết hợp baseUrl và ipnPath để tạo URL hoàn chỉnh
     * mà Viettel sẽ sử dụng để gửi thông báo kết quả thanh toán tức thời.</p>
     * 
     * <p>IPN là cơ chế quan trọng nhất để nhận kết quả thanh toán một cách đáng tin cậy,
     * do đó URL này cần được cấu hình và kiểm tra cẩn thận.</p>
     * 
     * @return URL đầy đủ cho API nhận thông báo thanh toán tức thời
     */
    public String getIpnUrl() {
        return partner.getBaseUrl() + partner.getIpnPath();
    }
    
    /**
     * Xây dựng URL đầy đủ cho trang chuyển hướng sau khi thanh toán.
     * 
     * <p>Phương thức này kết hợp baseUrl và redirectPath để tạo URL hoàn chỉnh
     * mà Viettel sẽ sử dụng để chuyển hướng khách hàng sau khi hoàn tất thanh toán
     * trên trang web của Viettel.</p>
     * 
     * <p>URL này là một phần quan trọng của trải nghiệm người dùng, cần đảm bảo
     * rằng nó dẫn đến một trang thông báo phù hợp về kết quả thanh toán.</p>
     * 
     * @return URL đầy đủ cho trang chuyển hướng sau khi thanh toán
     */
    public String getRedirectUrl() {
        return partner.getBaseUrl() + partner.getRedirectPath();
    }
}
