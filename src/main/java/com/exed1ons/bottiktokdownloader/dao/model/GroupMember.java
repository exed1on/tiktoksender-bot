package com.exed1ons.bottiktokdownloader.dao.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Entity
@Table(name = "members")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class GroupMember {

    @Id
    @Column(name = "id")
    Long id;

    @Column(name = "userName")
    String userName;

    @Column(name = "firstName")
    String firstName;

    @ManyToMany
    @JoinTable(
            name = "member_roles",
            joinColumns = @JoinColumn(name = "member_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )

    @ToString.Exclude
    List<Role> roles;
}