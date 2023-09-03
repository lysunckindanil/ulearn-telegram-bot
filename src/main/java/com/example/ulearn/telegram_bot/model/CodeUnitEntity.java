package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;

@Getter
@Setter
@Entity
@Table(name = "codeunits")
@NoArgsConstructor
public class CodeUnitEntity {
    public CodeUnitEntity(File original) {
        this.original = original;
    }

    public CodeUnitEntity(File original, File pattern, File destination) {
        this.original = original;
        this.pattern = pattern;
        this.destination = destination;
    }

    @Id
    @GeneratedValue
    private Long id;
    private File original;
    private File pattern;
    private File destination;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    private BlockEntity block;
}
