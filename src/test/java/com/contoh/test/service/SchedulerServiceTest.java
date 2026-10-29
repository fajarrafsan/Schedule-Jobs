package com.contoh.test.service;

import com.contoh.test.dto.SchedulerStatusResponse;
import com.contoh.test.scheduler.BatchProcessingJob;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class SchedulerServiceTest {

    private static final JobKey JOB_KEY = JobKey.jobKey("batchProcessingJob", "batch");
    private static final TriggerKey TRIGGER_KEY = TriggerKey.triggerKey("batchProcessingTrigger", "batch");

    @Mock
    private Scheduler scheduler;

    private SchedulerService service;

    @BeforeEach
    public void setUp() {
        service = new SchedulerService(scheduler, 60);
    }

    private Date toDate(LocalDateTime waktu) {
        return Date.from(waktu.atZone(ZoneId.systemDefault()).toInstant());
    }

    private Trigger triggerTiruan(LocalDateTime lastRun, LocalDateTime nextRun) {
        Trigger trigger = Mockito.mock(Trigger.class);
        Mockito.when(trigger.getPreviousFireTime()).thenReturn(lastRun == null ? null : toDate(lastRun));
        Mockito.when(trigger.getNextFireTime()).thenReturn(toDate(nextRun));
        return trigger;
    }

    @Test
    @DisplayName("start: job belum ada -> JobDetail & Trigger didaftarkan, tiap 60 detik, ulang terus")
    void start_belumAda() throws Exception {
        // Arrange
        Mockito.when(scheduler.checkExists(JOB_KEY)).thenReturn(false);
        Trigger trigger = triggerTiruan(null, LocalDateTime.of(2026, 10, 29, 10, 0, 0));
        Mockito.when(scheduler.getTrigger(TRIGGER_KEY)).thenReturn(trigger);

        // Act
        SchedulerStatusResponse result = service.start();

        // Assert: tangkap apa yang dikirim ke scheduleJob(), lalu periksa isinya
        ArgumentCaptor<JobDetail> jobCaptor = ArgumentCaptor.forClass(JobDetail.class);
        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        Mockito.verify(scheduler).scheduleJob(jobCaptor.capture(), triggerCaptor.capture());

        JobDetail jobDetail = jobCaptor.getValue();
        Assertions.assertThat(jobDetail.getKey()).isEqualTo(JOB_KEY);
        Assertions.assertThat(jobDetail.getJobClass()).isEqualTo(BatchProcessingJob.class);

        SimpleTrigger simpleTrigger = (SimpleTrigger) triggerCaptor.getValue();
        Assertions.assertThat(simpleTrigger.getKey()).isEqualTo(TRIGGER_KEY);
        Assertions.assertThat(simpleTrigger.getRepeatInterval()).isEqualTo(60_000L); // Quartz menyimpan dalam milidetik
        Assertions.assertThat(simpleTrigger.getRepeatCount()).isEqualTo(SimpleTrigger.REPEAT_INDEFINITELY);

        Assertions.assertThat(result.status()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("start: interval mengikuti nilai dari konfigurasi")
    void start_intervalDariKonfigurasi() throws Exception {
        SchedulerService service30Detik = new SchedulerService(scheduler, 30);
        Mockito.when(scheduler.checkExists(JOB_KEY)).thenReturn(false);

        service30Detik.start();

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        Mockito.verify(scheduler).scheduleJob(Mockito.any(JobDetail.class), triggerCaptor.capture());
        Assertions.assertThat(((SimpleTrigger) triggerCaptor.getValue()).getRepeatInterval()).isEqualTo(30_000L);
    }

    @Test
    @DisplayName("start: job sudah ada -> tidak didaftarkan lagi (tidak dobel)")
    void start_sudahAda() throws Exception {
        Mockito.when(scheduler.checkExists(JOB_KEY)).thenReturn(true);
        Trigger trigger = triggerTiruan(
                LocalDateTime.of(2026, 10, 29, 10, 0, 0), LocalDateTime.of(2026, 10, 29, 10, 1, 0));
        Mockito.when(scheduler.getTrigger(TRIGGER_KEY)).thenReturn(trigger);

        SchedulerStatusResponse result = service.start();

        Mockito.verify(scheduler, Mockito.never()).scheduleJob(Mockito.any(JobDetail.class), Mockito.any(Trigger.class));
        Assertions.assertThat(result.status()).isEqualTo("RUNNING");
    }

    @Test
    @DisplayName("start: Quartz error -> SchedulerException diteruskan ke atas")
    void start_errorQuartz() throws Exception {
        Mockito.when(scheduler.checkExists(JOB_KEY)).thenThrow(new SchedulerException("Quartz error"));

        Assertions.assertThatThrownBy(() -> service.start())
                .isInstanceOf(SchedulerException.class);
    }

    @Test
    @DisplayName("stop: job dihapus dari scheduler -> status STOPPED")
    void stop() throws Exception {
        // getTrigger tidak di-stub, jadi mengembalikan null, seperti setelah job dihapus
        SchedulerStatusResponse result = service.stop();

        Mockito.verify(scheduler).deleteJob(JOB_KEY);
        Assertions.assertThat(result.status()).isEqualTo("STOPPED");
    }

    @Test
    @DisplayName("status: tidak ada trigger -> STOPPED, lastRun & nextRun null")
    void status_stopped() throws Exception {
        Mockito.when(scheduler.getTrigger(TRIGGER_KEY)).thenReturn(null);

        SchedulerStatusResponse result = service.status();

        Assertions.assertThat(result.status()).isEqualTo("STOPPED");
        Assertions.assertThat(result.intervalSeconds()).isEqualTo(60);
        Assertions.assertThat(result.lastRun()).isNull();
        Assertions.assertThat(result.nextRun()).isNull();
    }

    @Test
    @DisplayName("status: baru di-start, belum pernah jalan -> lastRun null")
    void status_belumPernahJalan() throws Exception {
        LocalDateTime nextRun = LocalDateTime.of(2026, 10, 29, 10, 0, 0);
        Trigger trigger = triggerTiruan(null, nextRun);
        Mockito.when(scheduler.getTrigger(TRIGGER_KEY)).thenReturn(trigger);

        SchedulerStatusResponse result = service.status();

        Assertions.assertThat(result.status()).isEqualTo("RUNNING");
        Assertions.assertThat(result.lastRun()).isNull();
        Assertions.assertThat(result.nextRun()).isEqualTo(nextRun);
    }


}
