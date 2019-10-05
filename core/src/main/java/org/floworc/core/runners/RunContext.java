package org.floworc.core.runners;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.floworc.core.models.executions.Execution;
import org.floworc.core.models.executions.LogEntry;
import org.floworc.core.models.executions.MetricEntry;
import org.floworc.core.models.executions.TaskRun;
import org.floworc.core.models.tasks.Task;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@NoArgsConstructor
@Getter
public class RunContext {
    private static Handlebars handlebars = new Handlebars()
        .registerHelpers(ConditionalHelpers.class)
        .registerHelpers(EachHelper.class)
        .registerHelpers(LogHelper.class)
        .registerHelpers(StringHelpers.class)
        .registerHelpers(UnlessHelper.class)
        .registerHelpers(WithHelper.class)
        .registerHelperMissing((context, options) -> {
            throw new IllegalStateException("Missing variable: " + options.helperName);
        });

    private Map<String, Object> variables;

    private List<MetricEntry> metrics;

    private ContextAppender contextAppender;

    private Logger logger;

    public RunContext(Execution execution, TaskRun taskRun, Task task) {
        ImmutableMap.Builder<String, Object> variblesBuilder = ImmutableMap.<String, Object>builder()
            .put("execution", ImmutableMap.of(
                "id", execution.getId(),
                "startDate", execution.getState().startDate()
            ))
            .put("flow", ImmutableMap.of(
                "id", execution.getFlowId()
            ))
            .put("task", ImmutableMap.of(
                "id", task.getId(),
                "type", task.getType()
            ))
            .put("taskrun", ImmutableMap.of(
                "id", taskRun.getId(),
                "startDate", taskRun.getState().startDate()
            ))
            .put("env", System.getenv());

        if (execution.getTaskRunList() != null) {
            variblesBuilder
                .put("outputs", execution
                    .getTaskRunList()
                    .stream()
                    .filter(current -> current.getOutputs() != null)
                    .map(current -> new AbstractMap.SimpleEntry<>(current.getTaskId(), current.getOutputs()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                );
        }

        if (execution.getInputs() != null) {
            variblesBuilder.put("inputs", execution.getInputs());
        }

        this.variables = variblesBuilder.build();
    }

    @VisibleForTesting
    public RunContext(Map<String, Object> variables) {
        this.variables = variables;
    }

    public org.slf4j.Logger logger(Class cls) {
        if (this.logger == null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            this.logger = loggerContext.getLogger(cls);

            this.contextAppender = new ContextAppender();
            this.contextAppender.setContext(loggerContext);
            this.contextAppender.start();

            this.logger.addAppender(this.contextAppender);
            this.logger.setLevel(Level.TRACE);
            this.logger.setAdditive(true);
        }

        return this.logger;
    }

    public String render(String inline) throws IOException {
        Template template = handlebars.compileInline(inline);

        return template.apply(this.variables);
    }

    public List<LogEntry> logs() {
        return this.contextAppender
            .events
            .stream()
            .map(event -> LogEntry.builder()
                .level(org.slf4j.event.Level.valueOf(event.getLevel().toString()))
                .message(event.getFormattedMessage())
                .timestamp(Instant.ofEpochMilli(event.getTimeStamp()))
                .thread(event.getThreadName())
                .build()
            )
            .collect(Collectors.toList());
    }

    public List<MetricEntry> metrics() {
        return this.metrics;
    }

    public static class ContextAppender extends AppenderBase<ILoggingEvent> {
        private final List<ILoggingEvent> events = new ArrayList<>();

        @Override
        public void start() {
            super.start();
        }

        @Override
        public void stop() {
            super.stop();
            events.clear();
        }

        @Override
        protected void append(ILoggingEvent e) {
            events.add(e);
        }
    }
}