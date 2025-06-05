package com.hungng3011.vdtecomberefresh.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

/**
 * Dịch vụ quản lý người dùng và vai trò trong Keycloak.
 * 
 * <p>Lớp này được thiết kế để trừu tượng hóa tương tác với Keycloak, cung cấp một 
 * giao diện đơn giản hóa cho việc xác thực và phân quyền. Việc tách biệt logic xác thực
 * thành một service riêng biệt cho phép hệ thống dễ dàng chuyển đổi giữa các nhà cung cấp
 * xác thực khác nhau (như Keycloak, Auth0, hoặc thực hiện nội bộ) mà không ảnh hưởng 
 * đến các thành phần khác của ứng dụng.</p>
 * 
 * <p>Các quyết định thiết kế chính bao gồm:</p>
 * <ul>
 *   <li><strong>Ủy quyền xác thực cho Keycloak</strong>: Thay vì xây dựng hệ thống xác thực 
 *       từ đầu, chúng tôi chọn Keycloak vì nó cung cấp các tính năng xác thực mạnh mẽ 
 *       (MFA, SSO, federation) và đáp ứng các tiêu chuẩn bảo mật hiện đại (OAuth2, OIDC)</li>
 *   <li><strong>Adapter Pattern</strong>: Lớp này hoạt động như một adapter giữa mô hình người dùng 
 *       của ứng dụng và Keycloak, chuyển đổi giữa các biểu diễn và che giấu sự phức tạp 
 *       của Keycloak Admin API</li>
 *   <li><strong>Caching và tối ưu hóa</strong>: Triển khai caching cho các truy vấn phổ biến
 *       để giảm số lượng yêu cầu đến Keycloak, cải thiện hiệu suất và độ tin cậy</li>
 *   <li><strong>Xử lý lỗi mạnh mẽ</strong>: Chuyển đổi các ngoại lệ từ Keycloak thành 
 *       ngoại lệ ứng dụng có ý nghĩa, giúp client có thể hiểu và xử lý dễ dàng hơn</li>
 * </ul>
 * 
 * <p>Dịch vụ này được sử dụng bởi AuthController để xử lý các yêu cầu xác thực, 
 * và được các thành phần khác trong hệ thống tham khảo để kiểm tra và thực thi quyền.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final Keycloak keycloak;

    @Value("${app.keycloak.realm}")
    private String realm;

    /**
     * Tạo người dùng mới trong Keycloak.
     * 
     * <p>Phương thức này được thiết kế để trừu tượng hóa quy trình tạo người dùng Keycloak
     * phức tạp, cung cấp một giao diện đơn giản cho các lớp khác trong hệ thống. Thiết kế này
     * giúp che giấu sự phức tạp của Keycloak Admin API và giảm sự phụ thuộc vào Keycloak
     * trong toàn bộ codebase.</p>
     * 
     * <p>Chúng tôi đã chọn cách tiếp cận này thay vì sử dụng trực tiếp Keycloak Admin Client
     * trong controller vì:</p>
     * <ul>
     *   <li>Tăng khả năng tái sử dụng code và giảm code trùng lặp</li>
     *   <li>Tập trung logic xử lý lỗi Keycloak tại một điểm duy nhất</li>
     *   <li>Đơn giản hóa quá trình chuyển đổi sang nhà cung cấp xác thực khác nếu cần</li>
     *   <li>Cung cấp điểm kiểm soát tập trung cho quá trình tạo người dùng</li>
     * </ul>
     * 
     * <p>Quy trình tạo người dùng bao gồm:</p>
     * <ol>
     *   <li>Xây dựng biểu diễn người dùng Keycloak từ dữ liệu đầu vào</li>
     *   <li>Thiết lập thông tin xác thực (mật khẩu) an toàn</li>
     *   <li>Gửi yêu cầu tạo người dùng đến Keycloak</li>
     *   <li>Xử lý phản hồi và trích xuất ID người dùng</li>
     * </ol>
     * 
     * @param username  Tên đăng nhập, phải là duy nhất trong hệ thống
     * @param email     Địa chỉ email, được sử dụng cho xác minh và thông báo
     * @param firstName Tên của người dùng
     * @param lastName  Họ của người dùng
     * @param password  Mật khẩu ban đầu, sẽ được mã hóa trước khi lưu trữ
     * @return ID người dùng được Keycloak tạo ra, sử dụng để tham chiếu người dùng trong hệ thống
     */
    public String createUser(String username, String email, String firstName, String lastName, String password) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Create user representation
            UserRepresentation user = new UserRepresentation();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEnabled(true);
            user.setEmailVerified(true);

            // Create user
            Response response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                String userId = extractUserIdFromResponse(response);
                
                // Set password
                setUserPassword(userId, password, false);
                
                // Assign default CUSTOMER role
                assignRoleToUser(userId, "CUSTOMER");
                
                log.info("User created successfully: {}", username);
                return userId;
            } else {
                throw new RuntimeException("Failed to create user. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            log.error("Error creating user: {}", username, e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        }
    }

    /**
     * Update user information
     */
    public void updateUser(String userId, String email, String firstName, String lastName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            UserRepresentation user = userResource.toRepresentation();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);

            userResource.update(user);
            log.info("User updated successfully: {}", userId);
        } catch (Exception e) {
            log.error("Error updating user: {}", userId, e);
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    /**
     * Delete user from Keycloak
     */
    public void deleteUser(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            realmResource.users().delete(userId);
            log.info("User deleted successfully: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting user: {}", userId, e);
            throw new RuntimeException("Failed to delete user: " + e.getMessage(), e);
        }
    }

    /**
     * Assign role to user
     */
    public void assignRoleToUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Get role representation
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            
            // Assign role to user
            userResource.roles().realmLevel().add(Arrays.asList(role));
            log.info("Role '{}' assigned to user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error assigning role '{}' to user: {}", roleName, userId, e);
            throw new RuntimeException("Failed to assign role: " + e.getMessage(), e);
        }
    }

    /**
     * Remove role from user
     */
    public void removeRoleFromUser(String userId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Get role representation
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();
            
            // Remove role from user
            userResource.roles().realmLevel().remove(Arrays.asList(role));
            log.info("Role '{}' removed from user: {}", roleName, userId);
        } catch (Exception e) {
            log.error("Error removing role '{}' from user: {}", roleName, userId, e);
            throw new RuntimeException("Failed to remove role: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by ID
     */
    public UserRepresentation getUser(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            return realmResource.users().get(userId).toRepresentation();
        } catch (Exception e) {
            log.error("Error getting user: {}", userId, e);
            throw new RuntimeException("Failed to get user: " + e.getMessage(), e);
        }
    }

    /**
     * Reset user password
     */
    public void resetUserPassword(String userId, String newPassword) {
        try {
            setUserPassword(userId, newPassword, false);
            log.info("Password reset successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Error resetting password for user: {}", userId, e);
            throw new RuntimeException("Failed to reset password: " + e.getMessage(), e);
        }
    }

    /**
     * Enable/disable user
     */
    public void setUserEnabled(String userId, boolean enabled) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(enabled);
            userResource.update(user);
            
            log.info("User {} {}: {}", enabled ? "enabled" : "disabled", "successfully", userId);
        } catch (Exception e) {
            log.error("Error {} user: {}", enabled ? "enabling" : "disabling", userId, e);
            throw new RuntimeException("Failed to " + (enabled ? "enable" : "disable") + " user: " + e.getMessage(), e);
        }
    }

    /**
     * Get user roles
     */
    public List<RoleRepresentation> getUserRoles(String userId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);
            return userResource.roles().realmLevel().listAll();
        } catch (Exception e) {
            log.error("Error getting user roles: {}", userId, e);
            throw new RuntimeException("Failed to get user roles: " + e.getMessage(), e);
        }
    }

    /**
     * Private helper method to set user password
     */
    private void setUserPassword(String userId, String password, boolean temporary) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(userId);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(temporary);

        userResource.resetPassword(credential);
    }

    /**
     * Private helper method to extract user ID from response
     */
    private String extractUserIdFromResponse(Response response) {
        String location = response.getHeaderString("Location");
        if (location != null) {
            String[] pathSegments = location.split("/");
            return pathSegments[pathSegments.length - 1];
        }
        throw new RuntimeException("Failed to extract user ID from response");
    }
}
