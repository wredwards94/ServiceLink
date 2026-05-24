package com.wesleyedwards.ServiceLink;

import com.wesleyedwards.ServiceLink.Entities.*;
import com.wesleyedwards.ServiceLink.Repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.Repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.Repositories.UserRepository;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class Seeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    @Override
    public void run(String... args) throws Exception {
        // Sample Credentials
        Credentials credentials1 = new Credentials("user1", "password1");
        Credentials credentials2 = new Credentials("user2", "password2");

        // Sample Profiles
        Profile profile1 = new Profile("John", "Doe", "john.doe@example.com");
        Profile profile2 = new Profile("Jane", "Smith", "jane.smith@example.com");

        // Create Users
        User user1 = new User();
//        user1.setUserId(UUID.randomUUID());
        user1.setCredentials(credentials1);
        user1.setProfile(profile1);
        user1.setDisabled(false);

        User user2 = new User();
//        user2.setUserId(UUID.randomUUID());
        user2.setCredentials(credentials2);
        user2.setProfile(profile2);
        user2.setDisabled(false);

        // Save Users
        userRepository.saveAll(Arrays.asList(user1, user2));

        // Create Tickets
        Ticket ticket1 = new Ticket();
        ticket1.setTitle("Issue with Login");
        ticket1.setDescription("Unable to login to the system.");
        ticket1.setStatus(TicketStatus.NEW);
        ticket1.setPriority(TicketPriority.HIGH);
        ticket1.setCategory("Technical");
        ticket1.setAssignedTo(user1);
        ticket1.setRequester(user2);
//        ticket1.setCreatedAt(Timestamp.from(Instant.now()));
        ticket1.setUpdatedAt(LocalDateTime.now());

        Ticket ticket2 = new Ticket();
        ticket2.setTitle("Billing Error");
        ticket2.setDescription("Incorrect billing amount.");
        ticket2.setStatus(TicketStatus.IN_PROGRESS);
        ticket2.setPriority(TicketPriority.MEDIUM);
        ticket2.setCategory("Billing");
        ticket2.setAssignedTo(user2);
        ticket2.setRequester(user1);
//        ticket2.setCreatedAt(Timestamp.from(Instant.now()));
        ticket2.setUpdatedAt(LocalDateTime.now());

        Ticket ticket3 = new Ticket();
        ticket3.setTitle("Feature Request");
        ticket3.setDescription("Request for a dark mode feature.");
        ticket3.setStatus(TicketStatus.NEW);
        ticket3.setPriority(TicketPriority.LOW);
        ticket3.setCategory("Enhancement");
        ticket3.setAssignedTo(user1);
        ticket3.setRequester(user2);
//        ticket3.setCreatedAt(Timestamp.from(Instant.now()));
        ticket3.setUpdatedAt(LocalDateTime.now());

        Ticket ticket4 = new Ticket();
        ticket4.setTitle("System Crash");
        ticket4.setDescription("System crashes intermittently.");
        ticket4.setStatus(TicketStatus.IN_PROGRESS);
        ticket4.setPriority(TicketPriority.HIGH);
        ticket4.setCategory("Technical");
        ticket4.setAssignedTo(user2);
        ticket4.setRequester(user1);
//        ticket4.setCreatedAt(Timestamp.from(Instant.now()));
        ticket4.setUpdatedAt(LocalDateTime.now());

        Ticket ticket5 = new Ticket();
        ticket5.setTitle("Password Reset");
        ticket5.setDescription("Request for resetting the account password.");
        ticket5.setStatus(TicketStatus.NEW);
        ticket5.setPriority(TicketPriority.HIGH);
        ticket5.setCategory("Technical");
        ticket5.setAssignedTo(user1);
        ticket5.setRequester(user2);
//        ticket5.setCreatedAt(Timestamp.from(Instant.now()));
        ticket5.setUpdatedAt(LocalDateTime.now());

        Ticket ticket6 = new Ticket();
        ticket6.setTitle("UI Bug");
        ticket6.setDescription("Alignment issue on the dashboard.");
        ticket6.setStatus(TicketStatus.NEW);
        ticket6.setPriority(TicketPriority.LOW);
        ticket6.setCategory("Technical");
        ticket6.setAssignedTo(user2);
        ticket6.setRequester(user1);
//        ticket6.setCreatedAt(Timestamp.from(Instant.now()));
        ticket6.setUpdatedAt(LocalDateTime.now());

        // Save Tickets
        ticketRepository.saveAll(Arrays.asList(ticket1, ticket2, ticket3, ticket4, ticket5, ticket6));

        // Create Comments
        Comment comment1 = new Comment();
        comment1.setTicket(ticket1);
        comment1.setAuthor(user2);
        comment1.setContent("I am experiencing this issue since yesterday.");
//        comment1.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment2 = new Comment();
        comment2.setTicket(ticket2);
        comment2.setAuthor(user1);
        comment2.setContent("Please provide more details about the billing error.");
//        comment2.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment3 = new Comment();
        comment3.setTicket(ticket3);
        comment3.setAuthor(user2);
        comment3.setContent("Dark mode would be a great addition.");
//        comment3.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment4 = new Comment();
        comment4.setTicket(ticket4);
        comment4.setAuthor(user1);
        comment4.setContent("The system crash happens when performing a specific action.");
//        comment4.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment5 = new Comment();
        comment5.setTicket(ticket4);
        comment5.setAuthor(user2);
        comment5.setContent("Can you provide steps to reproduce the crash?");
//        comment5.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment6 = new Comment();
        comment6.setTicket(ticket1);
        comment6.setAuthor(user1);
        comment6.setContent("We are looking into this issue and will update you shortly.");
//        comment6.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment7 = new Comment();
        comment7.setTicket(ticket2);
        comment7.setAuthor(user2);
        comment7.setContent("The billing amount does not match the services provided.");
//        comment7.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment8 = new Comment();
        comment8.setTicket(ticket3);
        comment8.setAuthor(user1);
        comment8.setContent("Dark mode is being evaluated by our development team.");
//        comment8.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment9 = new Comment();
        comment9.setTicket(ticket4);
        comment9.setAuthor(user1);
        comment9.setContent("The crash seems related to memory usage spikes.");
//        comment9.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment10 = new Comment();
        comment10.setTicket(ticket4);
        comment10.setAuthor(user2);
        comment10.setContent("I will try testing it with a different configuration.");
//        comment10.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment11 = new Comment();
        comment11.setTicket(ticket5);
        comment11.setAuthor(user2);
        comment11.setContent("I forgot my password and need urgent access.");
//        comment11.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment12 = new Comment();
        comment12.setTicket(ticket5);
        comment12.setAuthor(user1);
        comment12.setContent("Password reset instructions have been sent to your email.");
//        comment12.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment13 = new Comment();
        comment13.setTicket(ticket6);
        comment13.setAuthor(user1);
        comment13.setContent("The UI bug has been noted. We will fix it in the next release.");
//        comment13.setCreatedAt(Timestamp.from(Instant.now()));

        Comment comment14 = new Comment();
        comment14.setTicket(ticket6);
        comment14.setAuthor(user2);
        comment14.setContent("Thanks for the update. Looking forward to the fix.");
//        comment14.setCreatedAt(Timestamp.from(Instant.now()));

        // Save Comments
        commentRepository.saveAll(Arrays.asList(comment1, comment2, comment3, comment4, comment5, comment6, comment7, comment8, comment9, comment10, comment11, comment12, comment13, comment14));

        System.out.println("User, Ticket, and Comment seeding completed.");
    }
}
