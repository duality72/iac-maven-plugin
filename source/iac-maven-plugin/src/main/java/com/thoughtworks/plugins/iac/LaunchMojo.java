package com.thoughtworks.plugins.iac;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;

/**
 * Goal which launches a platform.
 * 
 * @goal launch
 * 
 * @phase pre-integration-test
 */
public class LaunchMojo extends AbstractMojo {
	/**
	 * The Maven Project Object
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * The Maven Session Object
	 * 
	 * @parameter expression="${session}"
	 * @required
	 * @readonly
	 */
	protected MavenSession session;

	/**
	 * The Maven PluginManager Object
	 * 
	 * @component
	 * @required
	 */
	protected PluginManager pluginManager;


	public void execute() throws MojoExecutionException {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(
					"src/main/resources/platform.properties"));
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Could not read platform.properties", e);
		}
		String amiID = props.getProperty("ami");
		getLog().info("Launching instance with ami id: " + amiID );
		executeMojo(plugin(groupId("org.codehaus.mojo"),
				artifactId("ec2-maven-plugin"), version("1.0-SNAPSHOT")),
				goal("start"), configuration(element(
						name("launchConfigurations"), element(
								name("launchConfiguration"), element(
										name("imageId"), amiID),
								element(name("minCount"), "1"), element(
										name("maxCount"), "1"), element(
										name("keyName"), "thoughtworks"),
								element(name("wait"), "true"), element(
										name("terminate"), "true")))

				), executionEnvironment(project, session, pluginManager));
	}
}
