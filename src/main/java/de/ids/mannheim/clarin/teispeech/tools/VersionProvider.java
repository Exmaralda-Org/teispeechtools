package de.ids.mannheim.clarin.teispeech.tools;

import java.io.IOException;
import java.util.Properties;

import picocli.CommandLine.IVersionProvider;

class VersionProvider implements IVersionProvider {

    /**
     *
     * @return version number defined in
     * {@code src/main/resources/properties/project.properties}
     * @throws IOException when file broken/unavailable
     */
    @Override
    public String[] getVersion() throws IOException {
        final Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader()
                .getResourceAsStream("project.properties"));
        String version = properties.getProperty("version");
        return new String[] { version };
    }

}
