// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.example;

import com.yahoo.application.Application;
import com.yahoo.application.Networking;
import org.junit.Test;

import java.nio.file.FileSystems;

import static com.yahoo.application.Application.fromApplicationPackage;
import static org.junit.Assume.assumeTrue;

public class ApplicationMain {

    @Test
    public void runFromMaven() throws Exception {
        assumeTrue(Boolean.valueOf(System.getProperty("isMavenSurefirePlugin")));
        main(null);
    }

    public static void main(String[] args) throws Exception {
        try (Application app = fromApplicationPackage(
                FileSystems.getDefault().getPath("src/main/application"),
                Networking.enable)) {
                   app.getClass(); // throws NullPointerException
                   Thread.sleep(Long.MAX_VALUE);
        }
    }
}
