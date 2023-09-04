package com.locibot.organizer2.tasks;

import com.locibot.organizer2.database.repositories.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class UserDataDeletionTask {

    private final UserRepository userRepository;

    public UserDataDeletionTask(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 0 1 * * MON")
    public void deleteUserData() {
        userRepository.deleteAllOldUserData().block();
    }

}
