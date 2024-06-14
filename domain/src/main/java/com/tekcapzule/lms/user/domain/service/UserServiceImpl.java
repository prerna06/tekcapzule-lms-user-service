package com.tekcapzule.lms.user.domain.service;

import com.tekcapzule.lms.user.domain.command.*;
import com.tekcapzule.lms.user.domain.model.*;
import com.tekcapzule.lms.user.domain.repository.UserDynamoRepository;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private static final String HASH = "#";
    private UserDynamoRepository userDynamoRepository;

    @Autowired
    public UserServiceImpl(UserDynamoRepository userDynamoRepository) {
        this.userDynamoRepository = userDynamoRepository;
    }

    @Override
    public void create(CreateCommand createCommand) {

        log.info(String.format("Entering create user service - Phone No.:%s", createCommand.getPhoneNumber()));
        log.info(String.format("Entering create user service - User Id:%s", createCommand.getEmailId()));


        LmsUser lmsUser = LmsUser.builder()
                .userId(createCommand.getEmailId())
                .emailId(createCommand.getEmailId())
                .firstName(createCommand.getFirstName())
                .lastName(createCommand.getLastName())
                .phoneNumber(createCommand.getPhoneNumber())
                .activeSince(DateTime.now(DateTimeZone.UTC).toString())
                .address(Address.builder()
                        .addressLine1(createCommand.getAddress().getAddressLine1())
                        .addressLine2(createCommand.getAddress().getAddressLine1())
                        .city(createCommand.getAddress().getCity())
                        .state(createCommand.getAddress().getState())
                        .country(createCommand.getAddress().getCountry())
                        .zipCode(createCommand.getAddress().getZipCode()).build())
                .status(Status.ACTIVE)
                .build();

        lmsUser.setAddedOn(createCommand.getExecOn());
        lmsUser.setUpdatedOn(createCommand.getExecOn());
        lmsUser.setAddedBy(createCommand.getExecBy().getUserId());

        userDynamoRepository.save(lmsUser);
    }

    @Override
    public void update(UpdateCommand updateCommand) {

        log.info(String.format("Entering update user service - User Id:%s", updateCommand.getUserId()));

        LmsUser lmsUser = userDynamoRepository.findBy(updateCommand.getUserId());
        if (lmsUser != null) {
            lmsUser.setEmailId(updateCommand.getEmailId());
            lmsUser.setFirstName(updateCommand.getFirstName());
            lmsUser.setLastName(updateCommand.getLastName());
            lmsUser.setPhoneNumber(updateCommand.getPhoneNumber());
            lmsUser.setAddress(Address.builder()
                    .addressLine1(updateCommand.getAddress().getAddressLine1())
                    .addressLine2(updateCommand.getAddress().getAddressLine2())
                    .city(updateCommand.getAddress().getCity())
                    .state(updateCommand.getAddress().getState())
                    .country(updateCommand.getAddress().getCountry())
                    .zipCode(updateCommand.getAddress().getZipCode())
                    .build()
            );
            lmsUser.setSubscribedTopics(updateCommand.getSubscribedTopics());
            lmsUser.setEnrollments(updateCommand.getEnrollments());
            lmsUser.setUpdatedOn(updateCommand.getExecOn());
            lmsUser.setUpdatedBy(updateCommand.getExecBy().getUserId());
            userDynamoRepository.save(lmsUser);
        }
    }

    @Override
    public void disable(DisableCommand disableCommand) {

        log.info(String.format("Entering disable user service - User Id:%s", disableCommand.getUserId()));

        LmsUser lmsUser = userDynamoRepository.findBy(disableCommand.getUserId());
        if (lmsUser != null) {

            lmsUser.setStatus(Status.INACTIVE);

            lmsUser.setUpdatedOn(disableCommand.getExecOn());
            lmsUser.setUpdatedBy(disableCommand.getExecBy().getUserId());

            userDynamoRepository.save(lmsUser);
        }
    }

    @Override
    public void optInCourse(OptInCourseCommand optInCourseCommand) {

        log.info(String.format("Entering OptIn course service - User Id:%s, course Id:%s", optInCourseCommand.getUserId(),
                optInCourseCommand.getCourseId()));

        LmsUser lmsUser = userDynamoRepository.findBy(optInCourseCommand.getUserId());
        if (lmsUser != null) {
            List<Enrollment> enrollments = new ArrayList<>();
            if ( lmsUser.getEnrollments() != null) {
                enrollments.addAll(lmsUser.getEnrollments());
            }
            enrollments.add(Enrollment.builder()
                    .courseId(optInCourseCommand.getCourseId()).enrollmentStatus(EnrollmentStatus.OPTEDIN).build());

            lmsUser.setEnrollments(enrollments);

            lmsUser.setUpdatedOn(optInCourseCommand.getExecOn());
            lmsUser.setUpdatedBy(optInCourseCommand.getExecBy().getUserId());

            userDynamoRepository.save(lmsUser);
        }
    }

    @Override
    public void optOutCourse(OptOutCourseCommand optOutCourseCommand) {

        log.info(String.format("Entering optOut course service - User Id:%s, course Id:%s", optOutCourseCommand.getUserId(),
                optOutCourseCommand.getEnrollment().getCourseId()));

        LmsUser lmsUser = userDynamoRepository.findBy(optOutCourseCommand.getUserId());
        if (lmsUser != null) {
            List<Enrollment> enrollments = lmsUser.getEnrollments();
            enrollments.removeIf(course -> course.getCourseId().equals(optOutCourseCommand.getEnrollment().getCourseId()));
            lmsUser.setUpdatedOn(optOutCourseCommand.getExecOn());
            lmsUser.setUpdatedBy(optOutCourseCommand.getExecBy().getUserId());

            userDynamoRepository.save(lmsUser);
        }
    }

    @Override
    public void subscribeTopic(SubscribeTopicCommand subscribeTopicCommand) {
        log.info(String.format("Entering follow topic service - User Id:%s, Topic Code:%s", subscribeTopicCommand.getUserId(), subscribeTopicCommand.getTopicCodes()));

        LmsUser lmsUser = userDynamoRepository.findBy(subscribeTopicCommand.getUserId());
        if (lmsUser != null) {

            List<String> followedTopics = new ArrayList<>();
            followedTopics.addAll(subscribeTopicCommand.getTopicCodes());
            lmsUser.setSubscribedTopics(followedTopics);

            lmsUser.setUpdatedOn(subscribeTopicCommand.getExecOn());
            lmsUser.setUpdatedBy(subscribeTopicCommand.getExecBy().getUserId());

            userDynamoRepository.save(lmsUser);
        }
    }

    @Override
    public void unsubscribeTopic(UnSubscribeTopicCommand unSubscribeTopicCommand) {
        log.info(String.format("Entering unfollow topic service - User Id:%s, Topic Code:%s", unSubscribeTopicCommand.getUserId(), unSubscribeTopicCommand.getTopicCodes()));

        LmsUser lmsUser = userDynamoRepository.findBy(unSubscribeTopicCommand.getUserId());
        if (lmsUser != null) {

            List<String> followedTopics = new ArrayList<>();
            if (lmsUser.getSubscribedTopics() != null) {
                followedTopics = lmsUser.getSubscribedTopics();
            }

            followedTopics.removeAll(unSubscribeTopicCommand.getTopicCodes());
            lmsUser.setSubscribedTopics(followedTopics);

            lmsUser.setUpdatedOn(unSubscribeTopicCommand.getExecOn());
            lmsUser.setUpdatedBy(unSubscribeTopicCommand.getExecBy().getUserId());

            userDynamoRepository.save(lmsUser);
        }
    }

    @Override
    public LmsUser get(String userId, String tenantId) {

        log.info(String.format("Entering get user service - User Id:%s", userId));
        //return userDynamoRepository.findBy(tenantId+ HASH +userId);
        return userDynamoRepository.findBy(userId);
    }

    @Override
    public List<Enrollment> getCourseByStatus(String userId, String tenantId, String status) {
        log.info(String.format("Entering get course by status service - User Id:%s Course Status:%s", userId, status));

        //LmsUser user = userDynamoRepository.findBy(tenantId+ HASH +userId);
        LmsUser user = userDynamoRepository.findBy(userId);
        if(user!=null){
            return user.getEnrollments().stream().filter(course -> status.equals(course.getEnrollmentStatus())).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public Map<EnrollmentStatus, List<Enrollment>> getCoursesGroupByStatus(String userId, String tenantId) {
        log.info(String.format("Entering get course by status service - User Id:%s", userId));

        //LmsUser user = userDynamoRepository.findBy(tenantId+ HASH +userId);
        LmsUser user = userDynamoRepository.findBy(userId);
        if(user!=null){
            return user.getEnrollments().stream().collect(Collectors.groupingBy(Enrollment::getEnrollmentStatus));
        }
        return null;
    }

    @Override
    public int getAllUsersCount() {
        log.info("Entering getall users count service");
        return userDynamoRepository.getAllUsersCount();
    }

    @Override
    public void updateUserProgress(UpdateUserProgressCommand updateUserProgressCommand) {
        log.info("Entering updateUserProgress for UserId %s ", updateUserProgressCommand.getUserId());
        LmsUser lmsUser = userDynamoRepository.findBy(updateUserProgressCommand.getUserId());
        Map<String, Progress> userProgressDetails = lmsUser.getProgressDetails();
        String key = String.format("%s#%s#%s", updateUserProgressCommand.getCourseId(),
                updateUserProgressCommand.getModuleId(), updateUserProgressCommand.getChapterId());
        userProgressDetails.put(key, Progress.builder()
                .progressPercentage(updateUserProgressCommand.getProgressPercentage())
                .watchedDuration(updateUserProgressCommand.getWatchedDuration())
                .lastAccessed(updateUserProgressCommand.getLastAccessed()).build());
        userDynamoRepository.save(lmsUser);
    }
}
