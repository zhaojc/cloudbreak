package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.HDPInfo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.CloudbreakSecuritySetupException;
import com.sequenceiq.cloudbreak.domain.Component;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;

@Service
@Transactional
public class ImageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImageService.class);

    @Inject
    private ImageNameUtil imageNameUtil;

    @Inject
    private HdpInfoSearchService hdpInfoSearchService;

    @Inject
    private UserDataBuilder userDataBuilder;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public Image getImage(Long stackId) throws CloudbreakImageNotFoundException {
        return componentConfigProvider.getImage(stackId);
    }

    @Transactional(Transactional.TxType.NEVER)
    public void create(Stack stack, PlatformParameters params, String ambariVersion, String hdpVersion) {
        try {
            Platform platform = platform(stack.cloudPlatform());
            String platformString = platform(stack.cloudPlatform()).value().toLowerCase();
            String imageName = imageNameUtil.determineImageName(platformString, stack.getRegion(), ambariVersion, hdpVersion);
            String tmpSshKey = tlsSecurityService.readPublicSshKey(stack.getId());
            String sshUser = stack.getCredential().getLoginUserName();
            String publicSssKey = stack.getCredential().getPublicKey();
            Map<InstanceGroupType, String> userData = userDataBuilder.buildUserData(platform, publicSssKey, tmpSshKey, sshUser, params,
                    stack.getRelocateDocker() == null ? false : stack.getRelocateDocker());
            HDPInfo hdpInfo = hdpInfoSearchService.searchHDPInfo(ambariVersion, hdpVersion);
            if (hdpInfo != null) {
                String specificImage = imageNameUtil.determineImageName(hdpInfo, platformString, stack.getRegion());
                if (specificImage == null) {
                    LOGGER.warn("Cannot find image in the catalog, fallback to default image, ambari: {}, hdp: {}", ambariVersion, hdpVersion);
                    hdpInfo = null;
                } else {
                    LOGGER.info("Determined image from catalog: {}", specificImage);
                    imageName = specificImage;
                }
            }
            List<Component> components = new ArrayList<>();
            Image image;
            if (hdpInfo == null) {
                image = new Image(imageName, userData, null, null);
            } else {
                AmbariRepo ambariRepo = new AmbariRepo();
                ambariRepo.setPredefined(Boolean.TRUE);
                Component ambariRepoComponent = new Component(ComponentType.AMBARI_REPO_DETAILS, ComponentType.AMBARI_REPO_DETAILS.name(),
                        new Json(ambariRepo), stack);
                components.add(ambariRepoComponent);
                image = new Image(imageName, userData, hdpInfo.getRepo(), hdpInfo.getVersion());
            }
            Component component = new Component(ComponentType.IMAGE, ComponentType.IMAGE.name(), new Json(image), stack);
            components.add(component);
            componentConfigProvider.store(components);
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Failed to create json", e);
        } catch (CloudbreakSecuritySetupException e) {
            throw new CloudbreakServiceException("Failed to read temporary ssh credentials", e);
        }
    }


}
