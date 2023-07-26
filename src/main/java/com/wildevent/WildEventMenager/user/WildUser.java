package com.wildevent.WildEventMenager.user;

import com.wildevent.WildEventMenager.event.model.Event;
import com.wildevent.WildEventMenager.location.Location;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wild_user")
public class WildUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private boolean active;

    @ManyToMany
    private List<Location> location;

    @ManyToMany(mappedBy = "organizer")
    private List<Event> eventOrganized;

    @OneToMany(mappedBy = "userProposing")
    private List<Event> eventsProposed;
}
