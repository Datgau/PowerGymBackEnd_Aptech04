package com.example.project_backend04.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EntityChangedEvent extends ApplicationEvent {
    
    private final String entityName;
    private final String action;
    private final Object data;
    private final Long entityId;

    public EntityChangedEvent(Object source, String entityName, String action, Object data, Long entityId) {
        super(source);
        this.entityName = entityName;
        this.action = action;
        this.data = data;
        this.entityId = entityId;
    }
    
    public EntityChangedEvent(Object source, String entityName, String action, Object data) {
        this(source, entityName, action, data, null);
    }
}
