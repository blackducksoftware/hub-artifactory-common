package com.blackducksoftware.integration.hub.artifactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.api.notification.NotificationService;
import com.blackducksoftware.integration.hub.model.enumeration.NotificationEnum;
import com.blackducksoftware.integration.hub.model.view.NotificationView;

public class NotificationsManager {
    public List<NotificationView> getVulnerabilityNotifications(final NotificationService notificationService, final Date startDate, final Date endDate) throws IntegrationException {
        final long startTime = System.currentTimeMillis();
        System.out.println(String.format("getVulnerabilityNotifications() start time: %d", startTime));

        final List<NotificationView> notificationViews = notificationService.getAllNotifications(startDate, endDate);
        final List<NotificationView> vulnerabilityNotificationViews = notificationViews.stream().filter(view -> NotificationEnum.VULNERABILITY == view.type).collect(Collectors.toList());
        final long endTime = System.currentTimeMillis();
        System.out.println(String.format("getVulnerabilityNotifications() end time: %d", endTime));
        System.out.println(String.format("getVulnerabilityNotifications() duration time: %d", endTime - startTime));

        return vulnerabilityNotificationViews;
    }
}
