package com.arogya.cafe.security.provider;
import com.arogya.cafe.security.repository.StaffRepository;
import com.arogya.cafe.security.entity.Staff;
import com.arogya.cafe.security.entity.*;

import com.arogya.cafe.common.exception.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the currently authenticated {@link Staff} from the security context. */
@Component
public class CurrentStaffProvider {

    private final StaffRepository staff;

    public CurrentStaffProvider(StaffRepository staff) {
        this.staff = staff;
    }

    public Staff require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new NotFoundException("No authenticated user");
        }
        Staff found = staff.findByUsername(auth.getName());
        if (found == null) {
            throw new NotFoundException("Authenticated user '" + auth.getName() + "' not found");
        }
        return found;
    }
}
