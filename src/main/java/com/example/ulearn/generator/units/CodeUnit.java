package com.example.ulearn.generator.units;

import lombok.Getter;

import java.io.File;

import static com.example.ulearn.generator.engine.Generator.src;


@Getter
public class CodeUnit {
    private final String name;
    @Getter
    private final File file;

    public CodeUnit(String name) {
        this.name = name;
        this.file = new File(src + File.separator + "CodeOriginalFiles" + File.separator + name + ".txt");
    }

    @Override
    public String toString() {
        return name;
    }
}