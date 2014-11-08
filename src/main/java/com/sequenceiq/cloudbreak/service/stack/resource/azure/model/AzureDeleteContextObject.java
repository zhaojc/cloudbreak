package com.sequenceiq.cloudbreak.service.stack.resource.azure.model;

import java.io.File;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.credential.azure.AzureCertificateService;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;

public class AzureDeleteContextObject extends DeleteContextObject {

    private AzureClient azureClient;
    private String commonName;
    private String emailAsFolder;

    public AzureDeleteContextObject(Long stackId, String commonName, AzureClient azureClient, String emailAsFolder) {
        super(stackId);
        this.azureClient = azureClient;
        this.commonName = commonName;
        this.emailAsFolder = emailAsFolder;
    }

    public AzureClient getAzureClient() {
        return azureClient;
    }

    public void setAzureClient(AzureClient azureClient) {
        this.azureClient = azureClient;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getEmailAsFolder() {
        return emailAsFolder;
    }

    public synchronized AzureClient getNewAzureClient(AzureCredential credential) {
        File file = new File(AzureCertificateService.getUserJksFileName(credential, emailAsFolder));
        return new AzureClient(credential.getSubscriptionId(), file.getAbsolutePath(), credential.getJks());
    }
}