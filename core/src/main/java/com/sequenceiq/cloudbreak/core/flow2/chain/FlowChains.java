package com.sequenceiq.cloudbreak.core.flow2.chain;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class FlowChains {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowChains.class);

    @Inject
    private EventBus eventBus;
    private Map<String, Queue<Selectable>> flowChainMap = new ConcurrentHashMap<>();

    public void putFlowChain(String flowChainId, Queue<Selectable> flowChain) {
        flowChainMap.put(flowChainId, flowChain);
    }

    public void removeFlowChain(String flowChainId) {
        if (flowChainId != null) {
            flowChainMap.remove(flowChainId);
        }
    }

    public void triggerNextFlow(String flowChainId) {
        Queue<Selectable> queue = flowChainMap.get(flowChainId);
        if (queue != null) {
            Selectable selectable = queue.poll();
            if (selectable != null) {
                sendEvent(flowChainId, selectable);
            } else {
                removeFlowChain(flowChainId);
            }
        }
    }

    protected void sendEvent(String flowChainId, Selectable selectable) {
        LOGGER.info("Triggering event: {}", selectable);
        Map<String, Object> headers = new HashMap<>();
        headers.put("FLOW_CHAIN_ID", flowChainId);
        eventBus.notify(selectable.selector(), new Event<>(new Event.Headers(headers), selectable));
    }
}
