package com.hungng3011.vdtecomberefresh.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Autowired
    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserEntity createUser(UserDto userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("User existed");
        }
        else {
            UserEntity user = userMapper.toEntity(userDto);
            userRepository.save(user);
            return user;
        }
    }

    public UserEntity getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public UserDto getUserById(Long id) {
        UserEntity userEntity = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toDto(userEntity);
    }

    public List<UserDto> getAllUsers() {
        List<UserEntity> allUsers = userRepository.findAll();
        List<UserDto> userDtos = new ArrayList<>();
        for (UserEntity user : allUsers) {
            userDtos.addLast(userMapper.toDto(user));
        }
        return userDtos;
    }

    public UserEntity updateUser(UserDto userDto) {
        if (userRepository.existsById(userDto.getId())) {
            UserEntity user = userMapper.toEntity(userDto);
            userRepository.save(user);
            return user;
        }
        else {
            throw new RuntimeException("User does not exist");
        }
    }

    public void deleteUserById(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
        }
        else {
            throw new RuntimeException("User does not exist");
        }
    }
}
