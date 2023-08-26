package com.example.ulearn.generator.units;

import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;


@Getter
public class CodeUnit {
    @Getter
    private final File file;
    private final String name;

    public CodeUnit(File file) {
        this.file = file;
        this.name = FilenameUtils.removeExtension(file.getName());
    }

    @Override
    public String toString() {
        return name;
    }

}