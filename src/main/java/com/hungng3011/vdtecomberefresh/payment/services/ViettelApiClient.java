package com.hungng3011.vdtecomberefresh.payment.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hungng3011.vdtecomberefresh.payment.config.ViettelPaymentConfig;
import com.hungng3011.vdtecomberefresh.payment.dtos.viettel.*;
import com.hungng3011.vdtecomberefresh.payment.security.ViettelSignatureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Client giao tiếp với API Viettel Money.
 * 
 * <p>Lớp này được thiết kế dựa trên mẫu thiết kế Client Gateway, cung cấp 
 * một lớp trừu tượng hóa cho việc giao tiếp với dịch vụ bên ngoài (Viettel Money API).
 * Cách tiếp cận này giúp cô lập logic giao tiếp mạng khỏi logic nghiệp vụ.</p>
 * 
 * <p>Thiết kế này có một số ưu điểm quan trọng:</p>
 * <ul>
 *   <li><strong>Tách biệt mối quan tâm</strong>: Tách biệt hoàn toàn logic HTTP, chữ ký số,
 *       và xử lý lỗi khỏi logic nghiệp vụ thanh toán.</li>
 *   <li><strong>Dễ dàng thay thế</strong>: Nếu Viettel thay đổi API hoặc chúng ta muốn chuyển
 *       sang phương thức giao tiếp khác (như gRPC), chỉ cần thay đổi lớp này mà không
 *       ảnh hưởng đến phần còn lại của hệ thống.</li>
 *   <li><strong>Dễ kiểm thử</strong>: Có thể tạo mock dễ dàng cho lớp này trong quá trình kiểm thử.</li>
 * </ul>
 * 
 * <p>Lớp này chịu trách nhiệm:</p>
 * <ul>
 *   <li>Gửi các yêu cầu đến API Viettel Money</li>
 *   <li>Ký số các yêu cầu thông qua ViettelSignatureHandler</li>
 *   <li>Xác minh chữ ký số của các phản hồi</li>
 *   <li>Xử lý lỗi kết nối và phản hồi</li>
 *   <li>Chuyển đổi giữa các đối tượng DTO và JSON</li>
 * </ul>
 * 
 * <p>Lớp này sử dụng Apache HttpClient thay vì RestTemplate hoặc WebClient
 * để có kiểm soát chi tiết hơn đối với việc xử lý header và gọi HTTP.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ViettelApiClient {
    
    private final ViettelPaymentConfig config;
    private final ViettelSignatureHandler signatureHandler;
    private final ObjectMapper objectMapper;
    
    /**
     * Tạo giao dịch mới với Viettel Money.
     * 
     * <p>Phương thức này triển khai API Khởi tạo giao dịch của Viettel Money,
     * tuân theo quy trình xử lý sau:</p>
     * 
     * <ol>
     *   <li>Lấy thông tin cấu hình hiện tại dựa trên môi trường đang hoạt động</li>
     *   <li>Chuyển đổi đối tượng request thành chuỗi JSON</li>
     *   <li>Tạo chữ ký số từ chuỗi request bằng khóa riêng tư của merchant</li>
     *   <li>Thiết lập các header HTTP cần thiết (Content-Type, Authorization, Signature)</li>
     *   <li>Thực hiện request HTTP POST và đọc phản hồi</li>
     *   <li>Xác minh chữ ký của phản hồi sử dụng khóa công khai của Viettel</li>
     *   <li>Phân tích phản hồi JSON thành đối tượng Java</li>
     * </ol>
     * 
     * <p>Thiết kế xử lý lỗi trong phương thức này tuân theo nguyên tắc fail-fast,
     * ném ngoại lệ ngay khi phát hiện bất kỳ vấn đề nào để tránh xử lý không đúng.</p>
     * 
     * @param request Đối tượng chứa thông tin yêu cầu tạo giao dịch
     * @return Phản hồi từ Viettel Money chứa thông tin giao dịch đã tạo
     * @throws RuntimeException Khi xảy ra lỗi trong quá trình giao tiếp hoặc xác minh chữ ký
     */
    public ViettelTransactionInitiationResponse createTransaction(ViettelTransactionInitiationRequest request) {
        try {
            ViettelPaymentConfig.EnvironmentConfig env = config.getCurrentEnvironment();
            String url = env.getApiUrl() + "/v2/create-transaction";
            
            // Convert request to JSON
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Creating transaction with Viettel. OrderId: {}, Amount: {}", 
                    request.getOrderId(), request.getTransAmount());
            log.debug("Request URL: {}", url);
            log.debug("Request Body: {}", requestBody);
            
            // Sign the request
            String signature = signatureHandler.signMessage(requestBody, env.getPrivateKey());
            
            // Make HTTP request
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + env.getAccessToken());
                httpPost.setHeader("Signature", signature);
                httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
                
                try (ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null)) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    log.debug("Viettel Response Status: {}", response.getCode());
                    log.debug("Viettel Response Body: {}", responseBody);
                    
                    // Verify response signature
                    String responseSignature = response.getFirstHeader("Signature") != null ? 
                            response.getFirstHeader("Signature").getValue() : null;
                    
                    if (responseSignature == null || !signatureHandler.verifySignature(responseBody, responseSignature, env.getPublicKey())) {
                        log.error("Invalid signature in Viettel response for order: {}", request.getOrderId());
                        throw new RuntimeException("Invalid signature in Viettel response");
                    }
                    
                    ViettelTransactionInitiationResponse result = objectMapper.readValue(responseBody, ViettelTransactionInitiationResponse.class);
                    log.info("Transaction created successfully. OrderId: {}, VtRequestId: {}", 
                            request.getOrderId(), result.getData() != null ? result.getData().getVtRequestId() : "null");
                    
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Error creating transaction with Viettel for order: {}", request.getOrderId(), e);
            throw new RuntimeException("Failed to create transaction with Viettel", e);
        }
    }
    
    /**
     * Yêu cầu hoàn tiền từ Viettel Money
     * 
     * Phương thức này gửi yêu cầu hoàn tiền đến API Viettel Money,
     * ký số yêu cầu và xác minh chữ ký số của phản hồi.
     * 
     * @param request Đối tượng chứa thông tin yêu cầu hoàn tiền
     * @return Phản hồi từ Viettel Money về kết quả xử lý hoàn tiền
     * @throws RuntimeException Khi xảy ra lỗi trong quá trình giao tiếp hoặc xác minh chữ ký
     */
    public ViettelRefundResponse refundTransaction(ViettelRefundRequest request) {
        try {
            ViettelPaymentConfig.EnvironmentConfig env = config.getCurrentEnvironment();
            String url = env.getApiUrl() + "/v3/merchant/refund-transaction";
            
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Requesting refund from Viettel. OrderId: {}, OriginalRequestId: {}, Amount: {}", 
                    request.getOrderId(), request.getOriginalRequestId(), request.getTransAmount());
            log.debug("Refund Request URL: {}", url);
            log.debug("Refund Request Body: {}", requestBody);
            
            String signature = signatureHandler.signMessage(requestBody, env.getPrivateKey());
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + env.getAccessToken());
                httpPost.setHeader("Signature", signature);
                httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
                
                try (ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null)) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    log.debug("Viettel Refund Response Status: {}", response.getCode());
                    log.debug("Viettel Refund Response Body: {}", responseBody);
                    
                    String responseSignature = response.getFirstHeader("Signature") != null ? 
                            response.getFirstHeader("Signature").getValue() : null;
                    
                    if (responseSignature == null || !signatureHandler.verifySignature(responseBody, responseSignature, env.getPublicKey())) {
                        log.error("Invalid signature in Viettel refund response for order: {}", request.getOrderId());
                        throw new RuntimeException("Invalid signature in Viettel refund response");
                    }
                    
                    ViettelRefundResponse result = objectMapper.readValue(responseBody, ViettelRefundResponse.class);
                    log.info("Refund processed. OrderId: {}, Status: {}", request.getOrderId(), result.getStatus());
                    
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Error processing refund with Viettel for order: {}", request.getOrderId(), e);
            throw new RuntimeException("Failed to process refund with Viettel", e);
        }
    }
    
    /**
     * Truy vấn trạng thái giao dịch từ Viettel Money
     * 
     * Phương thức này gửi yêu cầu truy vấn đến API Viettel Money để 
     * lấy thông tin về trạng thái giao dịch, ký số yêu cầu và 
     * xác minh chữ ký số của phản hồi.
     * 
     * @param request Đối tượng chứa thông tin yêu cầu truy vấn
     * @return Phản hồi từ Viettel Money chứa thông tin trạng thái giao dịch
     * @throws RuntimeException Khi xảy ra lỗi trong quá trình giao tiếp hoặc xác minh chữ ký
     */
    public ViettelQueryTransactionResponse queryTransaction(ViettelQueryTransactionRequest request) {
        try {
            ViettelPaymentConfig.EnvironmentConfig env = config.getCurrentEnvironment();
            String url = env.getApiUrl() + "/v3/merchant/search-transaction";
            
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Querying transaction from Viettel. OrderId: {}, OriginalRequestId: {}", 
                    request.getOrderId(), request.getOriginalRequestId());
            log.debug("Query Request URL: {}", url);
            log.debug("Query Request Body: {}", requestBody);
            
            String signature = signatureHandler.signMessage(requestBody, env.getPrivateKey());
            
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("Authorization", "Bearer " + env.getAccessToken());
                httpPost.setHeader("Signature", signature);
                httpPost.setEntity(new StringEntity(requestBody, StandardCharsets.UTF_8));
                
                try (ClassicHttpResponse response = httpClient.executeOpen(null, httpPost, null)) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8);
                    log.debug("Viettel Query Response Status: {}", response.getCode());
                    log.debug("Viettel Query Response Body: {}", responseBody);
                    
                    String responseSignature = response.getFirstHeader("Signature") != null ? 
                            response.getFirstHeader("Signature").getValue() : null;
                    
                    if (responseSignature == null || !signatureHandler.verifySignature(responseBody, responseSignature, env.getPublicKey())) {
                        log.error("Invalid signature in Viettel query response");
                        throw new RuntimeException("Invalid signature in Viettel query response");
                    }
                    
                    ViettelQueryTransactionResponse result = objectMapper.readValue(responseBody, ViettelQueryTransactionResponse.class);
                    log.info("Transaction query completed. Results count: {}", 
                            result.getData() != null ? result.getData().size() : 0);
                    
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Error querying transaction from Viettel", e);
            throw new RuntimeException("Failed to query transaction from Viettel", e);
        }
    }
}
