package com.portfolio.notificationapi;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.portfolio.notificationapi.delivery.*;
import com.portfolio.notificationapi.entity.*;
import com.portfolio.notificationapi.repository.*;
import com.portfolio.notificationapi.service.NotificationService;
import com.portfolio.notificationapi.dto.NotificationRequest;
import com.portfolio.notificationapi.exception.BusinessException;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.concurrent.*;
import java.io.IOException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"notification.delivery.enabled=false","spring.rabbitmq.listener.simple.auto-startup=false"})
@AutoConfigureMockMvc @Import(ReliableDeliveryIntegrationTest.TestConfig.class)
@org.springframework.security.test.context.support.WithMockUser(roles="ADMIN")
class ReliableDeliveryIntegrationTest {
    static final EmbeddedPostgres PG=startDatabase();
    static final GreenMail MAIL=new GreenMail(new ServerSetup(0,"127.0.0.1",ServerSetup.PROTOCOL_SMTP));
    static EmbeddedPostgres startDatabase(){try{return EmbeddedPostgres.builder().setPort(0).start();}catch(IOException ex){throw new ExceptionInInitializerError(ex);}}
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->PG.getJdbcUrl("postgres","postgres"));
        r.add("spring.datasource.username",()->"postgres"); r.add("spring.datasource.password",()->"postgres");
    }
    @BeforeAll static void smtpStart(){MAIL.start();}
    @AfterAll static void close() throws IOException {MAIL.stop();PG.close();}
    static class MutableClock extends Clock {
        Instant now=Instant.parse("2030-01-01T12:00:00Z");
        public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId zone){return this;}
        public Instant instant(){return now;} void advance(long seconds){now=now.plusSeconds(seconds);}
    }
    @TestConfiguration static class TestConfig {
        @Bean @Primary MutableClock testClock(){return new MutableClock();}
        @Bean @Primary JavaMailSenderImpl testMailSender(){
            var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");
            sender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout","1000");
            sender.getJavaMailProperties().setProperty("mail.smtp.timeout","1000");return sender;
        }
    }
    @Autowired NotificationService service;
    @Autowired DeliveryWorker worker;
    @Autowired NotificationRepository notifications;
    @Autowired DeliveryAttemptRepository attempts;
    @Autowired ChannelRepository channels;
    @Autowired MutableClock clock;
    @Autowired JavaMailSenderImpl sender;
    @Autowired MockMvc mvc;
    Long emailId;
    @BeforeEach void reset() throws Exception {
        attempts.deleteAll();notifications.deleteAll();
        if(MAIL.getSmtp()==null || !MAIL.getSmtp().isRunning()) MAIL.start();
        MAIL.purgeEmailFromAllMailboxes();sender.setPort(MAIL.getSmtp().getPort());
        clock.now=Instant.parse("2030-01-01T12:00:00Z");
        var email=channels.findByNameIgnoreCase("EMAIL").orElseThrow();email.setActive(true);channels.save(email);emailId=email.getId();
    }
    NotificationRequest request(LocalDateTime schedule){return new NotificationRequest("Order confirmed","Sandbox test message","customer@example.invalid",emailId,schedule);}
    long create(){return service.create(request(null)).id();}
    @Test void sendsImmediateEmailAndPersistsAttempt() throws Exception {
        long id=create(); worker.process(id);
        assertThat(MAIL.getReceivedMessages()).hasSize(1);
        assertThat(MAIL.getReceivedMessages()[0].getSubject()).isEqualTo("Order confirmed");
        assertThat(MAIL.getReceivedMessages()[0].getContent().toString()).contains("Sandbox test message");
        var n=notifications.findById(id).orElseThrow();assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isEqualTo(LocalDateTime.now(clock));assertThat(n.getAttemptCount()).isEqualTo(1);
        assertThat(service.history(id)).singleElement().extracting(a->a.outcome()).isEqualTo("SMTP_ACCEPTED");
        mvc.perform(get("/api/v1/notifications/{id}/attempts",id)).andExpect(status().isOk()).andExpect(jsonPath("$[0].attemptNumber").value(1));
    }
    @Test void scheduledMessageWaitsUntilDue(){
        long id=service.create(request(LocalDateTime.now(clock).plusSeconds(60))).id();
        worker.process(id);assertThat(MAIL.getReceivedMessages()).isEmpty();assertThat(service.history(id)).isEmpty();
        clock.advance(60);worker.process(id);assertThat(MAIL.getReceivedMessages()).hasSize(1);
    }
    @Test void duplicateMessageAfterSuccessDoesNotSendAgain(){
        long id=create();worker.process(id);worker.process(id);
        assertThat(MAIL.getReceivedMessages()).hasSize(1);assertThat(service.history(id)).hasSize(1);
    }
    @Test void concurrentConsumersSendOnce() throws Exception {
        long id=create();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try {
            var first=pool.submit(()->{start.await();worker.process(id);return true;});
            var second=pool.submit(()->{start.await();worker.process(id);return true;});
            start.countDown();first.get(15,TimeUnit.SECONDS);second.get(15,TimeUnit.SECONDS);
        }finally{pool.shutdownNow();}
        assertThat(MAIL.getReceivedMessages()).hasSize(1);assertThat(service.history(id)).hasSize(1);
    }
    @Test void smtpOutageSchedulesBackoffAndRecovers(){
        long id=create();MAIL.stop();worker.process(id);
        var n=notifications.findById(id).orElseThrow();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(n.getNextAttemptAt()).isEqualTo(LocalDateTime.now(clock).plusSeconds(5));
        worker.process(id);assertThat(service.history(id)).hasSize(1);
        MAIL.start();sender.setPort(MAIL.getSmtp().getPort());clock.advance(5);worker.process(id);
        assertThat(notifications.findById(id).orElseThrow().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(MAIL.getReceivedMessages()).hasSize(1);assertThat(service.history(id)).hasSize(2);
    }
    @Test void exhaustedRetriesLeaveDurableDeadLetterWork(){
        long id=create();MAIL.stop();
        worker.process(id);clock.advance(5);worker.process(id);
        var n=notifications.findById(id).orElseThrow();assertThat(n.getNextAttemptAt()).isEqualTo(LocalDateTime.now(clock).plusSeconds(10));
        clock.advance(10);worker.process(id);worker.process(id);
        n=notifications.findById(id).orElseThrow();assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.isDeadLetterPending()).isTrue();assertThat(n.getAttemptCount()).isEqualTo(3);
        assertThat(service.history(id)).hasSize(3);assertThat(n.getNextAttemptAt()).isNull();
        worker.acknowledgeDeadLetter(id);assertThat(notifications.findById(id).orElseThrow().isDeadLetterPending()).isFalse();
    }
    @Test void cancellationSurvivesQueuedDuplicate(){long id=create();service.cancel(id);worker.process(id);assertThat(MAIL.getReceivedMessages()).isEmpty();assertThat(service.history(id)).isEmpty();}
    @Test void cancellationAfterSendIsRejected(){long id=create();worker.process(id);assertThatThrownBy(()->service.cancel(id)).isInstanceOf(BusinessException.class);}
    @Test void unsupportedChannelsAreRejected(){
        var sms=channels.findByNameIgnoreCase("SMS").orElseThrow();sms.setActive(true);channels.save(sms);
        assertThatThrownBy(()->service.create(new NotificationRequest("SMS","Unsupported","recipient@example.invalid",sms.getId(),null))).isInstanceOf(BusinessException.class);
    }
    @Test void disabledChannelAfterCreationDoesNotPretendSuccess(){
        long id=create();var c=channels.findById(emailId).orElseThrow();c.setActive(false);channels.save(c);worker.process(id);
        assertThat(MAIL.getReceivedMessages()).isEmpty();assertThat(notifications.findById(id).orElseThrow().getLastError()).isEqualTo("CHANNEL_INACTIVE");
    }
    @Test void idempotencyKeyReturnsSameNotification(){
        var a=service.create(request(null),"test-order-1");var b=service.create(request(null),"test-order-1");
        assertThat(a.id()).isEqualTo(b.id());assertThat(notifications.count()).isEqualTo(1);
    }
    @Test void scheduledIdempotencyMatchesPostgresTimestampPrecision(){
        var request = request(LocalDateTime.now(clock).plusHours(1).withNano(123456789));
        var first = service.create(request,"scheduled-precision");
        assertThat(service.create(request,"scheduled-precision").id()).isEqualTo(first.id());
    }
    @Test void idempotencyKeyRejectsDifferentPayload(){
        service.create(request(null),"test-order-2");
        assertThatThrownBy(()->service.create(new NotificationRequest("Different","Text","customer@example.invalid",emailId,null),"test-order-2")).isInstanceOf(BusinessException.class);
    }
    @Test void deletingNotificationAlsoDeletesHistory(){long id=create();worker.process(id);service.delete(id);worker.process(id);assertThat(attempts.count()).isZero();assertThat(notifications.existsById(id)).isFalse();}
    @Test void missingNotificationMessageIsIgnored(){worker.process(987654L);assertThat(attempts.count()).isZero();}
    @Test void pendingAndRetryingAreSelectable(){
        long id=create();assertThat(notifications.findReadyIds(DeliveryWorker.READY,LocalDateTime.now(clock),org.springframework.data.domain.PageRequest.of(0,100))).contains(id);
    }
    @Test void invalidRecipientGets400() throws Exception {
        mvc.perform(post("/api/v1/notifications").contentType("application/json").content("{\"title\":\"Hi\",\"message\":\"Hello\",\"recipient\":\"invalid-address\",\"channelId\":"+emailId+"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void upgradesExistingDatabaseWithoutDeletingNotifications() throws Exception {
        try(var connection=PG.getPostgresDatabase().getConnection();var sql=connection.createStatement()){
            sql.execute("create schema legacy_upgrade");sql.execute("set search_path to legacy_upgrade");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.ClassPathResource("db/migration/V1__initial_schema.sql"));
            sql.execute("insert into channels(name,active) values ('EMAIL',true)");
            sql.execute("insert into notifications(title,message,recipient,channel_id,status,created_at) values ('Keep me','Old message','legacy@example.invalid',1,'PENDING',current_timestamp)");
        }
        org.flywaydb.core.Flyway.configure().dataSource(PG.getJdbcUrl("postgres","postgres"),"postgres","postgres")
            .schemas("legacy_upgrade").defaultSchema("legacy_upgrade").baselineOnMigrate(true).baselineVersion("1").load().migrate();
        try(var connection=PG.getPostgresDatabase().getConnection();var sql=connection.createStatement();var rows=sql.executeQuery("select title,attempt_count from legacy_upgrade.notifications")){
            assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("Keep me");assertThat(rows.getInt(2)).isZero();
        }
    }
}
