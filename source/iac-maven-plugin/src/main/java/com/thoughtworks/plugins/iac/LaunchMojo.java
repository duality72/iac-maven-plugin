package com.thoughtworks.plugins.iac;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.mojo.ec2.AbstractEc2Mojo;
import org.codehaus.mojo.ec2.TerminateInstancesThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.LaunchConfiguration;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;

/**
 * Goal which launches a platform.
 * 
 * @goal launch
 * 
 * @phase pre-integration-test
 */
public class LaunchMojo extends AbstractEc2Mojo {

	final Logger logger = LoggerFactory.getLogger(LaunchMojo.class);

	public void doExecute(Jec2 ec2) throws MojoExecutionException {
		Properties props = new Properties();
		try {
			props.load(new FileInputStream(
					"src/main/resources/platform.properties"));
		} catch (IOException e) {
			throw new MojoExecutionException(
					"Could not read platform.properties", e);
		}
		String amiId = props.getProperty("ami");
		getLog().info("Launching instance with ami id: " + amiId);

		// Launch a new instance
		LaunchConfiguration lc = new LaunchConfiguration(amiId);
		try {
			List<Instance> instances = ec2.runInstances(lc).getInstances();
			waitForInstancesToStart(ec2, instances);
			// Add a shutdown hook to terminate the instance
			List<Instance> instancesToTerminate = new ArrayList<Instance>(1);
			instancesToTerminate.addAll(instances);
			TerminateInstancesThread.addShutdownHook(ec2, instancesToTerminate);
		} catch (EC2Exception e) {
			throw new MojoExecutionException("Exception in EC2: "
					+ e.getMessage(), e);
		}
	}

	private void waitForInstancesToStart(Jec2 ec2, List<Instance> instances)
			throws MojoExecutionException {
		List<String> instanceIds = new ArrayList<String>(instances.size());
		for (Instance instance : instances) {
			instanceIds.add(instance.getInstanceId());
		}

		while (!instanceIds.isEmpty()) {
			try {
				for (ReservationDescription description : ec2.describeInstances(instanceIds)) {
					for (Instance currentInstance : description.getInstances()) {
						if (currentInstance.isRunning()) {
							instanceIds.remove(currentInstance.getInstanceId());
						}
					}
				}
			} catch (EC2Exception e) {
				throw new MojoExecutionException("Error describing instances "
						+ instanceIds, e);
			}

			if (instanceIds.isEmpty()) {
				return;
			}

			try {
				logger.info("Waiting for instances to start: {}", instanceIds);
				Thread.sleep(10 * 1000); // 10 sec
			} catch (InterruptedException e) {
				throw new MojoExecutionException(
						"Poll for available interrupted : " + e.getMessage(), e);
			}
		}
	}
}
