/*
    Copyright 2019-2020 eMobilify GmbH

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package snc.openchargingnetwork.node.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.IntervalTask
import org.springframework.scheduling.config.ScheduledTaskRegistrar


@Configuration
class TaskConfig(private val scheduledTasks: List<IntervalTask>): SchedulingConfigurer {

    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        // override default thread pool task scheduler as follows:
        // note: not the same as the thread executor which runs async tasks (methods with with @Async annotations)

        // val threadPoolTaskScheduler = ThreadPoolTaskScheduler()
        // threadPoolTaskScheduler.poolSize = 10
        // threadPoolTaskScheduler.setThreadNamePrefix("task-pool-")
        // threadPoolTaskScheduler.initialize()
        // taskRegistrar.setTaskScheduler(threadPoolTaskScheduler)

        for (task in scheduledTasks) {
            taskRegistrar.addFixedRateTask(task)
        }
    }

}