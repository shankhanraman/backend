package com.arogya.cafe.security.service;

import com.arogya.cafe.security.entity.Staff;
import com.arogya.cafe.security.repository.StaffRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads a {@link Staff} as a Spring Security principal, mapping its role to a ROLE_* authority. */
@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffRepository staff;

    public StaffUserDetailsService(StaffRepository staff) {
        this.staff = staff;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Staff s = staff.findByUsername(username);
        if (s == null) {
            throw new UsernameNotFoundException("No staff with username '" + username + "'");
        }
        return User.withUsername(s.getUsername())
                .password(s.getPasswordHash())
                .authorities(
                        List.of(new SimpleGrantedAuthority("ROLE_" + s.getRole().name())))
                .build();
    }
}
