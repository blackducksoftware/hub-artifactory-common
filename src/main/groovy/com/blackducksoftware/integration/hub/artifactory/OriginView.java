package com.blackducksoftware.integration.hub.artifactory;

import java.util.Date;

import com.blackducksoftware.integration.hub.model.HubView;
import com.blackducksoftware.integration.hub.model.enumeration.ComponentVersionSourceEnum;
import com.blackducksoftware.integration.hub.model.view.ComplexLicenseView;

public class OriginView extends HubView {
    public ComplexLicenseView license;
    public String originId;
    public String originName;
    public Date releasedOn;
    public ComponentVersionSourceEnum source;
    public String versionName;

}
