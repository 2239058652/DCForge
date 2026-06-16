package com.forge.dc.modules.ai.service;

import com.forge.dc.modules.ai.config.RabbitMQConfig;
import com.forge.dc.modules.ai.dto.TaskMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskProducer {

    private final RabbitTemplate rabbitTemplate;

    public TaskProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendTask(TaskMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                message
        );
        log.info("任务已发送到队列: taskId={}", message.getTaskId());
    }
}
