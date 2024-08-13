package am.ik.translation.config;

import java.util.concurrent.Executor;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class AsyncConfig implements AsyncConfigurer {

	private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public AsyncConfig(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;
	}

	// https://github.com/micrometer-metrics/tracing/wiki/Spring-Cloud-Sleuth-3.1-Migration-Guide#async-instrumentation
	@Override
	public Executor getAsyncExecutor() {
		ContextSnapshotFactory contextSnapshotFactory = ContextSnapshotFactory.builder()
			.contextRegistry(ContextRegistry.getInstance())
			.build();
		return ContextExecutorService.wrap(this.threadPoolTaskExecutor.getThreadPoolExecutor(),
				contextSnapshotFactory::captureAll);
	}

}
