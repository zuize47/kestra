package io.kestra.runner.h2;

import io.kestra.core.exceptions.DeserializationException;
import io.kestra.core.queues.WorkerTriggerResultQueueInterface;
import io.kestra.core.runners.WorkerTriggerResult;
import io.kestra.core.utils.Either;
import io.kestra.jdbc.JdbcWorkerTriggerResultQueueService;
import io.micronaut.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@Slf4j
public class H2WorkerTriggerResultQueue extends H2Queue<WorkerTriggerResult> implements WorkerTriggerResultQueueInterface {
    private final JdbcWorkerTriggerResultQueueService jdbcWorkerTriggerResultQueueService;

    public H2WorkerTriggerResultQueue(ApplicationContext applicationContext) {
        super(WorkerTriggerResult.class, applicationContext);
        this.jdbcWorkerTriggerResultQueueService = applicationContext.getBean(JdbcWorkerTriggerResultQueueService.class);
    }

    @Override
    public Runnable receive(String consumerGroup, Class<?> queueType, Consumer<Either<WorkerTriggerResult, DeserializationException>> consumer) {
        return jdbcWorkerTriggerResultQueueService.receive(consumerGroup, queueType, consumer);
    }

    @Override
    public void pause() {
        jdbcWorkerTriggerResultQueueService.pause();
    }

    @Override
    public void cleanup() {
        jdbcWorkerTriggerResultQueueService.cleanup();
    }

    @Override
    public void close() {
        jdbcWorkerTriggerResultQueueService.close();
    }
}
