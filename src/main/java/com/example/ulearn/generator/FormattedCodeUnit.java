package com.example.ulearn.generator;

import lombok.Getter;

import java.io.File;

@Getter
public class FormattedCodeUnit extends CodeUnit {
    private final String block;

    public FormattedCodeUnit(String name, String block) {
        super(name);
        this.block = block;
    }

    @Override
    public File getFile() {
        return Generator.getFile(this.getName(), block);
    }
}
