package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;


@SuppressWarnings("unused")
@Getter
@Setter
@Entity
@Table(name = "codeunits")
@NoArgsConstructor
public class CodeUnit {
    @Id
    private String name;
    private File original;
    private boolean isFabricate;
    @ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.EAGER)
    private Block block;

    public CodeUnit(File original, boolean isFabricate) {
        this.name = FilenameUtils.removeExtension(original.getName());
        this.original = original;
        this.isFabricate = isFabricate;
    }

    public CodeUnit(File original) {
        this.name = FilenameUtils.removeExtension(original.getName());
        this.original = original;
        this.isFabricate = false;
    }
}
