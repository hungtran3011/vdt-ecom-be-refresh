package com.hungng3011.vdtecomberefresh.payment.services;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import com.hungng3011.vdtecomberefresh.exception.payment.PaymentProcessingException;
import com.hungng3011.vdtecomberefresh.exception.payment.PaymentGatewayException;
import com.hungng3011.vdtecomberefresh.exception.payment.RefundException;
import com.hungng3011.vdtecomberefresh.exception.payment.InvalidOrderStateException;
import com.hungng3011.vdtecomberefresh.mail.services.NotificationService;
import com.hungng3011.vdtecomberefresh.order.entities.Order;
import com.hungng3011.vdtecomberefresh.order.enums.OrderStatus;
import com.hungng3011.vdtecomberefresh.order.enums.PaymentMethod;
import com.hungng3011.vdtecomberefresh.order.repositories.OrderRepository;
import com.hungng3011.vdtecomberefresh.payment.config.ViettelPaymentConfig;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelTransactionInitiationRequest;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelTransactionInitiationResponse;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelRefundRequest;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelRefundResponse;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelQueryTransactionRequest;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.ViettelQueryTransactionResponse;
import com.hungng3011.vdtecomberefresh.payment.utils.PaymentStatusUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Dịch vụ quản lý các giao dịch thanh toán Viettel Money.
 * 
 * <p>Lớp này là thành phần trung tâm trong kiến trúc tích hợp thanh toán,
 * cung cấp giao diện cao cấp cho các hoạt động thanh toán và đóng vai trò
 * điều phối giữa các lớp khác nhau trong hệ thống.</p>
 * 
 * <p>Thiết kế của lớp này áp dụng các nguyên tắc sau:</p>
 * <ul>
 *   <li><strong>Facade Pattern</strong>: Cung cấp giao diện đơn giản hóa cho hệ thống phức tạp
 *       bên dưới, ẩn đi các chi tiết giao tiếp API và xử lý chữ ký số.</li>
 *   <li><strong>Single Responsibility</strong>: Tập trung vào quản lý quy trình thanh toán, 
 *       ủy thác việc giao tiếp HTTP cho ViettelApiClient.</li>
 *   <li><strong>Transactional Processing</strong>: Sử dụng @Transactional để đảm bảo
 *       tính nhất quán của dữ liệu trong các hoạt động thanh toán.</li>
 * </ul>
 * 
 * <p>Lớp này cung cấp các phương thức chính:</p>
 * <ul>
 *   <li>Khởi tạo giao dịch thanh toán</li>
 *   <li>Xử lý hoàn tiền</li>
 *   <li>Truy vấn trạng thái giao dịch</li>
 *   <li>Cập nhật trạng thái đơn hàng dựa trên kết quả thanh toán</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViettelPaymentService {
    
    private final ViettelApiClient viettelApiClient;
    private final ViettelPaymentConfig config;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    
    /**
     * Khởi tạo thanh toán cho đơn hàng sử dụng Viettel Money.
     * 
     * <p>Phương thức này triển khai quy trình thanh toán theo mô hình Saga đơn giản,
     * đảm bảo tính nhất quán của dữ liệu trong quá trình thanh toán bằng cách:</p>
     * 
     * <ol>
     *   <li>Kiểm tra và xác nhận trạng thái đơn hàng trước khi xử lý</li>
     *   <li>Tạo yêu cầu thanh toán với Viettel Money thông qua ViettelApiClient</li>
     *   <li>Cập nhật thông tin thanh toán vào đơn hàng khi và chỉ khi giao dịch được khởi tạo thành công</li>
     * </ol>
     * 
     * <p>Thiết kế sử dụng @Transactional để đảm bảo rằng tất cả các thay đổi đối với
     * đơn hàng sẽ được hoàn tác nếu có bất kỳ lỗi nào xảy ra trong quá trình xử lý.
     * Cách tiếp cận này giúp duy trì tính toàn vẹn dữ liệu.</p>
     * 
     * <p>Phương thức này hỗ trợ nhiều phương thức hiển thị thanh toán (returnType) để
     * đáp ứng các nhu cầu khách hàng khác nhau:</p>
     * <ul>
     *   <li>WEB: Chuyển hướng đến trang thanh toán Viettel (phù hợp cho desktop)</li>
     *   <li>QR: Hiển thị mã QR để quét (phù hợp cho ứng dụng web di động)</li>
     *   <li>DEEPLINK: Tạo liên kết sâu mở ứng dụng Viettel Money (phù hợp cho ứng dụng di động)</li>
     * </ul>
     * 
     * @param orderId Mã đơn hàng cần thanh toán
     * @param returnType Loại giao diện trả về (WEB, QR, DEEPLINK)
     * @return Phản hồi từ Viettel Money chứa thông tin thanh toán
     * @throws PaymentProcessingException Khi xảy ra lỗi trong quá trình xử lý thanh toán
     * @throws EntityNotFoundException Khi không tìm thấy đơn hàng
     * @throws InvalidOrderStateException Khi đơn hàng không ở trạng thái phù hợp để thanh toán
     * @throws PaymentGatewayException Khi có lỗi từ cổng thanh toán Viettel Money
     */
    @Transactional
    public ViettelTransactionInitiationResponse initiatePayment(String orderId, String returnType) {
        try {
            // Find the order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
            
            // Validate order can be paid
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                throw new InvalidOrderStateException("Order is not in pending payment status: " + orderId);
            }
            
            // Convert amount to VND cents (smallest unit)
            BigDecimal totalPrice = order.getTotalPrice();
            Long amountInCents = totalPrice.multiply(new BigDecimal(100)).longValue();
            
            // Build request
            ViettelTransactionInitiationRequest.CustomerInfo customerInfo = 
                    ViettelTransactionInitiationRequest.CustomerInfo.builder()
                            .customerPhone(order.getPhone())
                            .customerAddress(order.getAddress())
                            .build();
            
            ViettelTransactionInitiationRequest request = ViettelTransactionInitiationRequest.builder()
                    .orderId(orderId)
                    .transAmount(amountInCents)
                    .description("Payment for order " + orderId)
                    .returnType(returnType != null ? returnType : config.getSettings().getDefaultReturnType())
                    .returnUrl(config.getRedirectUrl())
                    .cancelUrl(config.getRedirectUrl())
                    .expireAfter(config.getSettings().getDefaultExpireAfterMinutes())
                    .customerInfo(customerInfo)
                    .build();
            
            // Call Viettel API
            ViettelTransactionInitiationResponse response = viettelApiClient.createTransaction(request);
            
            if (!"SUCCESS".equals(response.getStatus()) && !"00".equals(response.getStatus())) {
                log.error("Failed to create transaction with Viettel. OrderId: {}, Status: {}, Message: {}", 
                        orderId, response.getStatus(), response.getMessage());
                
                // Delete the order when payment initialization fails
                try {
                    log.info("Deleting order {} due to payment initialization failure", orderId);
                    orderRepository.deleteById(orderId);
                    log.info("Order {} successfully deleted after payment initialization failure", orderId);
                } catch (Exception deleteException) {
                    log.error("Failed to delete order {} after payment initialization failure", orderId, deleteException);
                    // Continue with the original exception
                }
                
                throw new PaymentGatewayException("Failed to create transaction with Viettel: " + response.getMessage());
            }
            
            // Update order with payment info
            order.setPaymentMethod(PaymentMethod.VIETTEL_MONEY);
            order.setPaymentId(response.getData().getVtRequestId());
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            
            log.info("Payment initiated successfully. OrderId: {}, VtRequestId: {}, ReturnType: {}", 
                    orderId, response.getData().getVtRequestId(), returnType);
            
            return response;
            
        } catch (PaymentGatewayException e) {
            // Re-throw payment gateway exceptions (order deletion already handled above)
            throw e;
        } catch (EntityNotFoundException | InvalidOrderStateException e) {
            // These are validation exceptions, don't delete the order
            log.error("Validation error for payment initiation. OrderId: {}", orderId, e);
            throw new PaymentProcessingException("Failed to initiate payment for order: " + orderId, e);
        } catch (Exception e) {
            log.error("Unexpected error initiating payment for order: {}", orderId, e);
            
            // Delete the order when unexpected error occurs during payment initialization
            try {
                log.info("Deleting order {} due to unexpected payment initialization error", orderId);
                orderRepository.deleteById(orderId);
                log.info("Order {} successfully deleted after unexpected payment initialization error", orderId);
            } catch (Exception deleteException) {
                log.error("Failed to delete order {} after unexpected payment initialization error", orderId, deleteException);
                // Continue with the original exception
            }
            
            throw new PaymentProcessingException("Failed to initiate payment for order: " + orderId, e);
        }
    }
    
    /**
     * Xử lý hoàn tiền cho đơn hàng.
     * 
     * <p>Phương thức này triển khai quy trình hoàn tiền theo mẫu thiết kế two-phase commit,
     * đảm bảo rằng trạng thái đơn hàng trong hệ thống của chúng ta chỉ được cập nhật sau
     * khi hoàn tiền thành công từ phía Viettel Money.</p>
     * 
     * <p>Quy trình chi tiết bao gồm:</p>
     * <ol>
     *   <li>Kiểm tra và xác thực thông tin đơn hàng từ cơ sở dữ liệu</li>
     *   <li>Tạo mã hoàn tiền độc đáo để tránh trùng lặp và dễ dàng theo dõi</li>
     *   <li>Gửi yêu cầu hoàn tiền đến Viettel Money thông qua ViettelApiClient</li>
     *   <li>Phân tích phản hồi và chỉ cập nhật trạng thái đơn hàng khi hoàn tiền thành công</li>
     *   <li>Gửi thông báo xác nhận hoàn tiền đến khách hàng qua email</li>
     * </ol>
     * 
     * <p>Phương thức này được thiết kế với khả năng phân tách lỗi (fault isolation),
     * đảm bảo rằng lỗi trong quá trình gửi email sẽ không ảnh hưởng đến việc hoàn tất
     * quá trình hoàn tiền. Điều này quan trọng để tránh các vấn đề về nhất quán dữ liệu.</p>
     * 
     * <p>Lưu ý quan trọng về giới hạn API:</p>
     * <ul>
     *   <li>Hoàn tiền chỉ được hỗ trợ cho giao dịch thanh toán từ tài khoản Viettel Money hoặc ngân hàng</li>
     *   <li>Không hỗ trợ hoàn tiền cho giao dịch từ các ví điện tử khác</li>
     *   <li>Có giới hạn thời gian cho hoàn tiền (thông thường là 30 ngày kể từ giao dịch gốc)</li>
     * </ul>
     * 
     * @param orderId Mã đơn hàng cần hoàn tiền
     * @param refundAmount Số tiền cần hoàn (đơn vị: xu VNĐ, 1 VNĐ = 100 xu)
     * @param reason Lý do hoàn tiền
     * @return Phản hồi từ Viettel Money về kết quả hoàn tiền
     * @throws RefundException Khi xảy ra lỗi trong quá trình xử lý hoàn tiền
     * @throws EntityNotFoundException Khi không tìm thấy đơn hàng
     * @throws InvalidOrderStateException Khi đơn hàng không có thông tin thanh toán
     */
    @Transactional
    public ViettelRefundResponse processRefund(String orderId, Long refundAmount, String reason) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
            
            if (order.getPaymentId() == null) {
                throw new InvalidOrderStateException("Order has no payment ID for refund: " + orderId);
            }
            
            // Generate refund order ID
            String refundOrderId = "REFUND_" + orderId + "_" + System.currentTimeMillis();
            
            ViettelRefundRequest request = ViettelRefundRequest.builder()
                    .transAmount(refundAmount)
                    .orderId(refundOrderId)
                    .originalRequestId(order.getPaymentId())
                    .description(reason != null ? reason : "Refund for order " + orderId)
                    .build();
            
            ViettelRefundResponse response = viettelApiClient.refundTransaction(request);
            
            if ("SUCCESS".equals(response.getStatus()) || "00".equals(response.getStatus())) {
                // Update order status
                order.setPaymentStatus(PaymentStatus.REFUNDED);
                order.setStatus(OrderStatus.CANCELLED);
                order.setUpdatedAt(LocalDateTime.now());
                orderRepository.save(order);
                
                // Send refund confirmation email
                try {
                    String customerEmail = order.getUserEmail(); // Use email directly from order
                    if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                        log.info("Sending refund confirmation email for order: {}", orderId);
                        BigDecimal refundAmountDecimal = new BigDecimal(refundAmount).divide(new BigDecimal(100));
                        notificationService.sendRefundConfirmationEmail(
                                orderId, 
                                customerEmail, 
                                response.getData().getVtRequestId(),
                                refundAmountDecimal
                        );
                    } else {
                        log.warn("No email address found for user: {}, skipping refund confirmation email for order: {}", 
                                order.getUserEmail(), orderId);
                    }
                } catch (Exception e) {
                    log.error("Failed to send refund confirmation email for order: {}", orderId, e);
                    // Don't fail the refund if email fails
                }
                
                log.info("Refund processed successfully. OrderId: {}, RefundOrderId: {}, Amount: {}", 
                        orderId, refundOrderId, refundAmount);
            } else {
                log.error("Failed to process refund with Viettel. OrderId: {}, Status: {}, Message: {}", 
                        orderId, response.getStatus(), response.getMessage());
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing refund for order: {}", orderId, e);
            throw new RefundException("Failed to process refund for order: " + orderId, e);
        }
    }
    
    /**
     * Truy vấn trạng thái giao dịch từ Viettel
     * 
     * Phương thức này gửi yêu cầu truy vấn đến Viettel Money để kiểm tra 
     * trạng thái của giao dịch dựa trên mã đơn hàng.
     * 
     * @param orderId Mã đơn hàng cần kiểm tra
     * @return Dữ liệu trạng thái giao dịch, hoặc null nếu không tìm thấy hoặc xảy ra lỗi
     */
    public ViettelQueryTransactionResponse.TransactionQueryData queryTransactionStatus(String orderId) {
        try {
            ViettelQueryTransactionRequest request = ViettelQueryTransactionRequest.builder()
                    .orderId(orderId)
                    .build();
            
            ViettelQueryTransactionResponse response = viettelApiClient.queryTransaction(request);
            
            if (response.getData() != null && !response.getData().isEmpty()) {
                return response.getData().get(0);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error querying transaction status for order: {}", orderId, e);
            return null;
        }
    }
    
    /**
     * Cập nhật trạng thái đơn hàng dựa trên kết quả thanh toán.
     * 
     * <p>Phương thức này triển khai mô hình xử lý sự kiện (Event Processing) để 
     * phản ứng với kết quả thanh toán từ Viettel Money. Điều này cho phép
     * hệ thống phản ứng không đồng bộ với các sự kiện thanh toán, tăng tính
     * mở rộng và khả năng chịu lỗi của hệ thống.</p>
     * 
     * <p>Thiết kế của phương thức này giải quyết nhiều thách thức phổ biến trong xử lý thanh toán:</p>
     * <ul>
     *   <li><strong>Xử lý dữ liệu trùng lặp</strong>: Có thể nhận nhiều thông báo cho cùng một giao dịch
     *       (từ IPN và từ redirect), nhưng chỉ cập nhật đơn hàng một lần</li>
     *   <li><strong>Xử lý lỗi chuyển trạng thái</strong>: Đảm bảo rằng đơn hàng chỉ chuyển từ trạng thái
     *       đang chờ thanh toán sang đã thanh toán hoặc thanh toán thất bại</li>
     *   <li><strong>Ghi log chi tiết</strong>: Ghi lại tất cả các thông tin giao dịch để phục vụ
     *       cho việc theo dõi, đối soát và xử lý sự cố</li>
     * </ul>
     * 
     * <p>Lưu ý rằng phương thức này được thiết kế để chạy trong một giao dịch (transaction)
     * để đảm bảo rằng tất cả các thay đổi đối với đơn hàng sẽ được hoàn tác nếu có lỗi xảy ra
     * trong quá trình xử lý.</p>
     * 
     * @param orderId Mã đơn hàng cần cập nhật
     * @param transactionStatus Mã trạng thái giao dịch (1 = thành công)
     * @param errorCode Mã lỗi (00 = không có lỗi)
     * @param vtRequestId Mã yêu cầu từ Viettel Money
     */
    @Transactional
    public void updateOrderPaymentStatus(String orderId, Integer transactionStatus, String errorCode, String vtRequestId) {
        try {
            Optional<Order> orderOpt = orderRepository.findById(orderId);
            if (orderOpt.isEmpty()) {
                log.warn("Order not found for payment update: {}", orderId);
                return;
            }
            
            Order order = orderOpt.get();
            
            if (transactionStatus == 1 && "00".equals(errorCode)) {
                // Payment successful
                order.setStatus(OrderStatus.PAID);
                order.setPaymentStatus(PaymentStatus.SUCCESSFUL);
                
                // Send payment success email
                try {
                    String customerEmail = order.getUserEmail(); // Use email directly from order
                    if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                        log.info("Sending payment success email for order: {}", orderId);
                        BigDecimal totalAmount = order.getTotalPrice();
                        notificationService.sendPaymentSuccessEmail(
                                orderId, 
                                customerEmail, 
                                vtRequestId != null ? vtRequestId : order.getPaymentId(),
                                totalAmount
                        );
                    } else {
                        log.warn("No email address found for user: {}, skipping payment success email for order: {}", 
                                order.getUserEmail(), orderId);
                    }
                } catch (Exception e) {
                    log.error("Failed to send payment success email for order: {}", orderId, e);
                    // Don't fail the payment update if email fails
                }
                
                log.info("Order payment completed successfully: {}", orderId);
            } else {
                // Payment failed
                order.setStatus(OrderStatus.PAYMENT_FAILED);
                order.setPaymentStatus(PaymentStatus.FAILED);
                
                // Send payment failure email
                try {
                    String customerEmail = order.getUserEmail(); // Use email directly from order
                    if (customerEmail != null && !customerEmail.trim().isEmpty()) {
                        log.info("Sending payment failed email for order: {}", orderId);
                        String errorMessage = "Payment processing failed with error code: " + errorCode;
                        notificationService.sendPaymentFailedEmail(
                                orderId, 
                                customerEmail, 
                                vtRequestId != null ? vtRequestId : order.getPaymentId(),
                                errorMessage
                        );
                    } else {
                        log.warn("No email address found for user: {}, skipping payment failed email for order: {}", 
                                order.getUserEmail(), orderId);
                    }
                } catch (Exception e) {
                    log.error("Failed to send payment failed email for order: {}", orderId, e);
                    // Don't fail the payment update if email fails
                }
                
                log.warn("Order payment failed. OrderId: {}, Status: {}, ErrorCode: {}", 
                        orderId, transactionStatus, errorCode);
                
                // Delete the order when payment fails
                try {
                    log.info("Deleting order {} due to payment failure", orderId);
                    orderRepository.deleteById(orderId);
                    log.info("Order {} successfully deleted after payment failure", orderId);
                    return; // Exit early since order is deleted
                } catch (Exception deleteException) {
                    log.error("Failed to delete order {} after payment failure, keeping order in failed state", orderId, deleteException);
                    // Continue to save the order in failed state if deletion fails
                }
            }
            
            if (vtRequestId != null) {
                order.setPaymentId(vtRequestId);
            }
            
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            
        } catch (Exception e) {
            log.error("Error updating order payment status. OrderId: {}", orderId, e);
        }
    }
    
    /**
     * Lấy tên môi trường hiện tại
     * 
     * @return Tên môi trường (sandbox, preprod hoặc production)
     */
    public String getCurrentEnvironment() {
        return config.getActiveEnvironment();
    }
}
