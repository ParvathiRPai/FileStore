package com.example.pava.filestore.rds;

import com.sun.istack.NotNull;
import lombok.Data;

import javax.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Data
public class DbUser {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Integer id;

    @NotNull
    @Column(unique = true)
    public String userName;
    @NotNull
    public String firstName;
    @NotNull
    public String lastName;
    public OffsetDateTime lastModifiedAt;
    public OffsetDateTime uploadedAt;
    @NotNull
    public String description;
    @NotNull
    public String email;
}
