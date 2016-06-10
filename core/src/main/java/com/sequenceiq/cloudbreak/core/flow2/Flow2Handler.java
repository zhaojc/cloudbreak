package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChains;
import com.sequenceiq.cloudbreak.core.flow2.config.FlowConfiguration;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationFlowConfig;
import com.sequenceiq.cloudbreak.service.flowlog.FlowLogService;

import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

@Component
public class Flow2Handler implements Consumer<Event<? extends Payload>> {
    public static final String FLOW_FINAL = "FLOWFINAL";

    private static final Logger LOGGER = LoggerFactory.getLogger(Flow2Handler.class);

    @Inject
    private FlowLogService flowLogService;

    @Resource
    private Map<String, FlowConfiguration<?>> flowConfigurationMap;

    @Inject
    private FlowChains flowChains;

    @Inject
    private FlowRegister runningFlows;

    @Inject
    private EventBus eventBus;

    @Override
    public void accept(Event<? extends Payload> event) {
        String key = (String) event.getKey();
        Payload payload = event.getData();
        String flowId = getFlowId(event);
        String flowChainId = getFlowChainId(event);

        if (FLOW_FINAL.equals(key)) {
            finalizeFlow(flowId, flowChainId, payload.getStackId());
        } else {
            if (flowId == null) {
                LOGGER.debug("flow trigger arrived: key: {}, payload: {}", key, payload);
                FlowConfiguration<?> flowConfig = flowConfigurationMap.get(key);
                if (flowConfig.getFlowTriggerCondition().isFlowTriggerable(payload.getStackId())) {
                    flowId = UUID.randomUUID().toString();
                    Flow flow = flowConfig.createFlow(flowId);
                    boolean accepted = runningFlows.put(flow, flowChainId, payload.getStackId(), flowConfig instanceof StackTerminationFlowConfig);
                    if (accepted) {
                        flow.initialize();
                        flowLogService.save(flowId, key, payload, flowConfig.getClass(), flow.getCurrentState());
                        flow.sendEvent(key, payload);
                    } else {
                        LOGGER.info("Other flow already running on stack: {}", payload.getStackId());
                    }
                }
            } else {
                LOGGER.debug("flow control event arrived: key: {}, flowid: {}, payload: {}", key, flowId, payload);
                Flow flow = runningFlows.get(flowId);
                if (flow != null) {
                    flowLogService.save(flowId, key, payload, flow.getFlowConfigClass(), flow.getCurrentState());
                    flow.sendEvent(key, payload);
                } else {
                    LOGGER.info("Cancelled flow finished running. Stack ID {}, flow ID {}, event {}", payload.getStackId(), flowId, key);
                }
            }
        }
    }

    private void finalizeFlow(String flowId, String flowChainId, Long stackId) {
        LOGGER.debug("flow finalizing arrived: id: {}", flowId);
        flowLogService.close(stackId, flowId);
        Flow flow = runningFlows.remove(flowId, stackId);
        if (flow.isFlowFailed()) {
            flowChains.removeFlowChain(flowChainId);
        } else if (flowChainId != null) {
            flowChains.triggerNextFlow(flowChainId);
        }
    }

    private String getFlowId(Event<?> event) {
        return event.getHeaders().get("FLOW_ID");
    }

    private String getFlowChainId(Event<?> event) {
        return event.getHeaders().get("FLOW_CHAIN_ID");
    }
}
