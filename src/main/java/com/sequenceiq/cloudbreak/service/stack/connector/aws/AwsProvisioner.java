package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.connector.Provisioner;

@Component
public class AwsProvisioner implements Provisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsProvisioner.class);

    @Autowired
    private AwsStackUtil awsStackUtil;

    @Autowired
    private CloudFormationTemplate cfTemplate;

    @Autowired
    private RetryingStackUpdater stackUpdater;

    @Override
    public synchronized void buildStack(Stack stack, String userData, Map<String, Object> setupProperties) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient client = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        String stackName = String.format("%s-%s", stack.getName(), stack.getId());
        CreateStackRequest createStackRequest = createStackRequest()
                .withStackName(stackName)
                .withTemplateBody(cfTemplate.getBody())
                .withNotificationARNs((String) setupProperties.get(SnsTopicManager.NOTIFICATION_TOPIC_ARN_KEY))
                .withParameters(
                        new Parameter().withParameterKey("SSHLocation").withParameterValue(awsTemplate.getSshLocation()),
                        new Parameter().withParameterKey("CBUserData").withParameterValue(userData),
                        new Parameter().withParameterKey("StackName").withParameterValue(stackName),
                        new Parameter().withParameterKey("InstanceCount").withParameterValue(stack.getNodeCount().toString()),
                        new Parameter().withParameterKey("InstanceType").withParameterValue(awsTemplate.getInstanceType().toString()),
                        new Parameter().withParameterKey("KeyName").withParameterValue(awsTemplate.getKeyName()),
                        new Parameter().withParameterKey("AMI").withParameterValue(awsTemplate.getAmiId())
                );
        CreateStackResult createStackResult = client.createStack(createStackRequest);
        Stack updatedStack = stackUpdater.updateStackCfAttributes(stack.getId(), stackName, createStackResult.getStackId());
        LOGGER.info("CloudFormation stack creation request sent with stack name: '{}' for stack: '{}'", stackName, updatedStack.getId());
    }

    protected CreateStackRequest createStackRequest() {
        return new CreateStackRequest();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}