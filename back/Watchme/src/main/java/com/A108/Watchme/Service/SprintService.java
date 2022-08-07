package com.A108.Watchme.Service;

import com.A108.Watchme.Http.ApiResponse;
import com.A108.Watchme.Repository.SprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class SprintService {
    @Autowired
    private SprintRepository sprintRepository;

    public ApiResponse deleteSprint(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long id = Long.parseLong(((UserDetails)authentication.getPrincipal()).getUsername());
    }
}
