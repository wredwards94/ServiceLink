package com.wesleyedwards.ServiceLink;

import com.wesleyedwards.ServiceLink.entities.*;
import com.wesleyedwards.ServiceLink.enums.Role;
import com.wesleyedwards.ServiceLink.repositories.CommentRepository;
import com.wesleyedwards.ServiceLink.repositories.TicketRepository;
import com.wesleyedwards.ServiceLink.repositories.UserRepository;
import com.wesleyedwards.ServiceLink.enums.TicketPriority;
import com.wesleyedwards.ServiceLink.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class Seeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    private static final String[] FIRST_NAMES = {
            "John", "Jane", "Michael", "Emily", "Chris",
            "Sarah", "David", "Laura", "James", "Emma"
    };

    private static final String[] LAST_NAMES = {
            "Doe", "Smith", "Johnson", "Williams", "Brown",
            "Jones", "Garcia", "Miller", "Davis", "Wilson"
    };

    private static final String[][] TICKETS = {
            {"Issue with Login", "Unable to login to the system.", "Technical"},
            {"Billing Error", "Incorrect billing amount on invoice.", "Billing"},
            {"Feature Request", "Request for a dark mode feature.", "Enhancement"},
            {"System Crash", "System crashes intermittently.", "Technical"},
            {"Password Reset", "Request for resetting account password.", "Technical"},
            {"UI Bug", "Alignment issue on the dashboard.", "Technical"},
            {"Slow Performance", "Application is running very slowly.", "Technical"},
            {"Data Export Issue", "Unable to export data to CSV.", "Technical"},
            {"Email Notifications", "Not receiving email notifications.", "Technical"},
            {"Access Denied", "Cannot access certain pages.", "Technical"},
            {"Report Generation", "Reports are generating incorrect data.", "Billing"},
            {"Mobile App Crash", "App crashes on iOS devices.", "Technical"},
            {"Search Not Working", "Search returns no results.", "Technical"},
            {"Integration Error", "Third party integration failing.", "Technical"},
            {"Account Locked", "Account has been locked out.", "Technical"},
            {"Payment Failed", "Payment processing is failing.", "Billing"},
            {"Dashboard Error", "Dashboard not loading correctly.", "Technical"},
            {"Profile Update", "Unable to update profile information.", "Technical"},
            {"API Timeout", "API calls timing out frequently.", "Technical"},
            {"Database Error", "Database connection errors occurring.", "Technical"}
    };

    private static final String[] COMMENTS = {
            "I am experiencing this issue since yesterday.",
            "Please provide more details about this issue.",
            "We are looking into this and will update you shortly.",
            "Can you provide steps to reproduce this?",
            "This has been escalated to the development team.",
            "A fix has been deployed, please verify.",
            "Issue confirmed, working on a resolution.",
            "Please clear your cache and try again.",
            "This is a known issue, fix coming in next release.",
            "Can you share any error logs?",
            "Workaround available, please check documentation.",
            "This will be resolved in the next sprint.",
            "Please try again after the maintenance window.",
            "Issue has been reproduced and assigned to dev team.",
            "Thank you for your patience while we resolve this."
    };

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            System.out.println("Database already seeded, skipping.");
            return;
        }

        // Create 10 Users
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Credentials credentials = new Credentials(
                    "user" + (i + 1),
                    "password" + (i + 1)
            );
            Profile profile = new Profile(
                    FIRST_NAMES[i],
                    LAST_NAMES[i],
                    FIRST_NAMES[i].toLowerCase() + "." + LAST_NAMES[i].toLowerCase() + "@example.com"
            );
            User user = new User();
            user.setCredentials(credentials);
            user.setProfile(profile);
            user.setDisabled(false);
            user.setRole(i == 0 ? Role.ADMIN : i == 1 ? Role.AGENT : Role.USER);
            users.add(user);
        }
        userRepository.saveAll(users);

        // Create 20 Tickets
        List<Ticket> tickets = new ArrayList<>();
        TicketStatus[] statuses = TicketStatus.values();
        TicketPriority[] priorities = TicketPriority.values();
        Random random = new Random();

        for (int i = 0; i < TICKETS.length; i++) {
            Ticket ticket = new Ticket();
            ticket.setTitle(TICKETS[i][0]);
            ticket.setDescription(TICKETS[i][1]);
            ticket.setCategory(TICKETS[i][2]);
            ticket.setStatus(statuses[random.nextInt(statuses.length)]);
            ticket.setPriority(priorities[random.nextInt(priorities.length)]);
            ticket.setRequester(users.get(random.nextInt(users.size())));
            ticket.setAssignedTo(users.get(random.nextInt(users.size())));
            tickets.add(ticket);
        }
        ticketRepository.saveAll(tickets);

        // Create 3-5 Comments per Ticket
        List<Comment> comments = new ArrayList<>();
        for (Ticket ticket : tickets) {
            int commentCount = 3 + random.nextInt(3); // 3 to 5 comments per ticket
            for (int i = 0; i < commentCount; i++) {
                Comment comment = new Comment();
                comment.setTicket(ticket);
                comment.setAuthor(users.get(random.nextInt(users.size())));
                comment.setContent(COMMENTS[random.nextInt(COMMENTS.length)]);
                comments.add(comment);
            }
        }
        commentRepository.saveAll(comments);

        System.out.println("Seeding completed:");
        System.out.println("- " + users.size() + " users created");
        System.out.println("- " + tickets.size() + " tickets created");
        System.out.println("- " + comments.size() + " comments created");
    }
}
