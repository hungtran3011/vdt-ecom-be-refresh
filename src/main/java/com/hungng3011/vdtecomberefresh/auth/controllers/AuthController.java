package com.hungng3011.vdtecomberefresh.auth.controllers;

import com.hungng3011.vdtecomberefresh.auth.AuthService;
import com.hungng3011.vdtecomberefresh.auth.dtos.AuthResponseDto;
import com.hungng3011.vdtecomberefresh.auth.dtos.UserRegistrationDto;
import com.hungng3011.vdtecomberefresh.profile.services.ProfileService;
import com.hungng3011.vdtecomberefresh.profile.dtos.ProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.UUID;

/**
 * Controller cho xác thực và quản lý người dùng.
 * 
 * <p>Lớp này được thiết kế để cung cấp một giao diện API thống nhất cho mọi tương tác 
 * liên quan đến xác thực và quản lý người dùng. Việc tập trung các endpoint này vào một 
 * controller riêng biệt (thay vì phân tán qua nhiều module) đảm bảo tính nhất quán trong 
 * cách xử lý các vấn đề về bảo mật và giúp dễ dàng áp dụng các chính sách bảo mật toàn cục.</p>
 * 
 * <p>Các quyết định thiết kế chính bao gồm:</p>
 * <ul>
 *   <li><strong>Tách biệt AuthController và ProfileController</strong>: Phân tách rõ ràng giữa 
 *       việc xác thực (ai được phép truy cập) và thông tin hồ sơ người dùng (thông tin cá nhân),
 *       giúp áp dụng các chiến lược bảo mật khác nhau cho từng loại dữ liệu</li>
 *   <li><strong>RESTful API</strong>: Tuân thủ nguyên tắc RESTful với các endpoint cụ thể 
 *       cho từng chức năng (đăng ký, đăng nhập, đổi mật khẩu...), giúp giao diện API rõ ràng 
 *       và dễ sử dụng</li>
 *   <li><strong>Ủy thác logic xử lý cho service</strong>: Controller chỉ xử lý yêu cầu HTTP và 
 *       định dạng phản hồi, trong khi logic nghiệp vụ được ủy thác cho AuthService, giúp tăng 
 *       khả năng tái sử dụng và testability</li>
 * </ul>
 * 
 * <p>Lớp này tương tác chặt chẽ với AuthService để thực hiện các thao tác xác thực và 
 * ProfileService để tạo và quản lý thông tin hồ sơ người dùng sau khi xác thực.</p>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final ProfileService profileService;

    /**
     * Đăng ký người dùng mới (chỉ Admin).
     * 
     * <p>Endpoint này được thiết kế để chỉ cho phép quản trị viên tạo tài khoản mới, 
     * thay vì cho phép đăng ký tự do. Cách tiếp cận này được chọn vì:</p>
     * <ul>
     *   <li>Đảm bảo kiểm soát chặt chẽ quy trình onboarding người dùng trong môi trường doanh nghiệp</li>
     *   <li>Ngăn chặn tình trạng đăng ký tài khoản giả mạo hoặc spam</li>
     *   <li>Cho phép xác minh và phê duyệt người dùng trước khi cấp quyền truy cập</li>
     * </ul>
     * 
     * <p>Quy trình xử lý bao gồm:</p>
     * <ol>
     *   <li>Tạo người dùng mới trong Keycloak thông qua AuthService</li>
     *   <li>Tạo hồ sơ người dùng tương ứng trong hệ thống cơ sở dữ liệu local</li>
     *   <li>Gán các quyền mặc định cho người dùng mới</li>
     * </ol>
     * 
     * <p>Chúng tôi sử dụng {@link @PreAuthorize} để thực thi kiểm tra quyền trước khi
     * phương thức được thực thi, cung cấp lớp bảo mật sớm và rõ ràng.</p>
     * 
     * @param registrationDto Thông tin đăng ký người dùng mới, được xác thực bởi Bean Validation
     * @return AuthResponseDto chứa thông tin về người dùng đã được tạo thành công
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponseDto> registerUser(@Valid @RequestBody UserRegistrationDto registrationDto) {
        try {
            String userId = authService.createUser(
                    registrationDto.getUsername(),
                    registrationDto.getEmail(),
                    registrationDto.getFirstName(),
                    registrationDto.getLastName(),
                    registrationDto.getPassword()
            );

            // Create local profile for the user
            ProfileDto profileDto = new ProfileDto();
            profileDto.setUserId(UUID.fromString(userId));
            profileDto.setFullName(registrationDto.getFirstName() + " " + registrationDto.getLastName());
            profileDto.setEmail(registrationDto.getEmail());
            profileService.createOrUpdate(profileDto);

            AuthResponseDto response = AuthResponseDto.builder()
                    .userId(userId)
                    .username(registrationDto.getUsername())
                    .email(registrationDto.getEmail())
                    .message("User registered successfully")
                    .success(true)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error registering user: {}", registrationDto.getUsername(), e);
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .message("Failed to register user: " + e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Assign role to user (Admin only)
     */
    @PostMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponseDto> assignRole(@PathVariable String userId, @PathVariable String roleName) {
        try {
            authService.assignRoleToUser(userId, roleName.toUpperCase());
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .userId(userId)
                    .message("Role assigned successfully")
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", roleName, userId, e);
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .message("Failed to assign role: " + e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Remove role from user (Admin only)
     */
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponseDto> removeRole(@PathVariable String userId, @PathVariable String roleName) {
        try {
            authService.removeRoleFromUser(userId, roleName.toUpperCase());
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .userId(userId)
                    .message("Role removed successfully")
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error removing role {} from user {}", roleName, userId, e);
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .message("Failed to remove role: " + e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Enable/disable user (Admin only)
     */
    @PutMapping("/users/{userId}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponseDto> setUserEnabled(@PathVariable String userId, @RequestParam boolean enabled) {
        try {
            authService.setUserEnabled(userId, enabled);
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .userId(userId)
                    .message("User " + (enabled ? "enabled" : "disabled") + " successfully")
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error {} user {}", enabled ? "enabling" : "disabling", userId, e);
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .message("Failed to " + (enabled ? "enable" : "disable") + " user: " + e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponseDto> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = jwt.getSubject();
            String username = jwt.getClaimAsString("preferred_username");
            String email = jwt.getClaimAsString("email");
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .userId(userId)
                    .username(username)
                    .email(email)
                    .message("User info retrieved successfully")
                    .success(true)
                    .build();
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting current user info", e);
            
            AuthResponseDto response = AuthResponseDto.builder()
                    .message("Failed to get user info: " + e.getMessage())
                    .success(false)
                    .build();
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
