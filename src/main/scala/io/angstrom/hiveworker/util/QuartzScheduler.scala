package io.angstrom.hiveworker.util

import com.twitter.logging.Logger
import org.quartz.CronExpression
import org.quartz.CronScheduleBuilder
import org.quartz.Job
import org.quartz.JobBuilder._
import org.quartz.JobExecutionContext
import org.quartz.Scheduler
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder._
import org.quartz.impl.StdSchedulerFactory
import scala.concurrent.duration.Duration

/** Based on https://github.com/mchv/play2-quartz */
object QuartzScheduler {
  private val log = Logger(getClass.getSimpleName)
  private val scheduler = StdSchedulerFactory.getDefaultScheduler

  def start() {
    scheduler.start()
  }

  def stop() {
    scheduler.shutdown()
  }

  def schedule(name: String, g: => Unit): ScheduleHolder = {
    log.info("Scheduling function with name: %s".format(name))
    val wrap: JobExecutionContext => Unit = x => g
    scheduleWithContext(name, wrap)
  }

  private[this] def scheduleWithContext(name: String, f: JobExecutionContext => Unit): ScheduleHolder = {
    new ScheduleHolder(name, f, scheduler)
  }
}

class ScheduleHolder(name: String, f: JobExecutionContext => Unit, scheduler: Scheduler) {
  def at(cronPattern: String) {
    ScheduleHolder.add(name, f)
    val job = newJob(classOf[GenJob]).withIdentity(name).build()
    val trigger = newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(new CronExpression(cronPattern))).build()
    scheduler.scheduleJob(job, trigger)
  }

  def every(interval: Duration) {
    ScheduleHolder.add(name, f)
    val job = newJob(classOf[GenJob]).withIdentity(name).build()
    val trigger = newTrigger().withSchedule(simpleSchedule().withIntervalInMilliseconds(interval.toMillis).repeatForever()).build()
    scheduler.scheduleJob(job, trigger)
  }
}

object ScheduleHolder {
  type JobFunc = JobExecutionContext => Unit

  private val jobs = new collection.mutable.HashMap[String, JobFunc] with collection.mutable.SynchronizedMap[String, JobFunc]

  def add(name: String, job: JobFunc) {
    jobs.put(name, job)
  }

  def get(name: String): Option[JobFunc] = jobs.get(name)
}

class GenJob extends Job {
  def execute(ctx: JobExecutionContext) {
    val name = ctx.getJobDetail.getKey.getName
    ScheduleHolder.get(name).map(f => f(ctx))
  }
}
