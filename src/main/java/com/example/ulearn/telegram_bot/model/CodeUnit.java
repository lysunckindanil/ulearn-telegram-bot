package com.example.ulearn.telegram_bot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;

import static com.example.ulearn.telegram_bot.service.FilesService.SOURCE;


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

    public CodeUnit(String name, boolean isFabricate) {
        this.name = name;
        this.original = new File(SOURCE + File.separator + "CodeOriginalFiles" + File.separator + name + ".txt");
        this.isFabricate = isFabricate;
    }

    public CodeUnit(String name) {
        this.name = name;
        this.original = new File("src" + File.separator + "main" + File.separator + "resources" + File.separator + "CodeData" + File.separator + "CodeOriginalFiles" + File.separator + name + ".txt");
        this.isFabricate = false;
    }
}
